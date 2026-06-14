package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import com.recred.backend.infrastructure.web.CheckBudgetDatesRequest;
import com.recred.backend.infrastructure.web.CheckBudgetDatesResponse;

import java.math.BigDecimal;
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
    private final ValidadorPresupuestoService budgetValidator;

    public CheckBudgetDates(ProductRepository productRepository,
                            ValidadorPresupuestoService budgetValidator) {
        this.productRepository = productRepository;
        this.budgetValidator = budgetValidator;
    }

    public CheckBudgetDatesResponse execute(UUID studentId, CheckBudgetDatesRequest request) {
        BigDecimal totalCartAmount = BigDecimal.ZERO;
        Map<UUID, BigDecimal> amountsByCategory = new HashMap<>();

        if (request.items() != null) {
            for (CheckBudgetDatesRequest.CartItemRequest item : request.items()) {
                Optional<Producto> productOpt = productRepository.findById(item.productId());
                if (productOpt.isEmpty()) {
                    throw new IllegalArgumentException("Product not found: " + item.productId());
                }
                Producto product = productOpt.get();
                BigDecimal price = product.getPrecio() != null
                        ? product.getPrecio() : BigDecimal.ZERO;
                BigDecimal quantity = BigDecimal.valueOf(item.quantity());
                BigDecimal itemTotal = price.multiply(quantity);

                totalCartAmount = totalCartAmount.add(itemTotal);

                if (product.getCategoria() != null && product.getCategoria().getId() != null) {
                    UUID categoryId = product.getCategoria().getId();
                    amountsByCategory.put(categoryId, amountsByCategory
                            .getOrDefault(categoryId, BigDecimal.ZERO).add(itemTotal));
                }
            }
        }

        List<CheckBudgetDatesResponse.DateBudgetStatus> results = new ArrayList<>();
        if (request.dates() != null) {
            for (String dateStr : request.dates()) {
                LocalDate date;
                try {
                    date = LocalDate.parse(dateStr);
                } catch (Exception e) {
                    results.add(new CheckBudgetDatesResponse.DateBudgetStatus(
                            dateStr, true, "Formato de fecha inválido."));
                    continue;
                }

                LocalDateTime momentoCompra = LocalDateTime.of(date, LocalTime.of(8, 0));

                Optional<String> blockReason = budgetValidator.obtenerMotivoBloqueoPresupuesto(
                        studentId,
                        totalCartAmount,
                        amountsByCategory,
                        momentoCompra
                );

                if (blockReason.isPresent()) {
                    results.add(new CheckBudgetDatesResponse.DateBudgetStatus(
                            dateStr, true, blockReason.get()));
                } else {
                    results.add(new CheckBudgetDatesResponse.DateBudgetStatus(
                            dateStr, false, null));
                }
            }
        }

        return new CheckBudgetDatesResponse(results);
    }
}
