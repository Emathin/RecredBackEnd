package com.recred.backend.infrastructure.email;

public record SmtpMailProperties(
        String host,
        int port,
        String username,
        String password,
        String from,
        boolean auth,
        boolean startTls,
        String sslTrust,
        int connectionTimeout,
        int timeout,
        int writeTimeout
) {
    public boolean enabled() {
        return host != null && !host.isBlank();
    }
}
