package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
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
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.persistence.SpringDataBuffetRepository;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final SpringDataBuffetRepository buffetRepository;
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
            SpringDataBuffetRepository buffetRepository,
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
            SpringDataBuffetRepository buffetRepository,
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
    public AdvancePurchaseResponse execute(AdvancePurchaseRequest request, String cognitoSub) {
        validateRequest(request);
        Usuario user = authenticatedUser(cognitoSub);

        Map<UUID, String> codes = new HashMap<>();
        Purchase firstPurchase = null;
        double total = 0;

        for (AdvancePurchaseRequest.OrderRequest orderRequest : request.orders()) {
            Purchase purchase = createPurchase(orderRequest, user);
            if (firstPurchase == null) {
                firstPurchase = purchase;
            }

            total += purchase.getTotalAmount().doubleValue();
            codes.put(purchase.getStudent().getId(), purchase.getWithdrawalCode());
        }

        return new AdvancePurchaseResponse(
                firstPurchase.getId().toString(),
                PurchaseStatus.from(firstPurchase),
                total,
                codes
        );
    }

    private Purchase createPurchase(
            AdvancePurchaseRequest.OrderRequest orderRequest,
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
        Map<Producto, Integer> items = items(orderRequest, buffetId);

        advancePurchaseRulesService.validate(buffetId, pickupSlot, pickupDate, items);

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

        purchaseValidator.validate(purchase);
        BigDecimal total = calculateTotal(items);
        Billetera wallet = wallet(orderRequest.studentId());

        if (!wallet.hasSufficientBalance(total)) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for student: " + orderRequest.studentId());
        }

        inventoryAvailabilityService.reserve(buffetId, items);
        wallet.debit(total);
        walletRepository.save(wallet);

        purchase.setWithdrawalCode(withdrawalCodeService.generateCode(purchase));
        Purchase savedPurchase = purchaseRepository.save(purchase);
        registerPurchaseMovement(wallet, total, buyer.id());
        return savedPurchase;
    }

    private void validateRequest(AdvancePurchaseRequest request) {
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
        if (user.getAlumno() == null || !user.getAlumno().getId().equals(studentId)) {
            throw new SecurityException("No autorizado para comprar a nombre de otro alumno");
        }

        return new Buyer(user.getId(), BuyerType.STUDENT, null);
    }

    private Buyer tutorBuyer(Usuario user, UUID studentId) {
        Tutor tutor = tutorRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new SecurityException(
                        "No se encontro un perfil de tutor para este usuario"));
        boolean isOwnChild = tutor.getHijos().stream()
                .anyMatch(child -> child.getId().equals(studentId));

        if (!isOwnChild) {
            throw new SecurityException("No autorizado para comprar a nombre de este alumno");
        }

        return new Buyer(user.getId(), BuyerType.TUTOR, tutor.getId());
    }

    private UUID buffetId(AdvancePurchaseRequest.OrderRequest orderRequest, Alumno student) {
        if (orderRequest.buffetId() != null) {
            return orderRequest.buffetId();
        }

        UUID schoolId = schoolId(student);
        return buffetRepository.findFirstByColegioEntityIdAndActivoTrue(schoolId)
                .map(BuffetEntity::getId)
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
            AdvancePurchaseRequest.OrderRequest orderRequest,
            UUID buffetId
    ) {
        if (orderRequest.items() == null || orderRequest.items().isEmpty()) {
            throw new IllegalArgumentException("items is required");
        }

        Map<Producto, Integer> items = new HashMap<>();
        for (AdvancePurchaseRequest.ItemRequest itemRequest : orderRequest.items()) {
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
                    Promotion promotion = promotionOpt.get();
                    if (promotion.getStatus() != PromotionStatus.ACTIVE) {
                        throw new IllegalArgumentException("La promocion no esta activa");
                    }
                    if (promotion.getBuffet() != null
                            && !promotion.getBuffet().getId().equals(buffetId)) {
                        throw new IllegalArgumentException(
                                "La promocion no pertenece a este buffet");
                    }
                    LocalDateTime now = LocalDateTime.now();
                    if (promotion.getStartDate() != null
                            && now.isBefore(promotion.getStartDate())) {
                        throw new IllegalArgumentException(
                                "La promocion aun no ha comenzado");
                    }
                    if (promotion.getEndDate() != null
                            && now.isAfter(promotion.getEndDate())) {
                        throw new IllegalArgumentException("La promocion ha finalizado");
                    }

                    if (promotion.getProducts() != null) {
                        BigDecimal discountPercentage = promotion.getDiscountPercentage();
                        BigDecimal multiplier = BigDecimal.ONE.subtract(
                                discountPercentage.divide(
                                        new BigDecimal("100"),
                                        4,
                                        java.math.RoundingMode.HALF_UP
                                )
                        );
                        for (Producto p : promotion.getProducts()) {
                            Producto activeProd = productRepository
                                    .findActiveByIdWithStock(p.getId(), buffetId)
                                    .orElseThrow(() ->
                                            new ProductNotFoundException(p.getId()));

                            Producto discountedProduct = new Producto();
                            discountedProduct.setId(activeProd.getId());
                            discountedProduct.setCategoria(activeProd.getCategoria());
                            discountedProduct.setNombre(activeProd.getNombre());
                            discountedProduct.setDescripcion(activeProd.getDescripcion());
                            discountedProduct.setPeso(activeProd.getPeso());
                            discountedProduct.setRequierePreparacion(
                                    activeProd.getRequierePreparacion()
                            );
                            discountedProduct.setActivo(activeProd.getActivo());
                            discountedProduct.setCreadoEn(activeProd.getCreadoEn());
                            discountedProduct.setActualizadoEn(activeProd.getActualizadoEn());
                            discountedProduct.setUrlImagen(activeProd.getUrlImagen());
                            discountedProduct.setStockActual(activeProd.getStockActual());
                            discountedProduct.setStockReservado(activeProd.getStockReservado());
                            discountedProduct.setStockMinimo(activeProd.getStockMinimo());
                            discountedProduct.setStockDisponible(activeProd.getStockDisponible());
                            discountedProduct.setEstadoInventario(
                                    activeProd.getEstadoInventario()
                            );
                            discountedProduct.setTipoManejoInventario(
                                    activeProd.getTipoManejoInventario()
                            );
                            discountedProduct.setCupoMaximoDiario(
                                    activeProd.getCupoMaximoDiario()
                            );
                            discountedProduct.setCupoDisponibleDia(
                                    activeProd.getCupoDisponibleDia()
                            );
                            discountedProduct.setMedidas(activeProd.getMedidas());
                            discountedProduct.setTipos(activeProd.getTipos());
                            discountedProduct.setClasificacionesSalud(
                                    activeProd.getClasificacionesSalud()
                            );

                            BigDecimal originalPrice = activeProd.getPrecio() != null
                                    ? activeProd.getPrecio()
                                    : BigDecimal.ZERO;
                            BigDecimal discountedPrice = originalPrice
                                    .multiply(multiplier)
                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                            discountedProduct.setPrecio(discountedPrice);
                            discountedProduct.setPromocionId(promotion.getId());
                            discountedProduct.setPromocionNombre(promotion.getName());

                            mergeOrPut(items, discountedProduct, itemRequest.quantity());
                        }
                    }
                } else {
                    throw new ProductNotFoundException(itemRequest.productId());
                }
            }
        }
        return items;
    }

    private void mergeOrPut(Map<Producto, Integer> items, Producto product, int quantity) {
        Producto existingKey = null;
        for (Producto k : items.keySet()) {
            boolean sameId = k.getId().equals(product.getId());
            boolean samePrice = k.getPrecio().compareTo(product.getPrecio()) == 0;
            boolean samePromotion = java.util.Objects.equals(
                    k.getPromocionId(),
                    product.getPromocionId()
            );
            if (sameId && samePrice && samePromotion) {
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
}
