package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PromotionRepository;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import java.math.BigDecimal;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
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
        
        if (items == null) {
            return new CartTotals(ZERO, new HashMap<>());
        }

        BigDecimal totalCartAmount = ZERO;
        Map<UUID, BigDecimal> amountsByCategory = new HashMap<>();

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

        return new CartTotals(totalCartAmount, amountsByCategory);
    }

    private BigDecimal processProductItem(
            Producto product,
            int quantityVal,
            Map<UUID, BigDecimal> amountsByCategory) {
        BigDecimal price = product.getPrecio() != null ? product.getPrecio() : ZERO;
        BigDecimal quantity = BigDecimal.valueOf(quantityVal);
        BigDecimal itemTotal = price.multiply(quantity);

        addAmountToCategory(amountsByCategory, product.getCategoria(), itemTotal);

        return itemTotal;
    }

    private BigDecimal processPromotionItem(
            CheckBudgetDatesRequest.CartItemRequest item,
            Map<UUID, BigDecimal> amountsByCategory) {
        Promotion promotion = findPromotionOrThrow(item.productId());
        BigDecimal promotionTotal = ZERO;

        if (promotion.getProducts() == null) {
            return promotionTotal;
        }

        BigDecimal multiplier = calculateDiscountMultiplier(promotion);

        for (Producto p : promotion.getProducts()) {
            BigDecimal itemTotal = calculateDiscountedItemTotal(p, multiplier, item.quantity());
            promotionTotal = promotionTotal.add(itemTotal);
            addAmountToCategory(amountsByCategory, p.getCategoria(), itemTotal);
        }

        return promotionTotal;
    }

    private Promotion findPromotionOrThrow(UUID promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product or Promotion not found: " + promotionId));
    }

    private BigDecimal calculateDiscountMultiplier(Promotion promotion) {
        BigDecimal discountPercentage = promotion.getDiscountPercentage() != null
                ? promotion.getDiscountPercentage()
                : ZERO;
        return ONE.subtract(
                discountPercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal calculateDiscountedItemTotal(
            Producto p, BigDecimal multiplier, int quantityVal) {
        BigDecimal originalPrice = p.getPrecio() != null ? p.getPrecio() : ZERO;
        BigDecimal discountedPrice = originalPrice
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal quantity = BigDecimal.valueOf(quantityVal);
        return discountedPrice.multiply(quantity);
    }

    private void addAmountToCategory(Map<UUID, BigDecimal> amountsByCategory, 
                                     CategoriaProducto categoria, 
                                     BigDecimal itemTotal) {
        if (categoria != null && categoria.getId() != null) {
            UUID categoryId = categoria.getId();
            amountsByCategory.put(categoryId, amountsByCategory
                    .getOrDefault(categoryId, ZERO).add(itemTotal));
        }
    }

    private List<CheckBudgetDatesResponse.DateBudgetStatus> validateDates(
            UUID studentId, CartTotals totals, List<LocalDate> dates) {
        if (dates == null) {
            return new ArrayList<>();
        }
        
        List<CheckBudgetDatesResponse.DateBudgetStatus> results = new ArrayList<>();
        for (LocalDate date : dates) {
            results.add(validateSingleDate(studentId, totals, date));
        }
        return results;
    }

    private CheckBudgetDatesResponse.DateBudgetStatus validateSingleDate(
            UUID studentId, CartTotals totals, LocalDate date) {
        
        LocalDateTime momentoCompra = LocalDateTime.of(date, LocalTime.of(8, 0));

        Optional<String> blockReason = budgetValidator.obtenerMotivoBloqueoPresupuesto(
                studentId,
                totals.totalCartAmount(),
                totals.amountsByCategory(),
                momentoCompra
        );

        return blockReason
                .map(reason -> new CheckBudgetDatesResponse.DateBudgetStatus(date, true, reason))
                .orElseGet(() -> new CheckBudgetDatesResponse.DateBudgetStatus(date, false, null));
    }
}
