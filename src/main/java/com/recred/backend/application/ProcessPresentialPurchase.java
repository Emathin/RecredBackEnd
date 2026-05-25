package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class ProcessPresentialPurchase {

    private final AlumnoRepository studentRepository;
    private final WalletRepository walletRepository;
    private final PurchaseRepository purchaseRepository;
    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService healthValidator;

    public ProcessPresentialPurchase(AlumnoRepository studentRepository,
                                     WalletRepository walletRepository,
                                     PurchaseRepository purchaseRepository,
                                     ValidadorHorarioService timeValidator,
                                     ValidacionRestriccionesService healthValidator) {

        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
    }

    public void execute(UUID studentId, Map<Producto, Integer> cart) {
        // 1. Fetch Student
        Alumno student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));

        // 2. Validate Restrictions
        timeValidator.validarCompra(student, cart.keySet(), LocalDateTime.now());
        healthValidator.validarCompra(student, cart.keySet());

        // 3. Calculate Total
        BigDecimal totalAmount = calculateTotal(cart);

        // 4. Fetch Wallet and Check Balance
        Billetera wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalStateException("Student wallet not found: " + studentId));

        if (!wallet.hasSufficientBalance(totalAmount)) {
            throw new InsufficientBalanceException("Insufficient balance for student: " + studentId);
        }

        // 5. Debit Wallet
        wallet.debit(totalAmount);

        // 6. Create and Save Purchase
        Purchase purchase = new Purchase(student, cart, MedioPago.CREDITOS);

        // 7. Persist changes
        walletRepository.save(wallet);
        purchaseRepository.save(purchase);
    }



    private BigDecimal calculateTotal(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> entry.getKey().getPrecio().multiply(new BigDecimal(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
