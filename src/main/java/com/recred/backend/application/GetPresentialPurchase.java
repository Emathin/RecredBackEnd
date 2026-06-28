package com.recred.backend.application;

import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetPresentialPurchase {

    private final PurchaseRepository purchaseRepository;

    public GetPresentialPurchase(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public GetPresentialPurchaseResponse execute(GetPresentialPurchaseRequest request) {
        Purchase purchase = purchaseRepository.findById(request.purchaseId())
                .orElseThrow(() -> new PurchaseNotFoundException(request.purchaseId()));
        
        return buildResponse(purchase);
    }

    private GetPresentialPurchaseResponse buildResponse(Purchase purchase) {
        List<GetPresentialPurchaseResponse.ItemResponse> items = new ArrayList<>();
        
        if (purchase.getItems() != null) {
            Map<UUID, List<Map.Entry<com.recred.backend.domain.models.Producto, Integer>>> 
                promoGroups =
                purchase.getItems().entrySet().stream()
                    .filter(entry -> entry.getKey().getPromocionId() != null)
                    .collect(Collectors.groupingBy(entry -> entry.getKey().getPromocionId()));

            purchase.getItems().entrySet().stream()
                .filter(entry -> entry.getKey().getPromocionId() == null)
                .forEach(entry -> items.add(mapItem(entry.getKey(), entry.getValue())));

            promoGroups.forEach((promoId, entries) -> {
                if (!entries.isEmpty()) {
                    String promoName = entries.get(0).getKey().getPromocionNombre();
                    int quantity = entries.get(0).getValue();
                    BigDecimal unitPrice = entries.stream()
                        .map(entry -> entry.getKey().getPrecio())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    boolean requierePrep = entries.stream()
                        .anyMatch(entry -> Boolean.TRUE.equals(
                                entry.getKey().getRequierePreparacion()));

                    items.add(new GetPresentialPurchaseResponse.ItemResponse(
                        promoId, promoName, quantity, unitPrice, requierePrep, 
                        null, promoId, promoName
                    ));
                }
            });
        }

        return new GetPresentialPurchaseResponse(
                purchase.getId(),
                purchase.getStudent() != null ? purchase.getStudent().getId() : null,
                purchase.getTotalAmount(),
                purchase.getStatus() != null ? purchase.getStatus().name() : null,
                purchase.getStatus() != null ? purchase.getStatus().getLabel() : null,
                purchase.getPaymentMethod() != null ? purchase.getPaymentMethod().name() : null,
                purchase.getDate(),
                purchase.getTipo() != null ? purchase.getTipo().name() : null,
                purchase.getWithdrawalCode(),
                purchase.getPickupSlotDescription(),
                purchase.getPickupDate(),
                purchase.getPickupSlotStartTime(),
                items
        );
    }

    private GetPresentialPurchaseResponse.ItemResponse mapItem(
            com.recred.backend.domain.models.Producto product,
            Integer quantity
    ) {
        return new GetPresentialPurchaseResponse.ItemResponse(
                product.getId(),
                product.getNombre(),
                quantity,
                product.getPrecio(),
                product.getRequierePreparacion(),
                product.getCupoMaximoDiario(),
                null,
                null
        );
    }
}
