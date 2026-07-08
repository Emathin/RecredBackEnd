package com.recred.backend.application;

import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Producto;
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

    public GetPresentialPurchaseResponse execute(GetPresentialPurchaseRequest purchaseRequest) {
        Purchase purchase = findPurchase(purchaseRequest.purchaseId());

        return responseFrom(purchase);
    }

    private Purchase findPurchase(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    private GetPresentialPurchaseResponse responseFrom(Purchase purchase) {
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
                itemResponsesFrom(purchase)
        );
    }

    private List<GetPresentialPurchaseResponse.ItemResponse> itemResponsesFrom(
            Purchase purchase) {
        if (purchase.getItems() == null) {
            return List.of();
        }

        List<GetPresentialPurchaseResponse.ItemResponse> itemResponses = new ArrayList<>();
        itemResponses.addAll(regularItemResponsesFrom(purchase));
        itemResponses.addAll(promotionItemResponsesFrom(purchase));
        return itemResponses;
    }

    private List<GetPresentialPurchaseResponse.ItemResponse> regularItemResponsesFrom(
            Purchase purchase) {
        return purchase.getItems().entrySet().stream()
                .filter(entry -> !productBelongsToPromotion(entry.getKey()))
                .map(entry -> regularItemResponseFrom(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<GetPresentialPurchaseResponse.ItemResponse> promotionItemResponsesFrom(
            Purchase purchase) {
        return promotionGroupsFrom(purchase).entrySet().stream()
                .map(entry -> promotionItemResponseFrom(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<UUID, List<Map.Entry<Producto, Integer>>> promotionGroupsFrom(
            Purchase purchase) {
        return purchase.getItems().entrySet().stream()
                .filter(entry -> productBelongsToPromotion(entry.getKey()))
                .collect(Collectors.groupingBy(entry -> entry.getKey().getPromocionId()));
    }

    private boolean productBelongsToPromotion(Producto product) {
        return product.getPromocionId() != null;
    }

    private GetPresentialPurchaseResponse.ItemResponse regularItemResponseFrom(
            Producto product,
            Integer quantity) {
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

    private GetPresentialPurchaseResponse.ItemResponse promotionItemResponseFrom(
            UUID promotionId,
            List<Map.Entry<Producto, Integer>> promotionItems) {
        Producto firstProduct = promotionItems.get(0).getKey();
        return new GetPresentialPurchaseResponse.ItemResponse(
                promotionId,
                firstProduct.getPromocionNombre(),
                promotionItems.get(0).getValue(),
                promotionUnitPrice(promotionItems),
                promotionRequiresPreparation(promotionItems),
                null,
                promotionId,
                firstProduct.getPromocionNombre()
        );
    }

    private BigDecimal promotionUnitPrice(List<Map.Entry<Producto, Integer>> promotionItems) {
        return promotionItems.stream()
                .map(entry -> entry.getKey().getPrecio())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean promotionRequiresPreparation(
            List<Map.Entry<Producto, Integer>> promotionItems) {
        return promotionItems.stream()
                .anyMatch(entry -> Boolean.TRUE.equals(
                        entry.getKey().getRequierePreparacion()));
    }
}
