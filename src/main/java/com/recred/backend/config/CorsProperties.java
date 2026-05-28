package com.recred.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        @DefaultValue({
                "https://recred.netlify.app",
                "http://localhost:*",
                "http://127.0.0.1:*"
        })
        List<String> allowedOriginPatterns
) {
}
