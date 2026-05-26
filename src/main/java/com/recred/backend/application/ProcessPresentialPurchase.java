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
<<<<<<< feature/compra-espontanea
=======
import com.recred.backend.domain.services.ValidadorPresupuestoService;
>>>>>>> control-presupuesto

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
<<<<<<< feature/compra-espontanea
=======
import java.util.Set;
>>>>>>> control-presupuesto
import java.util.UUID;
import java.util.stream.Collectors;

public class ProcessPresentialPurchase {

    private final AlumnoRepository studentRepository;
    private final WalletRepository walletRepository;
    private final PurchaseRepository purchaseRepository;
    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService healthValidator;
<<<<<<< feature/compra-espontanea

    public ProcessPresentialPurchase(AlumnoRepository studentRepository,
                                     WalletRepository walletRepository,
                                     PurchaseRepository purchaseRepository,
                                     ValidadorHorarioService timeValidator,
                                     ValidacionRestriccionesService healthValidator) {

        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
=======
    private final ValidadorPresupuestoService budgetValidator;

    public ProcessPresentialPurchase(ValidadorHorarioService timeValidator,
                                     ValidacionRestriccionesService healthValidator,
                                     ValidadorPresupuestoService budgetValidator) {
>>>>>>> control-presupuesto
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
        this.budgetValidator = budgetValidator;
    }

<<<<<<< feature/compra-espontanea
    public void execute(UUID studentId, Map<Producto, Integer> cart) {

        Alumno student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));


        timeValidator.validarCompra(student, cart.keySet(), LocalDateTime.now());
        healthValidator.validarCompra(student, cart.keySet());


        BigDecimal totalAmount = calculateTotal(cart);


        Billetera wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalStateException
                        ("Student wallet not found: " + studentId));

        if (!wallet.hasSufficientBalance(totalAmount)) {
            throw new InsufficientBalanceException
                    ("Insufficient balance for student: " + studentId);
        }


        wallet.debit(totalAmount);


        Purchase purchase = new Purchase(student, cart, MedioPago.CREDITOS);


        walletRepository.save(wallet);
        purchaseRepository.save(purchase);
    }



    private BigDecimal calculateTotal(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry ->
                        entry.getKey().getPrecio().multiply(new BigDecimal(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
=======
    public void execute(UUID studentId, Set<Producto> cart) {
        Alumno student = new Alumno();
        student.setId(studentId);

        LocalDateTime momentoCompra = LocalDateTime.now();

        timeValidator.validarCompra(student, cart, momentoCompra);
        healthValidator.validarCompra(student, cart);

        BigDecimal montoTotalCarrito = cart.stream()
                .map(p -> p.getPrecio() != null ?
                        p.getPrecio() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, BigDecimal> montosPorCategoria = cart.stream()
                .filter(p -> p.getCategoria() != null &&
                        p.getCategoria().getId() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCategoria().getId(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                p -> p.getPrecio() != null ?
                                        p.getPrecio() : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));

        budgetValidator.validarPresupuesto(
                studentId,
                montoTotalCarrito,
                montosPorCategoria,
                momentoCompra
        );

>>>>>>> control-presupuesto
    }
}
