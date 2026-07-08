package com.recred.backend.infrastructure.mercadopago;

import com.recred.backend.config.MercadoPagoProperties;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.services.PayoutPaymentService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
public class MercadoPagoPayoutAdapter implements PayoutPaymentService {

    private final MercadoPagoProperties properties;
    private final RestTemplate restTemplate;

    public MercadoPagoPayoutAdapter(MercadoPagoProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public PaymentDetails transferToThirdParty(
            String destinationCvu,
            String destinationCuit,
            String accountHolderName,
            BigDecimal amount,
            String idempotencyKey) {
        try {
            String url = "https://api.mercadopago.com/v1/payouts";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getAccessToken());
            headers.set("X-Idempotency-Key", idempotencyKey);

            PayoutIdentification identification = new PayoutIdentification(
                    "CUIT", 
                    destinationCuit
            );
            PayoutReceiver receiver = new PayoutReceiver(
                    "cvu", 
                    destinationCvu, 
                    accountHolderName, 
                    identification
            );
            PayoutRequest requestBody = new PayoutRequest(
                    idempotencyKey, 
                    amount, 
                    "bank_transfer", 
                    receiver
            );

            HttpEntity<PayoutRequest> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<PayoutResponse> responseEntity = restTemplate
                    .postForEntity(url, entity, PayoutResponse.class);
            PayoutResponse response = responseEntity.getBody();

            if (response == null) {
                return new PaymentDetails(
                        idempotencyKey, 
                        null, 
                        amount, 
                        PaymentStatus.PENDING
                );
            }

            PaymentStatus status = switch (response.status()) {
                case "approved" -> PaymentStatus.APPROVED;
                case "rejected" -> PaymentStatus.REJECTED;
                default -> PaymentStatus.PENDING;
            };

            String payoutIdStr = response.id() != null ? response.id().toString() : idempotencyKey;

            return new PaymentDetails(payoutIdStr, null, amount, status);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error en la conexión con la API de Payouts de Mercado Pago", 
                    e
            );
        }
    }

    private record PayoutRequest(
        String payout_id,
        BigDecimal amount,
        String payment_method_id,
        PayoutReceiver receiver
    ) {}

    private record PayoutReceiver(
        String type,
        String value,
        String name,
        PayoutIdentification identification
    ) {}

    private record PayoutIdentification(
        String type,
        String number
    ) {}

    private record PayoutResponse(
        Long id,
        String status,
        String status_detail
    ) {}
}
