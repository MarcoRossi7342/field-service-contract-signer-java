package cc.infrai.examples.fieldservice.infrai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public final class InfraiPdfClient {
    private static final int MAX_ATTEMPTS = 4;
    private final HttpClient http;
    private final ObjectMapper json;
    private final URI apiBase;
    private final String apiKey;

    public InfraiPdfClient(HttpClient http, ObjectMapper json, URI apiBase, String apiKey) {
        this.http = http;
        this.json = json;
        this.apiBase = apiBase;
        this.apiKey = apiKey;
    }

    public byte[] generateContract(String markdown, String idempotencyKey) {
        ObjectNode body = json.createObjectNode();
        body.put("markdown", markdown);
        body.put("page_size", "A4");
        body.put("orientation", "portrait");
        body.put("store", false);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpRequest request;
            try {
                request = HttpRequest.newBuilder(apiBase.resolve("/v1/pdf/generate"))
                        .timeout(Duration.ofSeconds(30))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Idempotency-Key", idempotencyKey)
                        .method("POST", HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                        .build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());

                if (!envelope.path("ok").asBoolean(false)) {
                    if (response.statusCode() == 429 && attempt < MAX_ATTEMPTS) {
                        pause(retryDelayMillis(response, attempt));
                        continue;
                    }
                    JsonNode error = envelope.path("error");
                    throw new InfraiException(error.path("code").asText("REQUEST_REJECTED"), error, response.statusCode());
                }
                if (response.statusCode() >= 500) {
                    throw new IOException("Infrai transport response: " + response.statusCode());
                }
                return decodePdf(envelope.path("data"));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("PDF generation interrupted", exception);
            } catch (IOException exception) {
                throw new IllegalStateException("PDF generation transport failed", exception);
            }
        }
        throw new IllegalStateException("PDF generation retry budget exhausted");
    }

    private byte[] decodePdf(JsonNode data) {
        String encoded = data.path("pdf").asText();
        if (encoded.isBlank()) {
            throw new IllegalStateException("PDF response did not contain document bytes");
        }
        return Base64.getDecoder().decode(encoded);
    }

    private long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Duration.ofSeconds(Long.parseLong(value)).toMillis())
                .orElse(250L * (1L << (attempt - 1)));
    }

    private void pause(long milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }
}
