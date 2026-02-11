package io.summerframework.autoconfigure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.summerframework.autoconfigure.SummerFrameworkProperties;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class SummerResponseAdvice implements ResponseBodyAdvice<Object> {

    private final SummerFrameworkProperties properties;
    private final ObjectMapper objectMapper;

    public SummerResponseAdvice(SummerFrameworkProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getContainingClass().isAnnotationPresent(RestController.class);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body instanceof ApiResponse<?>) {
            return body;
        }

        ApiResponse<Object> wrapped = ApiResponse.success(body, resolveRequestId(), properties.isIncludeTimestamp());
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            try {
                return objectMapper.writeValueAsString(wrapped);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Cannot serialize ApiResponse", ex);
            }
        }
        return wrapped;
    }

    private String resolveRequestId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        Object requestId = attributes.getRequest().getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        if (requestId == null) {
            return null;
        }

        String requestIdValue = requestId.toString();
        if (!StringUtils.hasText(requestIdValue)) {
            return null;
        }
        return requestIdValue;
    }
}
