package io.summerframework.core.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

final class ApiEnvelope {

    private ApiEnvelope() {
    }

    static Map<String, Object> success(Object data, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("path", path);
        body.put("timestamp", Instant.now().toString());
        body.put("data", data);
        return body;
    }

    static Map<String, Object> error(String message, int status, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("path", path);
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", message);
        return body;
    }
}
