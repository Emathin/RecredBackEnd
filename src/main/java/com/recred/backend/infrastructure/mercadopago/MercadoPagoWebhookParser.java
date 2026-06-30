package com.recred.backend.infrastructure.mercadopago;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class MercadoPagoWebhookParser {

    public WebhookEvent parseEvent(Map<String, Object> body, Map<String, String> queryParams) {
        String type = extractType(body, queryParams);
        String paymentId = extractPaymentId(body, queryParams);
        boolean isPayment = "payment".equals(type) && paymentId != null 
                && !paymentId.isEmpty() && !"null".equals(paymentId);
        String notificationId = extractNotificationIdFromBody(body);
        
        return new WebhookEvent(paymentId, isPayment, notificationId);
    }

    private String extractType(Map<String, Object> body, Map<String, String> queryParams) {
        String type = extractTypeFromBody(body);
        return type != null && !"null".equals(type) ? type : extractTypeFromQuery(queryParams);
    }

    private String extractTypeFromBody(Map<String, Object> body) {
        return body != null && body.get("type") != null ? String.valueOf(body.get("type")) : null;
    }

    private String extractTypeFromQuery(Map<String, String> queryParams) {
        return queryParams.getOrDefault("type", queryParams.get("topic"));
    }

    private String extractPaymentId(Map<String, Object> body, Map<String, String> queryParams) {
        String paymentId = extractPaymentIdFromBody(body);
        if (paymentId != null && !"null".equals(paymentId)) {
            return paymentId;
        }
        return extractPaymentIdFromQuery(queryParams);
    }

    private String extractPaymentIdFromBody(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        if (body.containsKey("data")) {
            Map<?, ?> data = (Map<?, ?>) body.get("data");
            if (data != null && data.get("id") != null) {
                return String.valueOf(data.get("id"));
            }
            return null;
        }
        return body.get("id") != null ? String.valueOf(body.get("id")) : null;
    }

    private String extractPaymentIdFromQuery(Map<String, String> queryParams) {
        return queryParams.getOrDefault("data.id", queryParams.get("id"));
    }

    private String extractNotificationIdFromBody(Map<String, Object> body) {
        if (body != null && body.get("id") != null) {
            String val = String.valueOf(body.get("id"));
            return !"null".equals(val) ? val : null;
        }
        return null;
    }
}
