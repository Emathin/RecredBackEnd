package com.recred.backend.infrastructure.mercadopago;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest;
import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preapproval.Preapproval;
import com.mercadopago.resources.preference.Preference;
import com.recred.backend.config.MercadoPagoProperties;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentReference;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
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
        return createPreference(
                studentId.toString(),
                "Carga de Saldo - Recred",
                "Acreditacion de saldo para consumo escolar",
                amount,
                "ARS",
                PaymentReference.topUp(studentId).toExternalReference()
        );
    }

    @Override
    public String createScheduledTopUpPreapproval(
            UUID studentId, BigDecimal amount, Integer frequency,
            String frequencyType, String payerEmail) {
        try {
            PreapprovalClient client = new PreapprovalClient();
            PreApprovalAutoRecurringCreateRequest autoRecurring = 
                    PreApprovalAutoRecurringCreateRequest.builder()
                    .frequency(frequency)
                    .frequencyType(frequencyType)
                    .transactionAmount(amount)
                    .currencyId("ARS")
                    .build();

            PreapprovalCreateRequest request = PreapprovalCreateRequest.builder()
                    .reason("Recarga Programada - Recred")
                    .externalReference(PaymentReference.topUp(studentId).toExternalReference())
                    .autoRecurring(autoRecurring)
                    .backUrl("https://recred.netlify.app/pago-exitoso")
                    .payerEmail(payerEmail)
                    /* .status("authorized") REMOVED to avoid API errors */
                    .build();

            Preapproval preapproval = client.create(request);
            return preapproval.getInitPoint();
        } catch (com.mercadopago.exceptions.MPApiException e) {
            String errorMsg = e.getApiResponse().getContent();
            System.err.println("Preapproval MPApiException content: " + errorMsg);
            throw new RuntimeException("Error MP: " + errorMsg, e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al crear la suscripción de recarga en Mercado Pago", e);
        }
    }

    @Override
    public String createUserSubscriptionPreference(
            UUID usuarioId,
            TipoPlan plan,
            PeriodoSuscripcion periodo,
            BigDecimal amount
    ) {
        return createPreference(
                usuarioId.toString(),
                "Suscripcion " + plan.name() + " - Recred",
                "Suscripcion " + periodo.name().toLowerCase() + " para usuario",
                amount,
                "ARS",
                PaymentReference.userSubscription(usuarioId, plan, periodo).toExternalReference()
        );
    }

    @Override
    public String createSchoolSubscriptionPreference(UUID colegioId, BigDecimal amount) {
        return createPreference(
                colegioId.toString(),
                "Suscripcion Colegio - Recred",
                "Acceso mensual institucional a Recred",
                amount,
                "USD",
                PaymentReference.schoolSubscription(colegioId).toExternalReference()
        );
    }

    private String createPreference(
            String itemId,
            String title,
            String description,
            BigDecimal amount,
            String currencyId,
            String externalReference
    ) {
        try {
            String successUrl = properties.getSuccessUrl();
            String notificationUrl = properties.getNotificationUrl();
            System.err.println("MP SUCCESS: [" + successUrl + "]");
            System.err.println("MP NOTIFICATION: [" + notificationUrl + "]");
            if (successUrl != null) {
                successUrl = successUrl.trim();
            }
            if (notificationUrl != null) {
                notificationUrl = notificationUrl.trim() + "?source_news=webhooks";
            }


            PreferenceClient client = new PreferenceClient();

            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .id(itemId)
                    .title(title)
                    .description(description)
                    .quantity(1)
                    .currencyId(currencyId)
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
                    .autoReturn(successUrl != null && successUrl.startsWith("https") 
                            ? "approved" : null)
                    .backUrls(backUrls)
                    .notificationUrl(notificationUrl)
                    .externalReference(externalReference)
                    .build();

            Preference preference = client.create(preferenceRequest);

            return preference.getInitPoint();

        } catch (com.mercadopago.exceptions.MPApiException e) {
            String errorMsg = e.getApiResponse().getContent();
            System.err.println("MPApiException content: " + errorMsg);
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

            PaymentStatus status = switch (payment.getStatus()) {
                case "approved" -> PaymentStatus.APPROVED;
                case "pending", "in_process" -> PaymentStatus.PENDING;
                case "rejected" -> PaymentStatus.REJECTED;
                case "refunded" -> PaymentStatus.REFUNDED;
                case "charged_back" -> PaymentStatus.CHARGEBACK;
                case "cancelled" -> PaymentStatus.CANCELLED;
                default -> PaymentStatus.PENDING;
            };

            return PaymentDetails.fromReference(
                    paymentId,
                    PaymentReference.fromExternalReference(payment.getExternalReference()),
                    payment.getTransactionAmount(),
                    status
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al recuperar detalles del pago: " + paymentId, e);
        }
    }
}
