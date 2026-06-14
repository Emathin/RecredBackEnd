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
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import org.springframework.transaction.annotation.Transactional;

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
    private final InventoryAvailabilityService inventoryAvailabilityService;

    public ProcessPresentialPurchase(AlumnoRepository studentRepository,
                                     WalletRepository walletRepository,
                                     PurchaseRepository purchaseRepository,
                                     ValidadorHorarioService timeValidator,
                                     ValidacionRestriccionesService healthValidator,
                                     ValidadorPresupuestoService budgetValidator,
                                     InventoryAvailabilityService inventoryAvailabilityService) {

        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
        this.budgetValidator = budgetValidator;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
    }

    @Transactional
    public Purchase execute(UUID studentId, Map<Producto, Integer> cart, UUID buffetId) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        Alumno student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));

        LocalDateTime momentoCompra = LocalDateTime.now();

        timeValidator.validarCompra(student, cart.keySet(), momentoCompra);
        healthValidator.validarCompra(student, cart.keySet());

        BigDecimal montoTotalCarrito = calculateTotal(cart);

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

        budgetValidator.validarPresupuesto(
                studentId,
                montoTotalCarrito,
                montosPorCategoria,
                momentoCompra
        );

        Billetera wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found: " + studentId));

        if (!wallet.hasSufficientBalance(montoTotalCarrito)) {
            throw new InsufficientBalanceException
                    ("Insufficient balance for student: " + studentId);
        }

        inventoryAvailabilityService.consumeDirectly(buffetId, cart);

        wallet.debit(montoTotalCarrito);

        Purchase purchase = new Purchase(student, cart, MedioPago.CREDITOS, student.getId(),
                BuyerType.STUDENT, buffetId);

        walletRepository.save(wallet);
        return purchaseRepository.save(purchase);
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
