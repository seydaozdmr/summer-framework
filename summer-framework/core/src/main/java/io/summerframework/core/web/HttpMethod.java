package io.summerframework.core.web;

enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH;

    static HttpMethod from(String method) {
        for (HttpMethod value : values()) {
            if (value.name().equalsIgnoreCase(method)) {
                return value;
            }
        }
        throw new BadRequestException("Unsupported HTTP method: " + method);
    }
}
