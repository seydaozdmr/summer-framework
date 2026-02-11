package io.summerframework.core.web;

final class RequestTimeoutException extends RuntimeException {

    RequestTimeoutException(String message) {
        super(message);
    }
}
