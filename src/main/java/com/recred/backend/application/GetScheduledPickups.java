package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GetScheduledPickups {

    private final PurchaseRepository purchaseRepository;

    public GetScheduledPickups(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<GetScheduledPickupsResponse> execute(GetScheduledPickupsRequest request) {
        List<Purchase> purchases = fetchScheduledPickups(request);
        return mapToResponse(purchases);
    }

    private List<Purchase> fetchScheduledPickups(GetScheduledPickupsRequest request) {
        return purchaseRepository.findScheduledPickups(
                request.buffetId(),
                request.fecha(),
                request.estadoRetiro(),
                request.status(),
                request.franjaId(),
                request.search()
        );
    }

    private List<GetScheduledPickupsResponse> mapToResponse(List<Purchase> purchases) {
        return purchases.stream()
                .map(this::toResponse)
                .toList();
    }

    private GetScheduledPickupsResponse toResponse(Purchase purchase) {
        return new GetScheduledPickupsResponse(
                purchase.getId(),
                getStudentId(purchase),
                getStudentName(purchase),
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
                mapItems(purchase)
        );
    }

    private UUID getStudentId(Purchase purchase) {
        if (purchase.getStudent() == null) {
            return null;
        }
        return purchase.getStudent().getId();
    }

    private String getStudentName(Purchase purchase) {
        if (purchase.getStudent() == null) {
            return null;
        }
        return purchase.getStudent().getNombre() + " "
                + purchase.getStudent().getApellido();
    }

    private List<GetScheduledPickupsResponse.PurchaseItemResponse> mapItems(
            Purchase purchase) {
        if (purchase.getItems() == null) {
            return List.of();
        }
        return purchase.getItems().entrySet().stream()
                .map(this::toItemResponse)
                .toList();
    }

    private GetScheduledPickupsResponse.PurchaseItemResponse toItemResponse(
            Map.Entry<Producto, Integer> entry) {
        return new GetScheduledPickupsResponse.PurchaseItemResponse(
                entry.getKey().getId(),
                entry.getKey().getNombre(),
                entry.getValue(),
                entry.getKey().getPrecio()
        );
    }
}
