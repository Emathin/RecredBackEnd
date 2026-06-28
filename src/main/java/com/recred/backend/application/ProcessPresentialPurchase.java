package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.PurchaseValidator;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProcessPresentialPurchase {

    private final AlumnoRepository studentRepository;
    private final WalletRepository walletRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseValidator purchaseValidator;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;
    private final ProductRepository productRepository;

    public ProcessPresentialPurchase(
            AlumnoRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            PurchaseValidator purchaseValidator,
            InventoryAvailabilityService inventoryAvailabilityService,
            MovimientoBilleteraRepository movimientoBilleteraRepository,
            ProductRepository productRepository
    ) {
        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseValidator = purchaseValidator;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ProcessPresentialPurchaseResponse execute(ProcessPresentialPurchaseRequest request) {
        if (request.buffetId() == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        Alumno student = findStudent(request.studentId());
        Map<Producto, Integer> resolvedCart = resolveCart(request.cart());

        Purchase purchase = new Purchase(
                student,
                resolvedCart,
                MedioPago.CREDITOS,
                student.getId(),
                BuyerType.STUDENT,
                request.buffetId()
        );

        purchaseValidator.validate(purchase);
        BigDecimal totalAmount = purchase.getTotalAmount();
        
        Billetera wallet = getStudentWallet(request.studentId());
        validateWalletBalance(wallet, totalAmount, request.studentId());

        inventoryAvailabilityService.consumeDirectly(request.buffetId(), resolvedCart);
        debitWalletAndSave(wallet, totalAmount);
        
        Purchase savedPurchase = purchaseRepository.save(purchase);
        registerMovement(wallet, student, totalAmount, "Compra presencial en buffet");
        
        return new ProcessPresentialPurchaseResponse(
                savedPurchase.getId(),
                savedPurchase.getTotalAmount(),
                savedPurchase.getStatus().name()
        );
    }

    private Alumno findStudent(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));
    }

    private Map<Producto, Integer> resolveCart(
            java.util.List<ProcessPresentialPurchaseRequest.ItemRequest> cart) {
        return cart.stream()
                .collect(Collectors.toMap(
                        item -> productRepository.findById(item.productId())
                        .orElseThrow(() -> new RuntimeException(
                                "Product not found: " + item.productId())),
                        ProcessPresentialPurchaseRequest.ItemRequest::quantity
                ));
    }

    private Billetera getStudentWallet(UUID studentId) {
        return walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Student wallet not found: " + studentId));
    }

    private void validateWalletBalance(Billetera wallet, BigDecimal totalAmount, UUID studentId) {
        if (!wallet.hasSufficientBalance(totalAmount)) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for student: " + studentId);
        }
    }

    private void debitWalletAndSave(Billetera wallet, BigDecimal totalAmount) {
        wallet.debit(totalAmount);
        walletRepository.save(wallet);
    }

    private void registerMovement(
            Billetera wallet, Alumno student, BigDecimal amount, String description) {
        if (movimientoBilleteraRepository == null) {
            return;
        }
        if (student.getUsuarioId() == null) {
            movimientoBilleteraRepository.registrarMovimientoSistema(
                    wallet.getId(),
                    TipoMovimientoBilletera.COMPRA,
                    amount.negate(),
                    description
                );
            return;
        }
        movimientoBilleteraRepository.registrarMovimiento(
                wallet.getId(),
                TipoMovimientoBilletera.COMPRA,
                amount.negate(),
                description,
                student.getUsuarioId()
        );
    }
}
