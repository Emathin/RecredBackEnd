package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import com.recred.backend.domain.services.WithdrawalCodeService;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CreateAssistantAdvancePurchase {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final AlumnoRepository studentRepository;
    private final WalletRepository walletRepository;
    private final PurchaseRepository purchaseRepository;
    private final FranjaHorariaRepository timeSlotRepository;
    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService healthValidator;
    private final ValidadorPresupuestoService budgetValidator;
    private final WithdrawalCodeService withdrawalCodeService;
    private final Clock clock;

    public CreateAssistantAdvancePurchase(
            AlumnoRepository studentRepository,
            WalletRepository walletRepository,
            PurchaseRepository purchaseRepository,
            FranjaHorariaRepository timeSlotRepository,
            ValidadorHorarioService timeValidator,
            ValidacionRestriccionesService healthValidator,
            ValidadorPresupuestoService budgetValidator,
            WithdrawalCodeService withdrawalCodeService,
            Clock clock
    ) {
        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.purchaseRepository = purchaseRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.timeValidator = timeValidator;
        this.healthValidator = healthValidator;
        this.budgetValidator = budgetValidator;
        this.withdrawalCodeService = withdrawalCodeService;
        this.clock = clock;
    }

    public Purchase execute(UUID studentId, UUID buffetId, Map<Producto, Integer> cart,
                            RecessTime recessTime, LocalDate pickupDate) {
        Alumno student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(studentId));
        FranjaHoraria pickupSlot = pickupSlot(student, recessTime);
        LocalDateTime pickupDateTime = LocalDateTime.of(pickupDate, pickupSlot.getHoraInicio());

        timeValidator.validarCompra(student, cart.keySet(), pickupDateTime);
        healthValidator.validarCompra(student, cart.keySet());

        BigDecimal total = calculateTotal(cart);
        budgetValidator.validarPresupuesto(studentId, total, totalsByCategory(cart),
                pickupDateTime);

        Billetera wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found: " + studentId));

        if (!wallet.hasSufficientBalance(total)) {
            throw new InsufficientBalanceException("Insufficient balance for student: "
                    + studentId);
        }

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

        wallet.debit(total);
        walletRepository.save(wallet);
        return purchaseRepository.save(purchase);
    }

    private FranjaHoraria pickupSlot(Alumno student, RecessTime recessTime) {
        UUID schoolId = schoolId(student);
        List<FranjaHoraria> slots = timeSlotRepository.findAllByColegioId(schoolId)
                .stream()
                .sorted(Comparator.comparing(FranjaHoraria::getHoraInicio))
                .toList();

        return slots.stream()
                .filter(slot -> matches(slot, recessTime))
                .findFirst()
                .orElseGet(() -> slotByPosition(slots, schoolId, recessTime));
    }

    private UUID schoolId(Alumno student) {
        if (student.getGrado() == null
                || student.getGrado().getColegio() == null
                || student.getGrado().getColegio().getId() == null) {
            throw new PickupTimeSlotNotFoundException(null, null);
        }

        return student.getGrado().getColegio().getId();
    }

    private FranjaHoraria slotByPosition(
            List<FranjaHoraria> slots,
            UUID schoolId,
            RecessTime recessTime
    ) {
        int index = switch (recessTime) {
            case FIRST_RECESS -> 0;
            case SECOND_RECESS -> 1;
            case NOON -> 2;
            case AFTER_HOURS -> 3;
        };

        if (slots.size() > index) {
            return slots.get(index);
        }

        throw new PickupTimeSlotNotFoundException(schoolId, recessTime);
    }

    private boolean matches(FranjaHoraria slot, RecessTime recessTime) {
        String description = normalize(slot.getDescripcion());
        return switch (recessTime) {
            case FIRST_RECESS -> containsAll(description, "primer", "recreo");
            case SECOND_RECESS -> containsAll(description, "segundo", "recreo");
            case NOON -> containsAny(description, "mediodia", "medio dia", "almuerzo");
            case AFTER_HOURS -> containsAny(description, "salida", "despues", "final");
        };
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

    private boolean containsAll(String message, String... values) {
        return List.of(values).stream().allMatch(message::contains);
    }

    private boolean containsAny(String message, String... values) {
        return List.of(values).stream().anyMatch(message::contains);
    }

    private String normalize(String value) {
        String lowerCaseValue = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        String normalizedValue = Normalizer.normalize(lowerCaseValue, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalizedValue).replaceAll("");
    }
}
