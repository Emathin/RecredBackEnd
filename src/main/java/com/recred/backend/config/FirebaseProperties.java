package com.recred.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
        boolean enabled,
        String serviceAccountPath
) {

    public boolean hasServiceAccountPath() {
        return serviceAccountPath != null && !serviceAccountPath.isBlank();
    }
}
