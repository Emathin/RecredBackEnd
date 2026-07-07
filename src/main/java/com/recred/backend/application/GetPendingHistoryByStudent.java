package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetPendingHistoryByStudent {

    private final PurchaseRepository purchaseRepository;

    public GetPendingHistoryByStudent(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<GetPendingHistoryByStudentResponse> execute(
            GetPendingHistoryByStudentRequest request) {
        List<Purchase> purchases = this.findPendingHistoryByStudent(request.alumnoId());
        return this.mapToResponse(purchases);
    }

    private List<Purchase> findPendingHistoryByStudent(UUID studentId) {
        return purchaseRepository.findPendientesByAlumno(studentId);
    }

    private List<GetPendingHistoryByStudentResponse> mapToResponse(List<Purchase> purchases) {
        return purchases.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private GetPendingHistoryByStudentResponse toResponse(Purchase purchase) {
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

        List<GetPendingHistoryByStudentResponse.Item> items = this.mapToResponseItems(purchase);

        return new GetPendingHistoryByStudentResponse(
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

    private List<GetPendingHistoryByStudentResponse.Item> mapToResponseItems(Purchase purchase) {
        if (purchase.getItems() == null) {
            return null;
        }
        return purchase.getItems().entrySet().stream()
                .map(entry -> new GetPendingHistoryByStudentResponse.Item(
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
}

