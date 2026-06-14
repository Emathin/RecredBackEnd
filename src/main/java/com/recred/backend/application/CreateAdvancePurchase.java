package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.AdvancePurchaseRulesService;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.RecessTimeSlotResolver;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateAdvancePurchase {

    private final PurchaseValidator purchaseValidator;
    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final WithdrawalCodeService withdrawalCodeService;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final RecessTimeSlotResolver recessTimeSlotResolver;
    private final AdvancePurchaseRulesService advancePurchaseRulesService;

    public CreateAdvancePurchase(
            PurchaseValidator purchaseValidator,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            WithdrawalCodeService withdrawalCodeService,
            InventoryAvailabilityService inventoryAvailabilityService,
            RecessTimeSlotResolver recessTimeSlotResolver,
            AdvancePurchaseRulesService advancePurchaseRulesService
    ) {
        this.purchaseValidator = purchaseValidator;
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.withdrawalCodeService = withdrawalCodeService;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.recessTimeSlotResolver = recessTimeSlotResolver;
        this.advancePurchaseRulesService = advancePurchaseRulesService;
    }

    @Transactional
    public AdvancePurchaseResponse execute(AdvancePurchaseRequest request) {
        validateRequest(request);

        Map<UUID, String> codes = new HashMap<>();
        Purchase firstPurchase = null;
        double total = 0;

        for (AdvancePurchaseRequest.OrderRequest orderRequest : request.orders()) {
            Purchase purchase = createPurchase(orderRequest);
            if (firstPurchase == null) {
                firstPurchase = purchase;
            }
            total += purchase.getTotalAmount().doubleValue();
            codes.put(purchase.getStudent().getId(), purchase.getWithdrawalCode());
        }

        return new AdvancePurchaseResponse(
                firstPurchase.getId().toString(),
                PurchaseStatus.from(firstPurchase),
                total,
                codes
        );
    }

    private Purchase createPurchase(AdvancePurchaseRequest.OrderRequest orderRequest) {
        if (orderRequest.buffetId() == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        Alumno student = alumnoRepository.findById(orderRequest.studentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        LocalDate pickupDate = LocalDate.parse(orderRequest.date());
        FranjaHoraria pickupSlot = recessTimeSlotResolver.resolve(
                student,
                orderRequest.recessTime()
        );

        Map<Producto, Integer> items = new HashMap<>();
        for (AdvancePurchaseRequest.ItemRequest itemRequest : orderRequest.items()) {
            Producto product = productRepository.findActiveByIdWithStock(
                            itemRequest.productId(),
                            orderRequest.buffetId()
                    )
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            items.put(product, itemRequest.quantity());
        }

        advancePurchaseRulesService.validate(
                orderRequest.buffetId(),
                pickupSlot,
                pickupDate,
                items);

        Purchase purchase = Purchase.advance(
                student,
                items,
                MedioPago.CREDITOS,
                orderRequest.buyerId(),
                orderRequest.buyerType(),
                orderRequest.buffetId(),
                pickupSlot.getId(),
                pickupDate
        );
        String withdrawalCode = withdrawalCodeService.generateCode(purchase);
        purchase.setWithdrawalCode(withdrawalCode);

        purchaseValidator.validate(purchase);
        inventoryAvailabilityService.reserve(orderRequest.buffetId(), items);

        purchase = purchaseRepository.save(purchase);
        if (purchase.getWithdrawalCode() == null) {
            purchase.setWithdrawalCode(withdrawalCode);
        }

        return purchase;
    }

    private void validateRequest(AdvancePurchaseRequest request) {
        if (request == null || request.orders() == null || request.orders().isEmpty()) {
            throw new IllegalArgumentException("orders is required");
        }
    }
}
