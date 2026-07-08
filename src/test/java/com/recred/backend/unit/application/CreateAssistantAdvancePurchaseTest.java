package com.recred.backend.unit.application;

import com.recred.backend.application.CreateAssistantAdvancePurchase;
import com.recred.backend.application.CreateAssistantAdvancePurchaseRequest;
import com.recred.backend.application.CreateAssistantAdvancePurchaseResponse;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
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
import org.mockito.ArgumentCaptor;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateAssistantAdvancePurchaseTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final String WITHDRAWAL_CODE = "ABC123";
    private static final LocalDate PICKUP_DATE = LocalDate.of(2026, 5, 27);
    private static final LocalDateTime PICKUP_DATETIME = LocalDateTime.of(2026, 5, 27, 10, 0);
    private static final BigDecimal WALLET_INITIAL_BALANCE = new BigDecimal("100.00");
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("15.00");
    private static final BigDecimal AMOUNT_30 = new BigDecimal("30.00");
    private static final BigDecimal AMOUNT_15 = new BigDecimal("15.00");
    private static final BigDecimal WALLET_FINAL_BALANCE = new BigDecimal("70.00");
    
    private AlumnoRepository studentRepository;
    private WalletRepository walletRepository;
    private PurchaseRepository purchaseRepository;
    private RecessTimeSlotResolver recessTimeSlotResolver;
    private ValidadorHorarioService timeValidator;
    private ValidacionRestriccionesService healthValidator;
    private ValidadorPresupuestoService budgetValidator;
    private WithdrawalCodeService withdrawalCodeService;
    private InventoryAvailabilityService inventoryAvailabilityService;
    private AdvancePurchaseRulesService advancePurchaseRulesService;
    private MovimientoBilleteraRepository movimientoBilleteraRepository;
    private CreateAssistantAdvancePurchase useCase;
    private Producto product;
    private Billetera wallet;

    @BeforeEach
    public void init() {
        studentRepository = mock(AlumnoRepository.class);
        walletRepository = mock(WalletRepository.class);
        purchaseRepository = mock(PurchaseRepository.class);
        recessTimeSlotResolver = mock(RecessTimeSlotResolver.class);
        timeValidator = mock(ValidadorHorarioService.class);
        healthValidator = mock(ValidacionRestriccionesService.class);
        budgetValidator = mock(ValidadorPresupuestoService.class);
        withdrawalCodeService = mock(WithdrawalCodeService.class);
        inventoryAvailabilityService = mock(InventoryAvailabilityService.class);
        advancePurchaseRulesService = mock(AdvancePurchaseRulesService.class);
        movimientoBilleteraRepository = mock(MovimientoBilleteraRepository.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));

        useCase = new CreateAssistantAdvancePurchase(
                studentRepository,
                walletRepository,
                purchaseRepository,
                recessTimeSlotResolver,
                timeValidator,
                healthValidator,
                budgetValidator,
                withdrawalCodeService,
                fixedClock,
                inventoryAvailabilityService,
                advancePurchaseRulesService,
                movimientoBilleteraRepository
        );

        product = aProduct(PRODUCT_PRICE);
        wallet = aWallet(WALLET_INITIAL_BALANCE);
    }

    @Test
    public void createAdvancePurchaseSuccessfully() {
        Alumno student = aStudentWithGrade();
        FranjaHoraria slot = aTimeSlot();
        
        givenAStudent(student);
        givenATimeSlotForFirstRecess(slot);
        givenAWallet(wallet);
        givenGeneratedWithdrawalCode(WITHDRAWAL_CODE);
        givenPurchaseIsSaved();

        CreateAssistantAdvancePurchaseResponse response = whenExecuteUseCaseWith(Map.of(product, 2));

        thenPurchaseShouldBeCreatedAndSaved(response);
        thenValidationsShouldBeCalled(student, Map.of(product, 2), slot, AMOUNT_30, Map.of());
        thenWalletShouldBeDebitedAndSaved(WALLET_FINAL_BALANCE);
    }

    @Test
    public void resolvePickupSlotByBuffetWhenStudentHasNoGrade() {
        givenAStudent(aStudentWithoutGrade());
        givenATimeSlotForFirstRecess(aTimeSlot());
        givenAWallet(wallet);
        givenGeneratedWithdrawalCode(WITHDRAWAL_CODE);
        givenPurchaseIsSaved();

        CreateAssistantAdvancePurchaseResponse response = whenExecuteUseCaseWith(Map.of(product, 1));

        thenPickupSlotShouldBeResolved(response, SLOT_ID);
    }

    @Test
    public void executeThrowsWhenStudentDoesNotExist() {
        givenStudentDoesNotExist();

        assertThrows(AlumnoNoEncontradoException.class, 
                () -> whenExecuteUseCaseWith(Map.of(product, 1)));
    }

    @Test
    public void executeThrowsWhenWalletDoesNotExist() {
        givenAStudent(aStudentWithGrade());
        givenATimeSlotForFirstRecess(aTimeSlot());
        givenWalletDoesNotExist();

        assertThrows(IllegalStateException.class, 
                () -> whenExecuteUseCaseWith(Map.of(product, 1)));
    }

    @Test
    public void executeThrowsWhenWalletBalanceIsInsufficient() {
        givenAStudent(aStudentWithGrade());
        givenATimeSlotForFirstRecess(aTimeSlot());
        givenAWallet(aWallet(BigDecimal.ZERO));

        assertThrows(InsufficientBalanceException.class, 
                () -> whenExecuteUseCaseWith(Map.of(product, 1)));
    }

    @Test
    public void executeThrowsWhenStockIsInsufficient() {
        givenAStudent(aStudentWithGrade());
        givenATimeSlotForFirstRecess(aTimeSlot());
        givenAWallet(wallet);
        givenInsufficientStockFor(Map.of(product, 2));

        assertThrows(InsufficientStockException.class, 
                () -> whenExecuteUseCaseWith(Map.of(product, 2)));

        thenNoPurchaseOrWalletChangesAreSaved();
    }

    @Test
    public void executeGroupsTotalsByCategoryAndRegistersUserMovement() {
        Alumno student = aStudentWithGrade();
        student.setUsuarioId(USER_ID);
        Producto freeProduct = aProductWithCategory(BigDecimal.ZERO, CATEGORY_ID);
        Producto paidProduct = aProductWithCategory(PRODUCT_PRICE, UUID.randomUUID());
        Map<Producto, Integer> cart = Map.of(freeProduct, 2, paidProduct, 2);

        givenAStudent(student);
        givenATimeSlotForFirstRecess(aTimeSlot());
        givenAWallet(wallet);
        givenGeneratedWithdrawalCode(WITHDRAWAL_CODE);
        givenPurchaseIsSaved();

        whenExecuteUseCaseWith(cart);

        thenUserMovementShouldBeRegistered(AMOUNT_30.negate());
        thenBudgetValidatorShouldBeCalledWith(AMOUNT_30, Map.of(
                freeProduct.getCategoria().getId(), BigDecimal.ZERO,
                paidProduct.getCategoria().getId(), AMOUNT_30
        ));
    }

    @Test
    public void executeRegistersSystemMovementWhenStudentHasNoUserId() {
        givenAStudent(aStudentWithGrade());
        givenATimeSlotForFirstRecess(aTimeSlot());
        givenAWallet(wallet);
        givenGeneratedWithdrawalCode(WITHDRAWAL_CODE);
        givenPurchaseIsSaved();

        whenExecuteUseCaseWith(Map.of(product, 1));

        thenSystemMovementShouldBeRegistered(AMOUNT_15.negate());
    }

    private void givenAStudent(Alumno student) {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private void givenATimeSlotForFirstRecess(FranjaHoraria slot) {
        when(recessTimeSlotResolver.resolveByBuffet(BUFFET_ID, RecessTime.FIRST_RECESS)).thenReturn(slot);
    }

    private void givenAWallet(Billetera wallet) {
        when(walletRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(wallet));
    }

    private void givenWalletDoesNotExist() {
        when(walletRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private void givenGeneratedWithdrawalCode(String code) {
        when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn(code);
    }

    private void givenPurchaseIsSaved() {
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenInsufficientStockFor(Map<Producto, Integer> cart) {
        doThrow(new InsufficientStockException(product.getId(), 2, 1)).when(inventoryAvailabilityService).reserve(BUFFET_ID, cart);
    }

    private CreateAssistantAdvancePurchaseResponse whenExecuteUseCaseWith(Map<Producto, Integer> cart) {
        return useCase.execute(new CreateAssistantAdvancePurchaseRequest(
                STUDENT_ID, BUFFET_ID, cart, RecessTime.FIRST_RECESS, PICKUP_DATE
        ));
    }

    private void thenPurchaseShouldBeCreatedAndSaved(CreateAssistantAdvancePurchaseResponse response) {
        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase savedPurchase = purchaseCaptor.getValue();

        assertThat(savedPurchase.getTipo()).isEqualTo(TipoCompra.ANTICIPADA);
        assertThat(savedPurchase.getStatus()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(savedPurchase.getBuffetId()).isEqualTo(BUFFET_ID);
        assertThat(savedPurchase.getPickupSlotId()).isEqualTo(SLOT_ID);
        assertThat(savedPurchase.getPickupDate()).isEqualTo(PICKUP_DATE);
        assertThat(savedPurchase.getWithdrawalCode()).isEqualTo(WITHDRAWAL_CODE);
        assertThat(savedPurchase.getTotalAmount()).isEqualTo(AMOUNT_30);
        
        assertThat(response.status()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(response.withdrawalCode()).isEqualTo(WITHDRAWAL_CODE);
        assertThat(response.totalAmount()).isEqualTo(AMOUNT_30);
    }

    private void thenValidationsShouldBeCalled(Alumno student, Map<Producto, Integer> cart, FranjaHoraria slot, BigDecimal totalAmount, Map<UUID, BigDecimal> totalsByCategory) {
        verify(timeValidator).validarCompra(eq(student), eq(cart.keySet()), eq(PICKUP_DATETIME));
        verify(healthValidator).validarCompra(eq(student), eq(cart.keySet()));
        verify(budgetValidator).validarPresupuesto(eq(STUDENT_ID), eq(totalAmount), eq(totalsByCategory), eq(PICKUP_DATETIME));
        verify(advancePurchaseRulesService).validate(BUFFET_ID, slot, PICKUP_DATE, cart);
        verify(inventoryAvailabilityService).reserve(BUFFET_ID, cart);
        verify(recessTimeSlotResolver).resolveByBuffet(BUFFET_ID, RecessTime.FIRST_RECESS);
    }

    private void thenBudgetValidatorShouldBeCalledWith(BigDecimal totalAmount, Map<UUID, BigDecimal> totalsByCategory) {
        verify(budgetValidator).validarPresupuesto(eq(STUDENT_ID), eq(totalAmount), eq(totalsByCategory), eq(PICKUP_DATETIME));
    }

    private void thenWalletShouldBeDebitedAndSaved(BigDecimal finalBalance) {
        assertThat(wallet.getSaldoCreditos()).isEqualTo(finalBalance);
        verify(walletRepository).save(wallet);
    }

    private void thenNoPurchaseOrWalletChangesAreSaved() {
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
        verify(withdrawalCodeService, never()).generateCode(any());
    }

    private void thenPickupSlotShouldBeResolved(CreateAssistantAdvancePurchaseResponse response, UUID expectedSlotId) {
        assertThat(response.pickupSlotId()).isEqualTo(expectedSlotId);
        verify(recessTimeSlotResolver).resolveByBuffet(BUFFET_ID, RecessTime.FIRST_RECESS);
    }

    private void thenUserMovementShouldBeRegistered(BigDecimal amount) {
        verify(movimientoBilleteraRepository).registrarMovimiento(
                eq(wallet.getId()), eq(TipoMovimientoBilletera.COMPRA), eq(amount), eq("Compra anticipada asistida"), eq(USER_ID)
        );
    }

    private void thenSystemMovementShouldBeRegistered(BigDecimal amount) {
        verify(movimientoBilleteraRepository).registrarMovimientoSistema(
                eq(wallet.getId()), eq(TipoMovimientoBilletera.COMPRA), eq(amount), eq("Compra anticipada asistida")
        );
    }

    private Alumno aStudentWithGrade() {
        Colegio school = new Colegio();
        school.setId(SCHOOL_ID);
        Grado grade = new Grado();
        grade.setColegio(school);
        Alumno student = new Alumno();
        student.setId(STUDENT_ID);
        student.setGrado(grade);
        return student;
    }

    private Alumno aStudentWithoutGrade() {
        Alumno student = new Alumno();
        student.setId(STUDENT_ID);
        return student;
    }

    private Producto aProduct(BigDecimal price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setPrecio(price);
        return product;
    }

    private Producto aProductWithCategory(BigDecimal price, UUID categoryId) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        Producto product = aProduct(price);
        product.setCategoria(category);
        return product;
    }

    private Billetera aWallet(BigDecimal balance) {
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(balance);
        return wallet;
    }

    private FranjaHoraria aTimeSlot() {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(SLOT_ID);
        timeSlot.setDescripcion("Primer recreo");
        timeSlot.setHoraInicio(LocalTime.of(10, 0));
        timeSlot.setHoraFin(LocalTime.of(10, 20));
        return timeSlot;
    }
}
