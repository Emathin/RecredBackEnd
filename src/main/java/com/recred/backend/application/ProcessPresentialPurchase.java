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
    private final com.recred.backend.domain.repositories.UsuarioRepository usuarioRepository;
    private final com.recred.backend.domain.repositories.KiosqueroRepository kiosqueroRepository;
    private final ExternalPaymentService externalPaymentService;

    public ProcessPresentialPurchase(AlumnoRepository studentRepository,
                                     WalletRepository walletRepository,
                                     PurchaseRepository purchaseRepository,
                                     PurchaseValidator purchaseValidator,
                                     com.recred.backend.domain.repositories.UsuarioRepository
                                             usuarioRepository,
                                     com.recred.backend.domain.repositories.KiosqueroRepository
                                             kiosqueroRepository,
                                     ExternalPaymentService externalPaymentService) {

        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseValidator = purchaseValidator;
        this.usuarioRepository = usuarioRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.externalPaymentService = externalPaymentService;
    }

    public Purchase execute(UUID studentId, Map<Producto, Integer> cart, String cognitoSub, MedioPago paymentMethod) {

        com.recred.backend.domain.models.Usuario usuario = usuarioRepository
                .findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        if (usuario.getRol() != com.recred.backend.domain.models.RolUsuario.VENDEDOR) {
            throw new SecurityException("Sólo un kiosquero puede realizar una venta espontánea");
        }

        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(usuario.getId());
        if (buffetId == null) {
            throw new IllegalStateException("El kiosquero no tiene un buffet asociado");
        }

        Alumno student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));

        Purchase purchase = new Purchase(student, cart, paymentMethod, student.getId(),
                BuyerType.STUDENT, buffetId);

        purchaseValidator.validate(purchase);

        BigDecimal montoTotalCarrito = purchase.getTotalAmount();

        if (paymentMethod == MedioPago.CREDITOS) {
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
        } else if (paymentMethod == MedioPago.MERCADO_PAGO) {
            purchase.setStatus(EstadoCompra.PENDIENTE);
            purchase = purchaseRepository.save(purchase);
            String paymentUrl = externalPaymentService.createPurchasePreference(purchase);
            purchase.setPaymentUrl(paymentUrl);
            return purchase;
        }

        return purchaseRepository.save(purchase);
    }
}
