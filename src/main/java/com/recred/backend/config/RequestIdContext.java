package com.recred.backend.config;

import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

public final class RequestIdContext {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private RequestIdContext() {
    }

    public static void put(String requestId) {
        MDC.put(MDC_KEY, requestId);
    }

    public static Optional<String> currentRequestId() {
        return Optional.ofNullable(MDC.get(MDC_KEY))
                .filter(StringUtils::hasText);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
