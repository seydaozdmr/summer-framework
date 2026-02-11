package io.summerframework.autoconfigure.web;

import io.summerframework.autoconfigure.SummerFrameworkProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

    private final SummerFrameworkProperties properties;

    public RequestIdFilter(SummerFrameworkProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(properties.getRequestIdHeader(), requestId);
        filterChain.doFilter(request, response);
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(properties.getRequestIdHeader());
        if (StringUtils.hasText(incoming)) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
