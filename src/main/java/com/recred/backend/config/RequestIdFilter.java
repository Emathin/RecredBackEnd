package com.recred.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.nanoTime();
        boolean completed = false;

        RequestIdContext.put(requestId);
        response.setHeader(RequestIdContext.HEADER_NAME, requestId);

        try {
            filterChain.doFilter(request, response);
            completed = true;
        } finally {
            logRequest(request, response, startedAt, completed);
            RequestIdContext.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        return incomingRequestId(request)
                .filter(this::isValidRequestId)
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    private Optional<String> incomingRequestId(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(RequestIdContext.HEADER_NAME))
                .map(String::trim);
    }

    private boolean isValidRequestId(String requestId) {
        return StringUtils.hasText(requestId)
                && requestId.length() <= MAX_REQUEST_ID_LENGTH
                && REQUEST_ID_PATTERN.matcher(requestId).matches();
    }

    private void logRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long startedAt,
            boolean completed
    ) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        int status = completed ? response.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR.value();

        LOGGER.atInfo()
                .setMessage("http_request")
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMs)
                .log();
    }
}
