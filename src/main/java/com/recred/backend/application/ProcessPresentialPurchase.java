package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.PurchaseValidator;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class ProcessPresentialPurchase {

    private final AlumnoRepository studentRepository;
    private final WalletRepository walletRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseValidator purchaseValidator;

    public ProcessPresentialPurchase(AlumnoRepository studentRepository,
                                     WalletRepository walletRepository,
                                     PurchaseRepository purchaseRepository,
                                     PurchaseValidator purchaseValidator) {

        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseValidator = purchaseValidator;
    }

    public Purchase execute(UUID studentId, Map<Producto, Integer> cart) {

        Alumno student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));

        Purchase purchase = new Purchase(student, cart, MedioPago.CREDITOS, student.getId(),
                BuyerType.STUDENT);

        purchaseValidator.validate(purchase);

        BigDecimal montoTotalCarrito = purchase.getTotalAmount();

        Billetera wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found: " + studentId));

        if (!wallet.hasSufficientBalance(montoTotalCarrito)) {
            throw new InsufficientBalanceException
                    ("Insufficient balance for student: " + studentId);
        }

        wallet.debit(montoTotalCarrito);

        walletRepository.save(wallet);
        return purchaseRepository.save(purchase);
    }
}
