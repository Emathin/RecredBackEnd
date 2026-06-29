package com.recred.backend.application;

import com.recred.backend.domain.models.StudentConsumptionDetail;
import com.recred.backend.domain.models.StudentConsumptionProduct;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetLoggedUserConsumptionDetailResponse(
        UUID studentId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        int purchaseCount,
        List<PurchaseResponse> purchases
) {

    public static GetLoggedUserConsumptionDetailResponse from(StudentConsumptionDetail detail) {
        return new GetLoggedUserConsumptionDetailResponse(
                detail.studentId(),
                detail.from(),
                detail.to(),
                detail.totalAmount(),
                detail.purchaseCount(),
                detail.purchasesOrEmpty().stream()
                        .map(PurchaseResponse::from)
                        .toList()
        );
    }

    public record PurchaseResponse(
            UUID purchaseId,
            LocalDateTime dateTime,
            String status,
            String paymentMethod,
            BigDecimal totalAmount,
            UUID buffetId,
            String buffetName,
            List<ProductResponse> products
    ) {

        private static PurchaseResponse from(StudentConsumptionPurchase purchase) {
            return new PurchaseResponse(
                    purchase.purchaseId(),
                    purchase.dateTime(),
                    purchase.statusName(),
                    purchase.paymentMethodName(),
                    purchase.totalAmount(),
                    purchase.buffetId(),
                    purchase.buffetName(),
                    purchase.productsOrEmpty().stream()
                            .map(ProductResponse::from)
                            .toList()
            );
        }
    }

    public record ProductResponse(
            UUID productId,
            String productName,
            String categoryName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            List<String> healthClassifications
    ) {

        private static ProductResponse from(StudentConsumptionProduct product) {
            return new ProductResponse(
                    product.productId(),
                    product.productName(),
                    product.categoryName(),
                    product.quantity(),
                    product.unitPrice(),
                    product.subtotal(),
                    product.healthClassificationsOrEmpty()
            );
        }
    }
}
