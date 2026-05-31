package com.recred.backend.application;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateAdvancePurchaseUseCase {

    private final PurchaseValidator purchaseValidator;
    private final StudentRepository studentRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final WithdrawalCodeService withdrawalCodeService;

    public CreateAdvancePurchaseUseCase(
            PurchaseValidator purchaseValidator,
            StudentRepository studentRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            WithdrawalCodeService withdrawalCodeService
    ) {
        this.purchaseValidator = purchaseValidator;
        this.studentRepository = studentRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.withdrawalCodeService = withdrawalCodeService;
    }

    public AdvancePurchaseResponse execute(AdvancePurchaseRequest request) {
        AdvancePurchaseRequest.OrderRequest orderRequest = request.orders().get(0);

        Student student = studentRepository.findById(orderRequest.studentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Map<Product, Integer> items = new HashMap<>();
        for (AdvancePurchaseRequest.ItemRequest itemRequest : orderRequest.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            items.put(product, itemRequest.quantity());
        }

        Purchase purchase = new Purchase(student, items, null);

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
