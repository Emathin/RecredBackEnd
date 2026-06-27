package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ObtenerHistorialPendienteAlumno {

    private final PurchaseRepository purchaseRepository;

    public ObtenerHistorialPendienteAlumno(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public List<ObtenerHistorialPendienteAlumnoResponse> execute(
            ObtenerHistorialPendienteAlumnoRequest request) {
        List<Purchase> purchases = purchaseRepository.findPendientesByAlumno(request.alumnoId());
        return purchases.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ObtenerHistorialPendienteAlumnoResponse toResponse(Purchase purchase) {
        UUID studentId = purchase.getStudent() != null
                ? purchase.getStudent().getId() : null;
        String status = purchase.getStatus() != null
                ? purchase.getStatus().name() : null;
        String statusLabel = purchase.getStatus() != null
                ? purchase.getStatus().getLabel() : null;
        String paymentMethod = purchase.getPaymentMethod() != null
                ? purchase.getPaymentMethod().name() : null;
        String tipo = purchase.getTipo() != null
                ? purchase.getTipo().name() : null;
        String withdrawalStatus = purchase.getWithdrawalStatus() != null
                ? purchase.getWithdrawalStatus().name() : null;

        List<ObtenerHistorialPendienteAlumnoResponse.Item> items = null;
        if (purchase.getItems() != null) {
            items = purchase.getItems().entrySet().stream()
                    .map(entry -> new ObtenerHistorialPendienteAlumnoResponse.Item(
                            entry.getKey().getId(),
                            entry.getKey().getNombre(),
                            entry.getValue(),
                            entry.getKey().getPrecio(),
                            entry.getKey().getRequierePreparacion(),
                            entry.getKey().getCupoMaximoDiario(),
                            entry.getKey().getPromocionId(),
                            entry.getKey().getPromocionNombre()
                    ))
                    .collect(Collectors.toList());
        }

        return new ObtenerHistorialPendienteAlumnoResponse(
                purchase.getId(),
                studentId,
                purchase.getTotalAmount(),
                status,
                statusLabel,
                paymentMethod,
                purchase.getDate(),
                items,
                tipo,
                purchase.getWithdrawalCode(),
                purchase.getPickupSlotDescription(),
                purchase.getPickupDate(),
                purchase.getPickupSlotStartTime(),
                withdrawalStatus
        );
    }
}

