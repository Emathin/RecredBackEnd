package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateAdvancePurchase {

    private final PurchaseValidator purchaseValidator;
    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final WithdrawalCodeService withdrawalCodeService;

    public CreateAdvancePurchase(
            PurchaseValidator purchaseValidator,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            WithdrawalCodeService withdrawalCodeService
    ) {
        this.purchaseValidator = purchaseValidator;
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.withdrawalCodeService = withdrawalCodeService;
    }

    public AdvancePurchaseResponse execute(AdvancePurchaseRequest request) {
        AdvancePurchaseRequest.OrderRequest orderRequest = request.orders().get(0);

        Alumno student = alumnoRepository.findById(orderRequest.studentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Map<Producto, Integer> items = new HashMap<>();
        for (AdvancePurchaseRequest.ItemRequest itemRequest : orderRequest.items()) {
            Producto product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            items.put(product, itemRequest.quantity());
        }

        Purchase purchase = new Purchase(
                student,
                items,
                MedioPago.CREDITOS,
                orderRequest.buyerId(),
                orderRequest.buyerType()
        );

        purchaseValidator.validate(purchase);

        purchase = purchaseRepository.save(purchase);

        String withdrawalCode = withdrawalCodeService.generateCode(purchase);
        Map<UUID, String> codes = new HashMap<>();
        codes.put(student.getId(), withdrawalCode);

        return new AdvancePurchaseResponse(
                purchase.getId().toString(),
                PurchaseStatus.from(purchase),
                purchase.getTotalAmount().doubleValue(),
                codes
        );
    }
}
