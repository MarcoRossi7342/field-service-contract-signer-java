package cc.infrai.examples.fieldservice.infrai;

import com.fasterxml.jackson.databind.JsonNode;

public final class InfraiException extends RuntimeException {
    private final String code;
    private final int statusCode;

    public InfraiException(String code, JsonNode error, int statusCode) {
        super(error.path("message").asText("Infrai request was rejected"));
        this.code = code;
        this.statusCode = statusCode;
    }

    public String code() { return code; }
    public int statusCode() { return statusCode; }
}
