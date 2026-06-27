package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.util.List;
import java.util.stream.Collectors;

public class GetScheduledPickups {

    private final PurchaseRepository purchaseRepository;

    public GetScheduledPickups(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<GetScheduledPickupsResponse> execute(GetScheduledPickupsRequest request) {
        List<Purchase> purchases = purchaseRepository.findScheduledPickups(
                request.buffetId(),
                request.fecha(),
                request.estadoRetiro(),
                request.franjaId(),
                request.search()
        );

        return purchases.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private GetScheduledPickupsResponse toResponse(Purchase purchase) {
        String studentName = null;
        java.util.UUID studentId = null;
        if (purchase.getStudent() != null) {
            studentId = purchase.getStudent().getId();
            studentName = purchase.getStudent().getNombre() + " "
                    + purchase.getStudent().getApellido();
        }

        List<GetScheduledPickupsResponse.PurchaseItemResponse> items = List.of();
        if (purchase.getItems() != null) {
            items = purchase.getItems().entrySet().stream()
                    .map(entry -> new GetScheduledPickupsResponse.PurchaseItemResponse(
                            entry.getKey().getId(),
                            entry.getKey().getNombre(),
                            entry.getValue(),
                            entry.getKey().getPrecio()
                    ))
                    .collect(Collectors.toList());
        }

        return new GetScheduledPickupsResponse(
                purchase.getId(),
                studentId,
                studentName,
                purchase.getTotalAmount(),
                purchase.getStatus() != null ? purchase.getStatus().name() : null,
                purchase.getStatus() != null ? purchase.getStatus().getLabel() : null,
                purchase.getWithdrawalStatus() != null
                        ? purchase.getWithdrawalStatus().name() : null,
                purchase.getPaymentMethod() != null
                        ? purchase.getPaymentMethod().name() : null,
                purchase.getDate(),
                purchase.getWithdrawalCode(),
                purchase.getPickupSlotId(),
                purchase.getPickupSlotDescription(),
                purchase.getPickupDate(),
                items
        );
    }
}

