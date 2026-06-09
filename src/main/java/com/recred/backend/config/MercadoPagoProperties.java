package com.recred.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mercadopago")
@Getter
@Setter
public class MercadoPagoProperties {

    private String accessToken;

    private String successUrl;

    private String failureUrl;

    private String pendingUrl;

    private String notificationUrl;

    private String webhookSecret;

}
