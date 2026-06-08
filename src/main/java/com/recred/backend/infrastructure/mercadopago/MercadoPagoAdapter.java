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
        // Inicializa la biblioteca con el Access Token de pruebas o produccion
        MercadoPagoConfig.setAccessToken(properties.getAccessToken());
    }

    @Override
    public String createTopUpPreference(UUID studentId, BigDecimal amount) {
        try {
            PreferenceClient client = new PreferenceClient();

            // Configura los detalles del item (carga de saldo)
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

            // Configura las URLs de retorno (back_urls)
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://5cpbvx-ip-179-37-104-36.tunnelmole.net/tutor" /* properties.getSuccessUrl() */)
                    .pending("https://5cpbvx-ip-179-37-104-36.tunnelmole.net/tutor" /* properties.getPendingUrl() */)
                    .failure("https://5cpbvx-ip-179-37-104-36.tunnelmole.net/tutor" /* properties.getFailureUrl() */)
                    .build();

            // Crea la solicitud de preferencia con items y back_urls
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .autoReturn("approved")
                    .backUrls(backUrls)
                    .notificationUrl("https://gej7kq-ip-179-37-104-36.tunnelmole.net/api/v1/payments/webhook") // cambiar por url expuesta
                    .externalReference(studentId.toString())
                    .build();

            // Ejecuta la creacion en Mercado Pago
            Preference preference = client.create(preferenceRequest);

            // Retorna el init_point para redireccion web
            return preference.getInitPoint();

        } catch (com.mercadopago.exceptions.MPApiException e) {
            String errorMsg = e.getApiResponse().getContent();
            System.err.println("Mercado Pago API Error Details: " + errorMsg);
            throw new RuntimeException("Error al crear preferencia: " + errorMsg, e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al crear preferencia de Mercado Pago", e);
        }
    }

    @Override
    public boolean isValidSignature(String signature, String requestId, String paymentId) {
        if (signature == null || requestId == null || paymentId == null) {
            return false;
        }

        try {
            // Extrae timestamp y hash del header x-signature
            String[] parts = signature.split(",");
            String ts = null;
            String hash = null;

            for (String part : parts) {
                String[] keyValue = part.split("=");
                if (keyValue.length == 2) {
                    if (keyValue[0].trim().equals("ts")) {
                        ts = keyValue[1].trim();
                    } else if (keyValue[0].trim().equals("v1")) {
                        hash = keyValue[1].trim();
                    }
                }
            }

            if (ts == null || hash == null) {
                return false;
            }

            // Construye el manifiesto dinamicamente segun documentacion oficial:
            // "id:[id];request-id:[request-id];ts:[ts];" (omitiendo campos ausentes)
            StringBuilder manifest = new StringBuilder();
            if (paymentId != null) {
                manifest.append("id:").append(paymentId).append(";");
            }
            if (requestId != null) {
                manifest.append("request-id:").append(requestId).append(";");
            }
            if (ts != null) {
                manifest.append("ts:").append(ts).append(";");
            }

            // Calcula HMAC-SHA256 usando Java nativo
            SecretKeySpec signingKey = new SecretKeySpec(
                    properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(manifest.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(rawHmac.length * 2);
            Formatter formatter = new Formatter(sb);
            for (byte b : rawHmac) {
                formatter.format("%02x", b);
            }
            String sha = sb.toString();

            return sha.equals(hash);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PaymentDetails getPaymentDetails(String paymentId) {
        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(paymentId));

            // Recupera el ID del alumno desde el externalReference guardado en la preferencia
            UUID studentId = UUID.fromString(payment.getExternalReference());

            // Mapea el estado de Mercado Pago a nuestro dominio
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
