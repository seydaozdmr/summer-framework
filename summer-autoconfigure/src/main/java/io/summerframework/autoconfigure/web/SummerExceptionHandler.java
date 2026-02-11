package io.summerframework.autoconfigure.web;

import io.summerframework.autoconfigure.SummerFrameworkProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class SummerExceptionHandler {

    private final SummerFrameworkProperties properties;

    public SummerExceptionHandler(SummerFrameworkProperties properties) {
        this.properties = properties;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception,
                                                              HttpServletRequest request) {
        List<String> messages = exception.getBindingResult().getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        String message = String.join(", ", messages);

        ApiResponse<Void> response = ApiResponse.failure(
                message,
                HttpStatus.BAD_REQUEST.value(),
                requestId(request),
                properties.isIncludeTimestamp());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException exception,
                                                                  HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String reason = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();

        ApiResponse<Void> response = ApiResponse.failure(
                reason,
                status.value(),
                requestId(request),
                properties.isIncludeTimestamp());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception exception, HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.failure(
                exception.getMessage() != null ? exception.getMessage() : "Unexpected server error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                requestId(request),
                properties.isIncludeTimestamp());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String formatFieldError(FieldError fieldError) {
        String defaultMessage = fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "is invalid";
        return fieldError.getField() + " " + defaultMessage;
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return value != null ? value.toString() : null;
    }
}
