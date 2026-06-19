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
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.PurchaseValidator;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class ProcessPresentialPurchase {

    private final AlumnoRepository studentRepository;
    private final WalletRepository walletRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseValidator purchaseValidator;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public ProcessPresentialPurchase(
            AlumnoRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            PurchaseValidator purchaseValidator,
            InventoryAvailabilityService inventoryAvailabilityService
    ) {
        this(studentRepository, walletRepository, purchaseRepository, purchaseValidator,
                inventoryAvailabilityService, null);
    }

    public ProcessPresentialPurchase(
            AlumnoRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            PurchaseValidator purchaseValidator,
            InventoryAvailabilityService inventoryAvailabilityService,
            MovimientoBilleteraRepository movimientoBilleteraRepository
    ) {
        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseValidator = purchaseValidator;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public Purchase execute(UUID studentId, Map<Producto, Integer> cart, UUID buffetId) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        Alumno student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));
        Purchase purchase = new Purchase(
                student,
                cart,
                MedioPago.CREDITOS,
                student.getId(),
                BuyerType.STUDENT,
                buffetId
        );

        purchaseValidator.validate(purchase);
        BigDecimal totalAmount = purchase.getTotalAmount();
        Billetera wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found: " + studentId));

        if (!wallet.hasSufficientBalance(totalAmount)) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for student: " + studentId);
        }

        inventoryAvailabilityService.consumeDirectly(buffetId, cart);
        wallet.debit(totalAmount);
        walletRepository.save(wallet);
        Purchase savedPurchase = purchaseRepository.save(purchase);
        registerMovement(wallet, student, totalAmount, "Compra presencial en buffet");
        return savedPurchase;
    }

    private void registerMovement(
            Billetera wallet,
            Alumno student,
            BigDecimal amount,
            String description) {
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
