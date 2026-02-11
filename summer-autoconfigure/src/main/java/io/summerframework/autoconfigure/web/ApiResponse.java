package io.summerframework.autoconfigure.web;

import java.time.Instant;

public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;
    private final Instant timestamp;
    private final String requestId;

    private ApiResponse(boolean success, T data, ErrorDetail error, Instant timestamp, String requestId) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = timestamp;
        this.requestId = requestId;
    }

    public static <T> ApiResponse<T> success(T data, String requestId, boolean includeTimestamp) {
        Instant timestamp = includeTimestamp ? Instant.now() : null;
        return new ApiResponse<>(true, data, null, timestamp, requestId);
    }

    public static <T> ApiResponse<T> failure(String message, int status, String requestId, boolean includeTimestamp) {
        Instant timestamp = includeTimestamp ? Instant.now() : null;
        ErrorDetail error = new ErrorDetail(message, status);
        return new ApiResponse<>(false, null, error, timestamp, requestId);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorDetail getError() {
        return error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public static class ErrorDetail {

        private final String message;
        private final int status;

        public ErrorDetail(String message, int status) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public int getStatus() {
            return status;
        }
    }
}
