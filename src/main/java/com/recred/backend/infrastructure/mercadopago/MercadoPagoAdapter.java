package com.recred.backend.infrastructure.mercadopago;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.recred.backend.config.MercadoPagoProperties;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.UUID;

@Service
public class MercadoPagoAdapter implements ExternalPaymentService {

    private final MercadoPagoProperties properties;

    public MercadoPagoAdapter(MercadoPagoProperties properties) {
        this.properties = properties;
        MercadoPagoConfig.setAccessToken(properties.getAccessToken());
    }

    @Override
    public String createTopUpPreference(UUID studentId, BigDecimal amount) {
        try {
            String successUrl = properties.getSuccessUrl().trim();
            String notificationUrl = properties.getNotificationUrl().trim() 
                    + "?source_news=webhooks";

            PreferenceClient client = new PreferenceClient();

            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .id(studentId.toString())
                    .title("Carga de Saldo - RecreoPago")
                    .description("Acreditacion de saldo para consumo escolar")
                    .quantity(1)
                    .currencyId("ARS")
                    .unitPrice(amount)
                    .build();

            List<PreferenceItemRequest> items = new ArrayList<>();
            items.add(itemRequest);

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .pending(properties.getPendingUrl().trim())
                    .failure(properties.getFailureUrl().trim())
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .autoReturn("approved")
                    .backUrls(backUrls)
                    .notificationUrl(notificationUrl)
                    .externalReference(studentId.toString())
                    .build();

            Preference preference = client.create(preferenceRequest);

            return preference.getInitPoint();

        } catch (com.mercadopago.exceptions.MPApiException e) {
            String errorMsg = e.getApiResponse().getContent();
            throw new RuntimeException("Error al crear preferencia: " + errorMsg, e);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear preferencia de Mercado Pago", e);
        }
    }

    @Override
    public boolean isValidSignature(String signature, String requestId, String paymentId) {
        if (signature == null || requestId == null || paymentId == null) {
            return false;
        }

        try {
            MercadoPagoSignatureData signatureData = parseSignature(signature);
            if (signatureData == null) {
                return false;
            }

            String manifest = buildManifest(paymentId, requestId, signatureData.ts());
            String expectedHash = calculateHmacSha256(
                    manifest, properties.getWebhookSecret().trim());

            return expectedHash.equals(signatureData.hash());
        } catch (Exception e) {
            return false;
        }
    }

    private record MercadoPagoSignatureData(String ts, String hash) {}

    private MercadoPagoSignatureData parseSignature(String signature) {
        String ts = null;
        String hash = null;
        for (String part : signature.split(",")) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                if (keyValue[0].trim().equals("ts")) {
                    ts = keyValue[1].trim();
                } else if (keyValue[0].trim().equals("v1")) {
                    hash = keyValue[1].trim();
                }
            }
        }
        return (ts != null && hash != null) ? new MercadoPagoSignatureData(ts, hash) : null;
    }

    private String buildManifest(String paymentId, String requestId, String ts) {
        return "id:" + paymentId + ";request-id:" + requestId + ";ts:" + ts + ";";
    }

    private String calculateHmacSha256(String data, String secret) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder sb = new StringBuilder(rawHmac.length * 2);
        try (Formatter formatter = new Formatter(sb)) {
            for (byte b : rawHmac) {
                formatter.format("%02x", b);
            }
        }
        return sb.toString();
    }

    @Override
    public PaymentDetails getPaymentDetails(String paymentId) {
        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(paymentId));

            UUID studentId = UUID.fromString(payment.getExternalReference());

            PaymentStatus status = switch (payment.getStatus()) {
                case "approved" -> PaymentStatus.APPROVED;
                case "pending", "in_process" -> PaymentStatus.PENDING;
                case "rejected" -> PaymentStatus.REJECTED;
                case "refunded" -> PaymentStatus.REFUNDED;
                case "charged_back" -> PaymentStatus.CHARGEBACK;
                case "cancelled" -> PaymentStatus.CANCELLED;
                default -> PaymentStatus.PENDING;
            };

            return new PaymentDetails(
                    paymentId,
                    studentId,
                    payment.getTransactionAmount(),
                    status
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al recuperar detalles del pago: " + paymentId, e);
        }
    }
}
