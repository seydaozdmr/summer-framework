package io.summerframework.core.web;

public record RestResponse(int status, Object body) {

    public static RestResponse ok(Object body) {
        return new RestResponse(200, body);
    }

    public static RestResponse created(Object body) {
        return new RestResponse(201, body);
    }

    public static RestResponse noContent() {
        return new RestResponse(204, null);
    }
}
