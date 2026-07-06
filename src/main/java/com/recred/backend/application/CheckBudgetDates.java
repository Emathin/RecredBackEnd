package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PromotionRepository;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CheckBudgetDates {

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final ValidadorPresupuestoService budgetValidator;

    public CheckBudgetDates(ProductRepository productRepository,
                            PromotionRepository promotionRepository,
                            ValidadorPresupuestoService budgetValidator) {
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.budgetValidator = budgetValidator;
    }

    public CheckBudgetDatesResponse execute(CheckBudgetDatesRequest request) {
        CartTotals totals = this.calculateCartTotals(request.items());
        List<CheckBudgetDatesResponse.DateBudgetStatus> results =
                this.validateDates(request.studentId(), totals, request.dates());
        return new CheckBudgetDatesResponse(results);
    }

    private record CartTotals(
            BigDecimal totalCartAmount,
            Map<UUID, BigDecimal> amountsByCategory) {}

    private CartTotals calculateCartTotals(
            List<CheckBudgetDatesRequest.CartItemRequest> items) {
        BigDecimal totalCartAmount = BigDecimal.ZERO;
        Map<UUID, BigDecimal> amountsByCategory = new HashMap<>();

        if (items != null) {
            for (CheckBudgetDatesRequest.CartItemRequest item : items) {
                Optional<Producto> productOpt = productRepository.findById(item.productId());
                if (productOpt.isPresent()) {
                    totalCartAmount = totalCartAmount.add(
                            processProductItem(productOpt.get(), item.quantity(), amountsByCategory)
                    );
                } else {
                    totalCartAmount = totalCartAmount.add(
                            processPromotionItem(item, amountsByCategory)
                    );
                }
            }
        }

        return new CartTotals(totalCartAmount, amountsByCategory);
    }

    private BigDecimal processProductItem(
            Producto product,
            int quantityVal,
            Map<UUID, BigDecimal> amountsByCategory) {
        BigDecimal price = product.getPrecio() != null
                ? product.getPrecio()
                : BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.valueOf(quantityVal);
        BigDecimal itemTotal = price.multiply(quantity);

        if (product.getCategoria() != null && product.getCategoria().getId() != null) {
            UUID categoryId = product.getCategoria().getId();
            amountsByCategory.put(categoryId, amountsByCategory
                    .getOrDefault(categoryId, BigDecimal.ZERO).add(itemTotal));
        }

        return itemTotal;
    }

    private BigDecimal processPromotionItem(
            CheckBudgetDatesRequest.CartItemRequest item,
            Map<UUID, BigDecimal> amountsByCategory) {
        Optional<Promotion> promotionOpt = promotionRepository.findById(item.productId());
        if (promotionOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Product or Promotion not found: " + item.productId());
        }

        Promotion promotion = promotionOpt.get();
        BigDecimal promotionTotal = BigDecimal.ZERO;

        if (promotion.getProducts() != null) {
            BigDecimal discountPercentage = promotion.getDiscountPercentage() != null
                    ? promotion.getDiscountPercentage()
                    : BigDecimal.ZERO;
            BigDecimal multiplier = BigDecimal.ONE.subtract(
                    discountPercentage.divide(
                            new BigDecimal("100"),
                            4,
                            RoundingMode.HALF_UP
                    )
            );

            for (Producto p : promotion.getProducts()) {
                BigDecimal originalPrice = p.getPrecio() != null
                        ? p.getPrecio()
                        : BigDecimal.ZERO;
                BigDecimal discountedPrice = originalPrice
                        .multiply(multiplier)
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal quantity = BigDecimal.valueOf(item.quantity());
                BigDecimal itemTotal = discountedPrice.multiply(quantity);

                promotionTotal = promotionTotal.add(itemTotal);

                if (p.getCategoria() != null && p.getCategoria().getId() != null) {
                    UUID categoryId = p.getCategoria().getId();
                    amountsByCategory.put(categoryId, amountsByCategory
                            .getOrDefault(categoryId, BigDecimal.ZERO).add(itemTotal));
                }
            }
        }

        return promotionTotal;
    }

    private List<CheckBudgetDatesResponse.DateBudgetStatus> validateDates(
            UUID studentId, CartTotals totals, List<String> dates) {
        List<CheckBudgetDatesResponse.DateBudgetStatus> results = new ArrayList<>();
        if (dates != null) {
            for (String dateStr : dates) {
                results.add(validateSingleDate(studentId, totals, dateStr));
            }
        }
        return results;
    }

    private CheckBudgetDatesResponse.DateBudgetStatus validateSingleDate(
            UUID studentId, CartTotals totals, String dateStr) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            return new CheckBudgetDatesResponse.DateBudgetStatus(
                    dateStr, true, "Formato de fecha inválido.");
        }

        LocalDateTime momentoCompra = LocalDateTime.of(date, LocalTime.of(8, 0));

        Optional<String> blockReason = budgetValidator.obtenerMotivoBloqueoPresupuesto(
                studentId,
                totals.totalCartAmount(),
                totals.amountsByCategory(),
                momentoCompra
        );

        return blockReason
                .map(reason -> new CheckBudgetDatesResponse.DateBudgetStatus(
                        dateStr, true, reason))
                .orElseGet(() -> new CheckBudgetDatesResponse.DateBudgetStatus(
                        dateStr, false, null));
    }
}
