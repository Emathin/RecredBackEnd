package com.recred.backend.unit.application;

import com.recred.backend.application.CreateAssistantAdvancePurchase;
import com.recred.backend.application.CreateAssistantAdvancePurchaseRequest;
import com.recred.backend.application.CreateAssistantAdvancePurchaseResponse;
import com.recred.backend.domain.exceptions.InsufficientStockException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.TipoCompra;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAssistantAdvancePurchaseTest {

    @Mock
    private AlumnoRepository studentRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private RecessTimeSlotResolver recessTimeSlotResolver;

    @Mock
    private ValidadorHorarioService timeValidator;

    @Mock
    private ValidacionRestriccionesService healthValidator;

    @Mock
    private ValidadorPresupuestoService budgetValidator;

    @Mock
    private WithdrawalCodeService withdrawalCodeService;
    @Mock
    private InventoryAvailabilityService inventoryAvailabilityService;
    @Mock
    private AdvancePurchaseRulesService advancePurchaseRulesService;
    @Mock
    private MovimientoBilleteraRepository movimientoBilleteraRepository;

    private CreateAssistantAdvancePurchase useCase;
    private UUID studentId;
    private UUID schoolId;
    private UUID buffetId;
    private UUID slotId;
    private Producto product;
    private Billetera wallet;

    @BeforeEach
    void setUp() {
        useCase = new CreateAssistantAdvancePurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                recessTimeSlotResolver,
                timeValidator,
                healthValidator,
                budgetValidator,
                withdrawalCodeService,
                Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"),
                        ZoneId.of("America/Argentina/Buenos_Aires")),
                inventoryAvailabilityService,
                advancePurchaseRulesService
        );
        studentId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        product = product();
        wallet = wallet();
    }

    @Test
    void executeCreatesPendingAdvancePurchaseWithPickupSlotAndWithdrawalCode() {
        LocalDate pickupDate = LocalDate.of(2026, 5, 27);
        Alumno student = student();
        Map<Producto, Integer> cart = Map.of(product, 2);
        FranjaHoraria slot = timeSlot();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(recessTimeSlotResolver.resolveByBuffet(buffetId, RecessTime.FIRST_RECESS))
                .thenReturn(slot);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn("ABC123");
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        CreateAssistantAdvancePurchaseResponse result =
                useCase.execute(request(buffetId, cart, pickupDate));

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase savedPurchase = purchaseCaptor.getValue();

        assertEquals(TipoCompra.ANTICIPADA, savedPurchase.getTipo());
        assertEquals(EstadoCompra.PENDIENTE, savedPurchase.getStatus());
        assertEquals(buffetId, savedPurchase.getBuffetId());
        assertEquals(slotId, savedPurchase.getPickupSlotId());
        assertEquals(pickupDate, savedPurchase.getPickupDate());
        assertEquals("ABC123", savedPurchase.getWithdrawalCode());
        assertEquals(new BigDecimal("30.00"), savedPurchase.getTotalAmount());
        assertEquals(new BigDecimal("70.00"), wallet.getSaldoCreditos());
        assertEquals(savedPurchase.getStatus(), result.status());
        assertEquals(savedPurchase.getWithdrawalCode(), result.withdrawalCode());
        assertEquals(savedPurchase.getTotalAmount(), result.totalAmount());

        verify(timeValidator).validarCompra(
                eq(student),
                eq(cart.keySet()),
                eq(LocalDateTime.of(2026, 5, 27, 10, 0))
        );
        verify(healthValidator).validarCompra(eq(student), eq(cart.keySet()));
        verify(budgetValidator).validarPresupuesto(
                eq(studentId),
                eq(new BigDecimal("30.00")),
                eq(Map.of()),
                eq(LocalDateTime.of(2026, 5, 27, 10, 0))
        );
        verify(advancePurchaseRulesService).validate(buffetId, slot, pickupDate, cart);
        verify(inventoryAvailabilityService).reserve(buffetId, cart);
        verify(walletRepository).save(wallet);
        verify(recessTimeSlotResolver).resolveByBuffet(buffetId, RecessTime.FIRST_RECESS);
    }

    @Test
    void execute_WhenStudentHasNoGrade_ShouldResolvePickupSlotByBuffet() {
        LocalDate pickupDate = LocalDate.of(2026, 5, 27);
        Alumno student = studentWithoutGrade();
        Map<Producto, Integer> cart = Map.of(product, 1);
        FranjaHoraria slot = timeSlot();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(recessTimeSlotResolver.resolveByBuffet(buffetId, RecessTime.FIRST_RECESS))
                .thenReturn(slot);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn("ABC123");
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        CreateAssistantAdvancePurchaseResponse result =
                useCase.execute(request(buffetId, cart, pickupDate));

        assertEquals(slotId, result.pickupSlotId());
        verify(recessTimeSlotResolver).resolveByBuffet(buffetId, RecessTime.FIRST_RECESS);
    }

    @Test
    void execute_WhenStockIsInsufficient_ShouldNotDebitWalletOrSavePurchase() {
        LocalDate pickupDate = LocalDate.of(2026, 5, 27);
        Alumno student = student();
        Map<Producto, Integer> cart = Map.of(product, 2);
        FranjaHoraria slot = timeSlot();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(recessTimeSlotResolver.resolveByBuffet(buffetId, RecessTime.FIRST_RECESS))
                .thenReturn(slot);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        doThrow(new InsufficientStockException(product.getId(), 2, 1))
                .when(inventoryAvailabilityService)
                .reserve(buffetId, cart);

        assertThrows(InsufficientStockException.class,
                () -> useCase.execute(request(buffetId, cart, pickupDate)));

        verify(walletRepository, never()).save(wallet);
        verify(purchaseRepository, never()).save(any(Purchase.class));
        verify(withdrawalCodeService, never()).generateCode(any(Purchase.class));
    }

    @Test
    void executeThrowsWhenBuffetIdIsMissing() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                request(null, Map.of(product, 1), LocalDate.of(2026, 5, 27))));
    }

    @Test
    void executeThrowsWhenStudentDoesNotExist() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(com.recred.backend.domain.exceptions.AlumnoNoEncontradoException.class,
                () -> useCase.execute(request(
                        buffetId,
                        Map.of(product, 1),
                        LocalDate.of(2026, 5, 27)
                )));
    }

    @Test
    void executeThrowsWhenWalletDoesNotExist() {
        LocalDate pickupDate = LocalDate.of(2026, 5, 27);
        Alumno student = student();
        Map<Producto, Integer> cart = Map.of(product, 1);
        FranjaHoraria slot = timeSlot();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(recessTimeSlotResolver.resolveByBuffet(buffetId, RecessTime.FIRST_RECESS))
                .thenReturn(slot);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> useCase.execute(request(buffetId, cart, pickupDate)));

        verify(inventoryAvailabilityService, never()).reserve(any(), any());
    }

    @Test
    void executeThrowsWhenWalletBalanceIsInsufficient() {
        LocalDate pickupDate = LocalDate.of(2026, 5, 27);
        Alumno student = student();
        Map<Producto, Integer> cart = Map.of(product, 10);
        FranjaHoraria slot = timeSlot();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(recessTimeSlotResolver.resolveByBuffet(buffetId, RecessTime.FIRST_RECESS))
                .thenReturn(slot);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        assertThrows(com.recred.backend.domain.exceptions.InsufficientBalanceException.class,
                () -> useCase.execute(request(buffetId, cart, pickupDate)));

        verify(inventoryAvailabilityService, never()).reserve(any(), any());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void executeGroupsTotalsByCategoryAndRegistersUserMovement() {
        LocalDate pickupDate = LocalDate.of(2026, 5, 27);
        UUID userId = UUID.randomUUID();
        Alumno student = student();
        student.setUsuarioId(userId);
        Producto freeProduct = productWithCategory(null, UUID.randomUUID());
        Producto paidProduct = productWithCategory(new BigDecimal("15.00"), UUID.randomUUID());
        Map<Producto, Integer> cart = Map.of(freeProduct, 2, paidProduct, 2);
        FranjaHoraria slot = timeSlot();
        CreateAssistantAdvancePurchase useCaseWithMovements = useCaseWithMovements();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(recessTimeSlotResolver.resolveByBuffet(buffetId, RecessTime.FIRST_RECESS))
                .thenReturn(slot);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn("ABC123");
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        useCaseWithMovements.execute(request(buffetId, cart, pickupDate));

        verify(budgetValidator).validarPresupuesto(
                eq(studentId),
                eq(new BigDecimal("30.00")),
                eq(Map.of(
                        freeProduct.getCategoria().getId(),
                        BigDecimal.ZERO,
                        paidProduct.getCategoria().getId(),
                        new BigDecimal("30.00")
                )),
                eq(LocalDateTime.of(2026, 5, 27, 10, 0))
        );
        verify(movimientoBilleteraRepository).registrarMovimiento(
                eq(wallet.getId()),
                eq(TipoMovimientoBilletera.COMPRA),
                eq(new BigDecimal("-30.00")),
                eq("Compra anticipada asistida"),
                eq(userId)
        );
    }

    @Test
    void executeRegistersSystemMovementWhenStudentHasNoUserId() {
        LocalDate pickupDate = LocalDate.of(2026, 5, 27);
        Alumno student = student();
        Map<Producto, Integer> cart = Map.of(product, 1);
        FranjaHoraria slot = timeSlot();
        CreateAssistantAdvancePurchase useCaseWithMovements = useCaseWithMovements();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(recessTimeSlotResolver.resolveByBuffet(buffetId, RecessTime.FIRST_RECESS))
                .thenReturn(slot);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn("ABC123");
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        useCaseWithMovements.execute(request(buffetId, cart, pickupDate));

        verify(movimientoBilleteraRepository).registrarMovimientoSistema(
                eq(wallet.getId()),
                eq(TipoMovimientoBilletera.COMPRA),
                eq(new BigDecimal("-15.00")),
                eq("Compra anticipada asistida")
        );
    }

    private Alumno student() {
        Colegio school = new Colegio();
        school.setId(schoolId);
        Grado grade = new Grado();
        grade.setColegio(school);
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setGrado(grade);
        return student;
    }

    private Alumno studentWithoutGrade() {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setPrecio(new BigDecimal("15.00"));
        return product;
    }

    private Producto productWithCategory(BigDecimal price, UUID categoryId) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        Producto product = product();
        product.setPrecio(price);
        product.setCategoria(category);
        return product;
    }

    private Billetera wallet() {
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("100.00"));
        return wallet;
    }

    private CreateAssistantAdvancePurchase useCaseWithMovements() {
        return new CreateAssistantAdvancePurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                recessTimeSlotResolver,
                timeValidator,
                healthValidator,
                budgetValidator,
                withdrawalCodeService,
                Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"),
                        ZoneId.of("America/Argentina/Buenos_Aires")),
                inventoryAvailabilityService,
                advancePurchaseRulesService,
                movimientoBilleteraRepository
        );
    }

    private CreateAssistantAdvancePurchaseRequest request(
            UUID buffetId,
            Map<Producto, Integer> cart,
            LocalDate pickupDate
    ) {
        return new CreateAssistantAdvancePurchaseRequest(
                studentId,
                buffetId,
                cart,
                RecessTime.FIRST_RECESS,
                pickupDate
        );
    }

    private FranjaHoraria timeSlot() {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(slotId);
        timeSlot.setDescripcion("Primer recreo");
        timeSlot.setHoraInicio(LocalTime.of(10, 0));
        timeSlot.setHoraFin(LocalTime.of(10, 20));
        return timeSlot;
    }
}
