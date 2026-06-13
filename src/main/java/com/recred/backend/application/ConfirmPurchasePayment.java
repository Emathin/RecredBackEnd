package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class ConfirmPurchasePayment {

    private final PurchaseRepository purchaseRepository;
    private final ExternalPaymentService externalPaymentService;

    public ConfirmPurchasePayment(
            PurchaseRepository purchaseRepository,
            ExternalPaymentService externalPaymentService) {
        this.purchaseRepository = purchaseRepository;
        this.externalPaymentService = externalPaymentService;
    }

    @Transactional
    public void execute(String paymentId) {
        PaymentDetails details = externalPaymentService.getPaymentDetails(paymentId);

        if (details.status() == PaymentStatus.APPROVED && details.externalReference() != null) {
            if (details.externalReference().startsWith("PURCHASE_")) {
                UUID purchaseId = UUID.fromString(details.externalReference().substring(9));
                Purchase purchase = purchaseRepository.findById(purchaseId)
                        .orElseThrow(() -> new RuntimeException(
                                "Compra no encontrada: " + purchaseId));

                if (purchase.getStatus() == EstadoCompra.PENDIENTE) {
                    // Si requiere preparacion, queda en PENDIENTE (ya está) pero marcada como pagada?
                    // En este dominio, ENTREGADO significa que ya se le dio al alumno.
                    // Para venta espontanea, si no requiere preparacion, pasamos a ENTREGADO.
                    if (requiresPreparation(purchase)) {
                        purchase.changeStatusTo(EstadoCompra.EN_PREPARACION);
                    } else {
                        // En venta espontanea, si ya pagó y no requiere preparación, se entrega.
                        // Pero quién lo entrega? El kiosquero. 
                        // Tal vez deberíamos pasar a LISTO o simplemente dejar que el kiosquero lo entregue.
                        // El usuario dijo "concluya", así que ENTREGADO parece bien si es presencial.
                        purchase.setStatus(EstadoCompra.ENTREGADO);
                    }
                    purchaseRepository.save(purchase);
                }
            }
        }
    }

    private boolean requiresPreparation(Purchase purchase) {
        return purchase.getItems().keySet().stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.getRequierePreparacion()));
    }
}
