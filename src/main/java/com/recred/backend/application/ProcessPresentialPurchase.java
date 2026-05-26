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
import com.recred.backend.domain.services.ValidadorPresupuestoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProcessPresentialPurchase {

    private final AlumnoRepository studentRepository;
    private final WalletRepository walletRepository;
    private final PurchaseRepository purchaseRepository;
    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService healthValidator;
    private final ValidadorPresupuestoService budgetValidator;

    public ProcessPresentialPurchase(AlumnoRepository studentRepository,
                                     WalletRepository walletRepository,
                                     PurchaseRepository purchaseRepository,
                                     ValidadorHorarioService timeValidator,
                                     ValidacionRestriccionesService healthValidator,
                                     ValidadorPresupuestoService budgetValidator) {

        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
        this.budgetValidator = budgetValidator;
    }

    public void execute(UUID studentId, Map<Producto, Integer> cart) {

        // 1. Obtener el alumno real de la base de datos
        Alumno student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));

        LocalDateTime momentoCompra = LocalDateTime.now();

        // 2. Validaciones de horario y salud
        timeValidator.validarCompra(student, cart.keySet(), momentoCompra);
        healthValidator.validarCompra(student, cart.keySet());

        // 3. Calcular montos considerando las cantidades del carrito
        BigDecimal montoTotalCarrito = calculateTotal(cart);

        // Agrupar por categoría multiplicando el precio por la cantidad
        Map<UUID, BigDecimal> montosPorCategoria = cart.entrySet().stream()
                .filter(entry -> entry.getKey().getCategoria() != null &&
                        entry.getKey().getCategoria().getId() != null)
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getCategoria().getId(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                entry -> {
                                    BigDecimal precio = entry.getKey().getPrecio() != null ?
                                            entry.getKey().getPrecio() : BigDecimal.ZERO;
                                    return precio.multiply(new BigDecimal(entry.getValue()));
                                },
                                BigDecimal::add
                        )
                ));

        // 4. Validar presupuesto
        budgetValidator.validarPresupuesto(
                studentId,
                montoTotalCarrito,
                montosPorCategoria,
                momentoCompra
        );

        // 5. Validar saldo y debitar de la billetera
        Billetera wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found: " + studentId));

        if (!wallet.hasSufficientBalance(montoTotalCarrito)) {
            throw new InsufficientBalanceException
                    ("Insufficient balance for student: " + studentId);
        }

        wallet.debit(montoTotalCarrito);

        // 6. Guardar la compra
        Purchase purchase = new Purchase(student, cart, MedioPago.CREDITOS);

        walletRepository.save(wallet);
        purchaseRepository.save(purchase);
    }

    private BigDecimal calculateTotal(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> {
                    BigDecimal precio = entry.getKey().getPrecio() != null ?
                            entry.getKey().getPrecio() : BigDecimal.ZERO;
                    return precio.multiply(new BigDecimal(entry.getValue()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}