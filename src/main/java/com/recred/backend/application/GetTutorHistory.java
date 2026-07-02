package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetTutorHistory {

    private final PurchaseRepository purchaseRepository;
    private final TutorRepository tutorRepository;

    public GetTutorHistory(PurchaseRepository purchaseRepository,
                                  TutorRepository tutorRepository) {
        this.purchaseRepository = purchaseRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional(readOnly = true)
    public List<GetHistoryByStudentResponse> execute(UUID tutorId) {
        return mapToResponse(purchaseRepository.findHistorialByTutor(tutorId));
    }

    @Transactional(readOnly = true)
    public List<GetHistoryByStudentResponse> executeByCognitoSubject(String cognitoSubject) {
        UUID tutorId = tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new RuntimeException(
                        "Tutor no encontrado para el usuario autenticado"))
                .getId();
        return mapToResponse(purchaseRepository.findHistorialByTutor(tutorId));
    }

    private List<GetHistoryByStudentResponse> mapToResponse(List<Purchase> purchases) {
        return purchases.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private GetHistoryByStudentResponse toResponse(Purchase purchase) {
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

        List<GetHistoryByStudentResponse.Item> items = mapToResponseItems(purchase);

        return new GetHistoryByStudentResponse(
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

    private List<GetHistoryByStudentResponse.Item> mapToResponseItems(Purchase purchase) {
        if (purchase.getItems() == null) {
            return null;
        }
        return purchase.getItems().entrySet().stream()
                .map(entry -> new GetHistoryByStudentResponse.Item(
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