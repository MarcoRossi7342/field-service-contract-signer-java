package cc.infrai.examples.fieldservice.service;

import cc.infrai.examples.fieldservice.domain.WorkOrder;
import cc.infrai.examples.fieldservice.infrai.InfraiPdfClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

public final class ContractSigningService {
    private final SigningDecision decision;
    private final InfraiPdfClient pdfClient;
    private final ObjectMapper json;
    private final PrivateKey signingKey;
    private final Path outputDirectory;

    public ContractSigningService(SigningDecision decision, InfraiPdfClient pdfClient,
                                  ObjectMapper json, PrivateKey signingKey, Path outputDirectory) {
        this.decision = decision;
        this.pdfClient = pdfClient;
        this.json = json;
        this.signingKey = signingKey;
        this.outputDirectory = outputDirectory;
    }

    public SignedContract sign(WorkOrder order) {
        SigningDecision.Decision result = decision.evaluate(order);
        if (!result.approved()) {
            throw new ContractNotReadyException(result.reason());
        }

        try {
            byte[] pdf = pdfClient.generateContract(renderContract(order), "contract-" + order.id());
            byte[] signature = signBytes(pdf);
            String digest = hex(MessageDigest.getInstance("SHA-256").digest(pdf));
            Files.createDirectories(outputDirectory);
            Path pdfPath = outputDirectory.resolve(order.id() + ".pdf");
            Path signaturePath = outputDirectory.resolve(order.id() + ".sig");
            Path manifestPath = outputDirectory.resolve(order.id() + ".signature.json");
            Files.write(pdfPath, pdf);
            Files.write(signaturePath, signature);

            ObjectNode manifest = json.createObjectNode();
            manifest.put("workOrderId", order.id());
            manifest.put("algorithm", "SHA256withRSA");
            manifest.put("documentSha256", digest);
            manifest.put("signedAt", Instant.now().toString());
            manifest.put("signature", Base64.getEncoder().encodeToString(signature));
            Files.writeString(manifestPath, json.writerWithDefaultPrettyPrinter().writeValueAsString(manifest));
            return new SignedContract(pdfPath, signaturePath, manifestPath, digest);
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Contract signing failed", exception);
        }
    }

    private byte[] signBytes(byte[] pdf) throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(signingKey);
        signer.update(pdf);
        return signer.sign();
    }

    private String renderContract(WorkOrder order) {
        StringBuilder markdown = new StringBuilder()
                .append("# Field-service completion contract\n\n")
                .append("Work order: ").append(order.id()).append("\n\n")
                .append("Customer: ").append(order.customerName()).append("\n\n")
                .append("Service address: ").append(order.serviceAddress()).append("\n\n")
                .append("Dispatch status: ").append(order.dispatchStatus()).append("\n\n")
                .append("## Work evidence\n\n");
        order.photos().forEach(photo -> markdown.append("- ").append(photo.caption())
                .append(" (").append(photo.objectReference()).append(")\n"));
        return markdown.append("\n## Technician follow-up\n\n")
                .append(order.followUp().technicianName()).append(": ")
                .append(order.followUp().summary()).append("\n")
                .toString();
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) value.append(String.format("%02x", item));
        return value.toString();
    }

    public static PrivateKey readPrivateKey(Path path) {
        try {
            String pem = Files.readString(path, StandardCharsets.US_ASCII)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalArgumentException("Could not read PKCS#8 RSA signing key", exception);
        }
    }

    public record SignedContract(Path pdf, Path signature, Path manifest, String documentSha256) {}

    public static final class ContractNotReadyException extends IllegalStateException {
        public ContractNotReadyException(String reason) { super(reason); }
    }
}
