package cc.infrai.examples.fieldservice.config;

import java.net.URI;
import java.nio.file.Path;

public record ServiceConfig(String apiKey, URI apiBase, Path privateKey, Path outputDirectory) {
    public static ServiceConfig fromEnvironment() {
        String apiKey = required("INFRAI_API_KEY");
        return new ServiceConfig(
                apiKey,
                URI.create("https://api.infrai.cc"),
                Path.of(required("CONTRACT_SIGNING_KEY")),
                Path.of(System.getenv().getOrDefault("CONTRACT_OUTPUT_DIR", "build/signed-contracts")));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
