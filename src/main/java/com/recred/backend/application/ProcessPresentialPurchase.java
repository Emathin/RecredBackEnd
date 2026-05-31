package com.recred.backend.application;

import com.recred.backend.domain.exceptions.StudentNotFoundException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.Wallet;
import com.recred.backend.domain.models.PaymentMethod;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.RestrictionsValidatorService;
import com.recred.backend.domain.services.TimeValidatorService;
import com.recred.backend.domain.services.BudgetValidatorService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProcessPresentialPurchase {

    private final StudentRepository studentRepository;
    private final WalletRepository walletRepository;
    private final PurchaseRepository purchaseRepository;
    private final TimeValidatorService timeValidator;
    private final RestrictionsValidatorService healthValidator;
    private final BudgetValidatorService budgetValidator;

    public ProcessPresentialPurchase(StudentRepository studentRepository,
                                     WalletRepository walletRepository,
                                     PurchaseRepository purchaseRepository,
                                     TimeValidatorService timeValidator,
                                     RestrictionsValidatorService healthValidator,
                                     BudgetValidatorService budgetValidator) {

        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
        this.budgetValidator = budgetValidator;
    }

    public Purchase execute(UUID studentId, Map<Product, Integer> cart) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        LocalDateTime momentoCompra = LocalDateTime.now();

        timeValidator.validatePurchase(student, cart.keySet(), momentoCompra);
        healthValidator.validatePurchase(student, cart.keySet());

        BigDecimal montoTotalCarrito = calculateTotal(cart);

        Map<UUID, BigDecimal> montosPorCategoria = cart.entrySet().stream()
                .filter(entry -> entry.getKey().getCategory() != null &&
                        entry.getKey().getCategory().getId() != null)
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getCategory().getId(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                entry -> {
                                    BigDecimal precio = entry.getKey().getPrice() != null ?
                                            entry.getKey().getPrice() : BigDecimal.ZERO;
                                    return precio.multiply(new BigDecimal(entry.getValue()));
                                },
                                BigDecimal::add
                        )
                ));

        budgetValidator.validateBudget(
                studentId,
                montoTotalCarrito,
                montosPorCategoria,
                momentoCompra
        );

        Wallet wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found: " + studentId));

        if (!wallet.hasSufficientBalance(montoTotalCarrito)) {
            throw new InsufficientBalanceException
                    ("Insufficient balance for student: " + studentId);
        }

        wallet.debit(montoTotalCarrito);

        Purchase purchase = new Purchase(student, cart, PaymentMethod.CREDITOS);

        walletRepository.save(wallet);
        return purchaseRepository.save(purchase);
    }

    private BigDecimal calculateTotal(Map<Product, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> {
                    BigDecimal precio = entry.getKey().getPrice() != null ?
                            entry.getKey().getPrice() : BigDecimal.ZERO;
                    return precio.multiply(new BigDecimal(entry.getValue()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
