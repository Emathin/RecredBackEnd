package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.AdvancePurchaseRulesService;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.RecessTimeSlotResolver;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import com.recred.backend.domain.services.WithdrawalCodeService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateAssistantAdvancePurchase {

    private final AlumnoRepository studentRepository;
    private final WalletRepository walletRepository;
    private final PurchaseRepository purchaseRepository;
    private final RecessTimeSlotResolver recessTimeSlotResolver;
    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService healthValidator;
    private final ValidadorPresupuestoService budgetValidator;
    private final WithdrawalCodeService withdrawalCodeService;
    private final Clock clock;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final AdvancePurchaseRulesService advancePurchaseRulesService;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public CreateAssistantAdvancePurchase(
            AlumnoRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            RecessTimeSlotResolver recessTimeSlotResolver,
            ValidadorHorarioService timeValidator,
            ValidacionRestriccionesService healthValidator,
            ValidadorPresupuestoService budgetValidator,
            WithdrawalCodeService withdrawalCodeService,
            Clock clock,
            InventoryAvailabilityService inventoryAvailabilityService,
            AdvancePurchaseRulesService advancePurchaseRulesService
    ) {
        this(studentRepository, walletRepository, purchaseRepository, recessTimeSlotResolver,
                timeValidator, healthValidator, budgetValidator, withdrawalCodeService, clock,
                inventoryAvailabilityService, advancePurchaseRulesService, null);
    }

    public CreateAssistantAdvancePurchase(
            AlumnoRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            RecessTimeSlotResolver recessTimeSlotResolver,
            ValidadorHorarioService timeValidator,
            ValidacionRestriccionesService healthValidator,
            ValidadorPresupuestoService budgetValidator,
            WithdrawalCodeService withdrawalCodeService,
            Clock clock,
            InventoryAvailabilityService inventoryAvailabilityService,
            AdvancePurchaseRulesService advancePurchaseRulesService,
            MovimientoBilleteraRepository movimientoBilleteraRepository
    ) {
        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.recessTimeSlotResolver = recessTimeSlotResolver;
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
        this.budgetValidator = budgetValidator;
        this.withdrawalCodeService = withdrawalCodeService;
        this.clock = clock;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.advancePurchaseRulesService = advancePurchaseRulesService;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public Purchase execute(UUID studentId, UUID buffetId, Map<Producto, Integer> cart,
                            RecessTime recessTime, LocalDate pickupDate) {
        requireBuffetId(buffetId);
        Alumno student = student(studentId);
        FranjaHoraria pickupSlot = pickupSlot(student, recessTime);
        LocalDateTime pickupDateTime = pickupDateTime(pickupDate, pickupSlot);
        validatePurchase(student, buffetId, cart, pickupDate, pickupSlot, pickupDateTime);
        BigDecimal total = validatedTotal(studentId, cart, pickupDateTime);
        Billetera wallet = fundedWallet(studentId, total);

        inventoryAvailabilityService.reserve(buffetId, cart);
        Purchase savedPurchase = savePurchase(
                advancePurchase(student, cart, buffetId, pickupSlot, pickupDate),
                wallet,
                total
        );
        registerPurchaseMovement(wallet, student, total);
        return savedPurchase;
    }

    private void requireBuffetId(UUID buffetId) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }
    }

    private Alumno student(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));
    }

    private FranjaHoraria pickupSlot(Alumno student, RecessTime recessTime) {
        return recessTimeSlotResolver.resolve(student, recessTime);
    }

    private LocalDateTime pickupDateTime(LocalDate pickupDate, FranjaHoraria pickupSlot) {
        return LocalDateTime.of(pickupDate, pickupSlot.getHoraInicio());
    }

    private void validatePurchase(
            Alumno student,
            UUID buffetId,
            Map<Producto, Integer> cart,
            LocalDate pickupDate,
            FranjaHoraria pickupSlot,
            LocalDateTime pickupDateTime
    ) {
        advancePurchaseRulesService.validate(buffetId, pickupSlot, pickupDate, cart);
        timeValidator.validarCompra(student, cart.keySet(), pickupDateTime);
        healthValidator.validarCompra(student, cart.keySet());
    }

    private BigDecimal validatedTotal(
            UUID studentId,
            Map<Producto, Integer> cart,
            LocalDateTime pickupDateTime
    ) {
        BigDecimal total = calculateTotal(cart);
        budgetValidator.validarPresupuesto(studentId, total, totalsByCategory(cart),
                pickupDateTime);
        return total;
    }

    private Billetera fundedWallet(UUID studentId, BigDecimal total) {
        Billetera wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found: " + studentId));

        if (!wallet.hasSufficientBalance(total)) {
            throw new InsufficientBalanceException("Insufficient balance for student: "
                    + studentId);
        }

        return wallet;
    }

    private Purchase advancePurchase(
            Alumno student,
            Map<Producto, Integer> cart,
            UUID buffetId,
            FranjaHoraria pickupSlot,
            LocalDate pickupDate
    ) {
        Purchase purchase = Purchase.advance(
                student,
                cart,
                MedioPago.CREDITOS,
                student.getId(),
                BuyerType.STUDENT,
                buffetId,
                pickupSlot.getId(),
                pickupDate
        );
        purchase.setDate(LocalDateTime.now(clock));
        purchase.setWithdrawalCode(withdrawalCodeService.generateCode(purchase));
        return purchase;
    }

    private Purchase savePurchase(Purchase purchase, Billetera wallet, BigDecimal total) {
        wallet.debit(total);
        walletRepository.save(wallet);
        return purchaseRepository.save(purchase);
    }

    private BigDecimal calculateTotal(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> price(entry.getKey()).multiply(BigDecimal.valueOf(
                        entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<UUID, BigDecimal> totalsByCategory(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .filter(entry -> entry.getKey().getCategoria() != null
                        && entry.getKey().getCategoria().getId() != null)
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getCategoria().getId(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                entry -> price(entry.getKey()).multiply(BigDecimal.valueOf(
                                        entry.getValue())),
                                BigDecimal::add
                        )
                ));
    }

    private BigDecimal price(Producto product) {
        return product.getPrecio() == null ? BigDecimal.ZERO : product.getPrecio();
    }

    private void registerPurchaseMovement(Billetera wallet, Alumno student, BigDecimal amount) {
        if (movimientoBilleteraRepository == null) {
            return;
        }
        if (student.getUsuarioId() == null) {
            movimientoBilleteraRepository.registrarMovimientoSistema(
                    wallet.getId(),
                    TipoMovimientoBilletera.COMPRA,
                    amount.negate(),
                    "Compra anticipada asistida"
            );
            return;
        }
        movimientoBilleteraRepository.registrarMovimiento(
                wallet.getId(),
                TipoMovimientoBilletera.COMPRA,
                amount.negate(),
                "Compra anticipada asistida",
                student.getUsuarioId()
        );
    }
}
