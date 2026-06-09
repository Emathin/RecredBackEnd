package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.persistence.SpringDataBuffetRepository;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class CreateAdvancePurchase {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final PurchaseValidator purchaseValidator;
    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final WithdrawalCodeService withdrawalCodeService;
    private final WalletRepository walletRepository;
    private final FranjaHorariaRepository timeSlotRepository;
    private final SpringDataBuffetRepository buffetRepository;
    private final UsuarioRepository usuarioRepository;

    public CreateAdvancePurchase(
            PurchaseValidator purchaseValidator,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            WithdrawalCodeService withdrawalCodeService,
            WalletRepository walletRepository,
            FranjaHorariaRepository timeSlotRepository,
            SpringDataBuffetRepository buffetRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.purchaseValidator = purchaseValidator;
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.withdrawalCodeService = withdrawalCodeService;
        this.walletRepository = walletRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.buffetRepository = buffetRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public AdvancePurchaseResponse execute(AdvancePurchaseRequest request, String cognitoSub) {
        if (request.orders() == null || request.orders().isEmpty()) {
            throw new IllegalArgumentException("Orders list cannot be empty");
        }

        Usuario usuario = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        UUID buyerId = usuario.getId();
        BuyerType buyerType;

        double totalSum = 0.0;
        Map<UUID, String> codes = new HashMap<>();
        String firstOrderId = null;
        PurchaseStatus responseStatus = null;

        for (AdvancePurchaseRequest.OrderRequest orderRequest : request.orders()) {
            if (usuario.getRol() == RolUsuario.ALUMNO) {
                if (usuario.getAlumno() == null || 
                    !usuario.getAlumno().getId().equals(orderRequest.studentId())) {
                    throw new SecurityException(
                            "No autorizado para comprar a nombre de otro alumno");
                }
                buyerType = BuyerType.STUDENT;
            } else if (usuario.getRol() == RolUsuario.PADRE) {
                if (usuario.getTutor() == null || 
                    usuario.getTutor().getHijos().stream()
                            .noneMatch(h -> h.getId().equals(orderRequest.studentId()))) {
                    throw new SecurityException(
                            "No autorizado para comprar a nombre de este alumno");
                }
                buyerType = BuyerType.TUTOR;
            } else {
                throw new SecurityException("Rol no autorizado para realizar compras");
            }

            Alumno student = alumnoRepository.findById(orderRequest.studentId())
                    .orElseThrow(() -> new AlumnoNoEncontradoException(orderRequest.studentId()));

            UUID schoolId = schoolId(student);

            UUID buffetId = buffetRepository
                    .findFirstByColegioEntityIdAndActivoTrue(schoolId)
                    .map(BuffetEntity::getId)
                    .orElseThrow(() -> new RuntimeException(
                            "Buffet not found for school: " + schoolId));

            FranjaHoraria pickupSlot = pickupSlot(student, orderRequest.recessTime());
            LocalDate pickupDate = LocalDate.parse(orderRequest.date());

            Map<Producto, Integer> items = new HashMap<>();
            for (AdvancePurchaseRequest.ItemRequest itemRequest : orderRequest.items()) {
                Producto product = productRepository.findById(itemRequest.productId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                items.put(product, itemRequest.quantity());
            }

            Purchase purchase = Purchase.advance(
                    student,
                    items,
                    MedioPago.CREDITOS,
                    buyerId,
                    buyerType,
                    buffetId,
                    pickupSlot.getId(),
                    pickupDate
            );

            purchaseValidator.validate(purchase);

            BigDecimal total = calculateTotal(items);

            Billetera wallet = walletRepository
                    .findByStudentId(orderRequest.studentId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Student wallet not found: " + orderRequest.studentId()));

            if (!wallet.hasSufficientBalance(total)) {
                throw new InsufficientBalanceException(
                        "Insufficient balance for student: " + orderRequest.studentId());
            }

            wallet.debit(total);
            walletRepository.save(wallet);

            purchase.setWithdrawalCode(withdrawalCodeService.generateCode(purchase));
            purchase = purchaseRepository.save(purchase);

            totalSum += purchase.getTotalAmount().doubleValue();
            codes.put(student.getId(), purchase.getWithdrawalCode());

            if (firstOrderId == null) {
                firstOrderId = purchase.getId().toString();
                responseStatus = PurchaseStatus.from(purchase);
            }
        }

        return new AdvancePurchaseResponse(
                firstOrderId,
                responseStatus,
                totalSum,
                codes
        );
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

    private String normalize(String value) {
        String lowerCaseValue = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        String normalizedValue = Normalizer.normalize(lowerCaseValue, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalizedValue).replaceAll("");
    }

    private boolean containsAll(String message, String... values) {
        return List.of(values).stream().allMatch(message::contains);
    }

    private boolean containsAny(String message, String... values) {
        return List.of(values).stream().anyMatch(message::contains);
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
