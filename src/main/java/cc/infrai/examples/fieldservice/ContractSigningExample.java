package cc.infrai.examples.fieldservice;

import cc.infrai.examples.fieldservice.config.ServiceConfig;
import cc.infrai.examples.fieldservice.domain.WorkOrder;
import cc.infrai.examples.fieldservice.infrai.InfraiPdfClient;
import cc.infrai.examples.fieldservice.service.ContractSigningService;
import cc.infrai.examples.fieldservice.service.SigningDecision;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;

public final class ContractSigningExample {
    private ContractSigningExample() {}

    public static void main(String[] args) {
        ServiceConfig config = ServiceConfig.fromEnvironment();
        ObjectMapper json = new ObjectMapper();
        InfraiPdfClient pdfClient = new InfraiPdfClient(
                HttpClient.newHttpClient(), json, config.apiBase(), config.apiKey());
        ContractSigningService service = new ContractSigningService(
                new SigningDecision(), pdfClient, json,
                ContractSigningService.readPrivateKey(config.privateKey()), config.outputDirectory());

        WorkOrder order = new WorkOrder(
                "WO-2048",
                "North Ridge Learning Center",
                "18 Scholar Lane",
                WorkOrder.DispatchStatus.COMPLETED,
                List.of(
                        new WorkOrder.WorkPhoto("Panel before repair", "photos/WO-2048-before.jpg", Instant.parse("2026-08-25T02:10:00Z")),
                        new WorkOrder.WorkPhoto("Panel after repair", "photos/WO-2048-after.jpg", Instant.parse("2026-08-25T03:15:00Z"))),
                new WorkOrder.TechnicianFollowUp(
                        "Mina Chen", "Replaced the relay and verified the classroom controls.",
                        Instant.parse("2026-08-25T03:20:00Z")));

        ContractSigningService.SignedContract signed = service.sign(order);
        System.out.println("Signed contract: " + signed.pdf());
        System.out.println("Detached signature: " + signed.signature());
        System.out.println("Signature manifest: " + signed.manifest());
        System.out.println("Document SHA-256: " + signed.documentSha256());
    }
}
