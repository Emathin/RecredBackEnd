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

    // Access Token de Mercado Pago (Sandbox o Produccion)
    private String accessToken;

    // URL de exito para redireccion tras el pago
    private String successUrl;

    // URL de fallo para redireccion tras el pago
    private String failureUrl;

    // URL de pago pendiente para redireccion tras el pago
    private String pendingUrl;

    // Secreto del Webhook para validar la firma x-signature
    private String webhookSecret;
}
