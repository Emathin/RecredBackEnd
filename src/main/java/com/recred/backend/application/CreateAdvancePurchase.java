package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.AdvancePurchaseRulesService;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.RecessTimeSlotResolver;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.domain.repositories.PromotionRepository;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.BuffetRepository;

import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreateAdvancePurchase {

    private final PurchaseValidator purchaseValidator;
    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final WithdrawalCodeService withdrawalCodeService;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final RecessTimeSlotResolver recessTimeSlotResolver;
    private final AdvancePurchaseRulesService advancePurchaseRulesService;
    private final WalletRepository walletRepository;
    private final BuffetRepository buffetRepository;
    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final PromotionRepository promotionRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public CreateAdvancePurchase(
            PurchaseValidator purchaseValidator,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            WithdrawalCodeService withdrawalCodeService,
            InventoryAvailabilityService inventoryAvailabilityService,
            RecessTimeSlotResolver recessTimeSlotResolver,
            AdvancePurchaseRulesService advancePurchaseRulesService,
            WalletRepository walletRepository,
            BuffetRepository buffetRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            PromotionRepository promotionRepository
    ) {
        this(purchaseValidator, alumnoRepository, productRepository, purchaseRepository,
                withdrawalCodeService, inventoryAvailabilityService, recessTimeSlotResolver,
                advancePurchaseRulesService, walletRepository, buffetRepository,
                usuarioRepository, tutorRepository, promotionRepository, null);
    }

    public CreateAdvancePurchase(
            PurchaseValidator purchaseValidator,
            AlumnoRepository alumnoRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            WithdrawalCodeService withdrawalCodeService,
            InventoryAvailabilityService inventoryAvailabilityService,
            RecessTimeSlotResolver recessTimeSlotResolver,
            AdvancePurchaseRulesService advancePurchaseRulesService,
            WalletRepository walletRepository,
            BuffetRepository buffetRepository,
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            PromotionRepository promotionRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository
    ) {
        this.purchaseValidator = purchaseValidator;
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.withdrawalCodeService = withdrawalCodeService;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.recessTimeSlotResolver = recessTimeSlotResolver;
        this.advancePurchaseRulesService = advancePurchaseRulesService;
        this.walletRepository = walletRepository;
        this.buffetRepository = buffetRepository;
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.promotionRepository = promotionRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public CreateAdvancePurchaseResponse execute(CreateAdvancePurchaseRequest request) {
        validateRequest(request);
        Usuario user = authenticatedUser(request.cognitoSub());
        CreatedPurchases purchases = createPurchases(request.orders(), user);
        return response(purchases);
    }

    private CreatedPurchases createPurchases(
            List<CreateAdvancePurchaseRequest.OrderRequest> orderRequests,
            Usuario user
    ) {
        Map<UUID, String> codes = new HashMap<>();
        Purchase firstPurchase = null;
        double total = 0;

        for (CreateAdvancePurchaseRequest.OrderRequest orderRequest : orderRequests) {
            Purchase purchase = createPurchase(orderRequest, user);
            if (firstPurchase == null) {
                firstPurchase = purchase;
            }

            total += purchase.getTotalAmount().doubleValue();
            codes.put(purchase.getStudent().getId(), purchase.getWithdrawalCode());
        }

        return new CreatedPurchases(firstPurchase, total, codes);
    }

    private CreateAdvancePurchaseResponse response(CreatedPurchases purchases) {
        Purchase firstPurchase = purchases.firstPurchase();
        return new CreateAdvancePurchaseResponse(
                firstPurchase.getId().toString(),
                PurchaseStatus.from(firstPurchase),
                purchases.total(),
                purchases.codes()
        );
    }

    private Purchase createPurchase(
            CreateAdvancePurchaseRequest.OrderRequest orderRequest,
            Usuario user
    ) {
        Alumno student = alumnoRepository.findById(orderRequest.studentId())
                .orElseThrow(() -> new AlumnoNoEncontradoException(orderRequest.studentId()));
        Buyer buyer = authorizedBuyer(user, student.getId());
        UUID buffetId = buffetId(orderRequest, student);
        LocalDate pickupDate = pickupDate(orderRequest.date());
        FranjaHoraria pickupSlot = recessTimeSlotResolver.resolve(
                student,
                orderRequest.recessTime()
        );
        Map<Producto, Integer> items = items(orderRequest, buffetId, pickupDate);

        advancePurchaseRulesService.validate(buffetId, pickupSlot, pickupDate, items);

        Purchase purchase = buildPurchase(
                student, buyer, buffetId, pickupDate, pickupSlot, orderRequest, items);

        purchaseValidator.validate(purchase);
        BigDecimal total = calculateTotal(items);

        processPaymentAndReserve(orderRequest.studentId(), total, buyer.id(), buffetId, items);

        purchase.setWithdrawalCode(withdrawalCodeService.generateCode(purchase));
        Purchase savedPurchase = purchaseRepository.save(purchase);
        registerPurchaseMovement(wallet(orderRequest.studentId()), total, buyer.id());
        return savedPurchase;
    }

    private Purchase buildPurchase(
            Alumno student,
            Buyer buyer,
            UUID buffetId,
            LocalDate pickupDate,
            FranjaHoraria pickupSlot,
            CreateAdvancePurchaseRequest.OrderRequest orderRequest,
            Map<Producto, Integer> items
    ) {
        Purchase purchase = Purchase.advance(
                student,
                items,
                MedioPago.CREDITOS,
                buyer.id(),
                buyer.type(),
                buffetId,
                pickupSlot.getId(),
                pickupDate
        );
        purchase.setSuggestionId(orderRequest.suggestionId());
        purchase.setTutorId(buyer.tutorId());
        return purchase;
    }

    private void processPaymentAndReserve(
            UUID studentId,
            BigDecimal total,
            UUID buyerId,
            UUID buffetId,
            Map<Producto, Integer> items
    ) {
        Billetera wallet = wallet(studentId);

        wallet.debit(total);
        inventoryAvailabilityService.reserve(buffetId, items);
        walletRepository.save(wallet);
    }

    private void validateRequest(CreateAdvancePurchaseRequest request) {
        if (request == null || request.orders() == null || request.orders().isEmpty()) {
            throw new IllegalArgumentException("orders is required");
        }
    }

    private Usuario authenticatedUser(String cognitoSub) {
        return usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
    }

    private Buyer authorizedBuyer(Usuario user, UUID studentId) {
        if (user.getRol() == RolUsuario.ALUMNO) {
            return studentBuyer(user, studentId);
        }

        if (user.getRol() == RolUsuario.PADRE) {
            return tutorBuyer(user, studentId);
        }

        throw new SecurityException("Rol no autorizado para realizar compras");
    }

    private Buyer studentBuyer(Usuario user, UUID studentId) {
        user.validatePurchaseForSelf(studentId);
        return new Buyer(user.getId(), BuyerType.STUDENT, null);
    }

    private Buyer tutorBuyer(Usuario user, UUID studentId) {
        Tutor tutor = tutorRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new SecurityException(
                        "No se encontro un perfil de tutor para este usuario"));

        tutor.validatePurchaseFor(studentId);

        return new Buyer(user.getId(), BuyerType.TUTOR, tutor.getId());
    }

    private UUID buffetId(CreateAdvancePurchaseRequest.OrderRequest orderRequest, Alumno student) {
        if (orderRequest.buffetId() != null) {
            return orderRequest.buffetId();
        }

        UUID schoolId = schoolId(student);
        return buffetRepository.findByColegioId(schoolId).stream()
                .filter(b -> Boolean.TRUE.equals(b.getActivo()))
                .findFirst()
                .map(com.recred.backend.domain.models.Buffet::getId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Buffet not found for school: " + schoolId));
    }

    private UUID schoolId(Alumno student) {
        if (student.getGrado() == null
                || student.getGrado().getColegio() == null
                || student.getGrado().getColegio().getId() == null) {
            throw new IllegalArgumentException("Student school is required");
        }

        return student.getGrado().getColegio().getId();
    }

    private LocalDate pickupDate(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("date is required");
        }

        try {
            return LocalDate.parse(date);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("La fecha de retiro no es valida");
        }
    }

    private Map<Producto, Integer> items(
            CreateAdvancePurchaseRequest.OrderRequest orderRequest,
            UUID buffetId,
            LocalDate pickupDate
    ) {
        if (orderRequest.items() == null || orderRequest.items().isEmpty()) {
            throw new IllegalArgumentException("items is required");
        }

        Map<Producto, Integer> items = new HashMap<>();
        for (CreateAdvancePurchaseRequest.ItemRequest itemRequest : orderRequest.items()) {
            processItemRequest(itemRequest, buffetId, pickupDate, items);
        }
        return items;
    }

    private void processItemRequest(
            CreateAdvancePurchaseRequest.ItemRequest itemRequest,
            UUID buffetId,
            LocalDate pickupDate,
            Map<Producto, Integer> items
    ) {
        Optional<Producto> productOpt = productRepository.findActiveByIdWithStock(
                itemRequest.productId(),
                buffetId
        );
        if (productOpt.isPresent()) {
            mergeOrPut(items, productOpt.get(), itemRequest.quantity());
        } else {
            Optional<Promotion> promotionOpt = promotionRepository
                    .findById(itemRequest.productId());
            if (promotionOpt.isPresent()) {
                processPromotion(promotionOpt.get(), itemRequest.quantity(),
                        buffetId, pickupDate, items);
            } else {
                throw new ProductNotFoundException(itemRequest.productId());
            }
        }
    }

    private void processPromotion(
            Promotion promotion,
            int quantity,
            UUID buffetId,
            LocalDate pickupDate,
            Map<Producto, Integer> items
    ) {
        promotion.validateFor(pickupDate, buffetId);

        if (promotion.getProducts() != null) {
            for (Producto p : promotion.getProducts()) {
                Producto activeProd = productRepository
                        .findActiveByIdWithStock(p.getId(), buffetId)
                        .orElseThrow(() -> new ProductNotFoundException(p.getId()));

                mergeOrPut(items, activeProd.discountedFor(promotion), quantity);
            }
        }
    }

    private void mergeOrPut(Map<Producto, Integer> items, Producto product, int quantity) {
        Producto existingKey = null;
        for (Producto k : items.keySet()) {
            if (k.hasSamePurchaseIdentityAs(product)) {
                existingKey = k;
                break;
            }
        }
        if (existingKey != null) {
            items.put(existingKey, items.get(existingKey) + quantity);
        } else {
            items.put(product, quantity);
        }
    }

    private Billetera wallet(UUID studentId) {
        return walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Student wallet not found: " + studentId));
    }

    private BigDecimal calculateTotal(Map<Producto, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> price(entry.getKey()).multiply(BigDecimal.valueOf(
                        entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal price(Producto product) {
        return product.getPrecio() == null ? BigDecimal.ZERO : product.getPrecio();
    }

    private void registerPurchaseMovement(Billetera wallet, BigDecimal amount, UUID userId) {
        if (movimientoBilleteraRepository == null) {
            return;
        }
        movimientoBilleteraRepository.registrarMovimiento(
                wallet.getId(),
                TipoMovimientoBilletera.COMPRA,
                amount.negate(),
                "Compra anticipada en buffet",
                userId
        );
    }

    private record Buyer(UUID id, BuyerType type, UUID tutorId) {
    }

    private record CreatedPurchases(
            Purchase firstPurchase,
            double total,
            Map<UUID, String> codes
    ) {
    }
}
