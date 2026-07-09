package com.recred.backend.unit.application;

import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateAdvancePurchaseRequest;
import com.recred.backend.application.CreateAdvancePurchaseResponse;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PromotionRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.AdvancePurchaseRulesService;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.RecessTimeSlotResolver;
import com.recred.backend.domain.services.WithdrawalCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAdvancePurchaseUseCaseTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID TUTOR_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID PROMOTION_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID PURCHASE_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final UUID SUGGESTION_ID = UUID.randomUUID();
    private static final String COGNITO_SUB = "sub";
    private static final LocalDate PICKUP_DATE = LocalDate.of(2026, 6, 20);
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("25.00");
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("100.00");

    @Mock private PurchaseValidator purchaseValidator;
    @Mock private AlumnoRepository alumnoRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private WithdrawalCodeService withdrawalCodeService;
    @Mock private InventoryAvailabilityService inventoryAvailabilityService;
    @Mock private RecessTimeSlotResolver recessTimeSlotResolver;
    @Mock private AdvancePurchaseRulesService advancePurchaseRulesService;
    @Mock private WalletRepository walletRepository;
    @Mock private BuffetRepository buffetRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private PromotionRepository promotionRepository;

    @InjectMocks
    private CreateAdvancePurchase useCase;

    @Test
    void execute_WhenTutorBuysForOwnChild_ShouldCreateAdvancePurchase() {
        givenValidContextForTutorPurchase();

        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, SUGGESTION_ID);
        CreateAdvancePurchaseResponse response = whenExecute(request);

        thenPurchaseIsCreatedForTutor(response);
    }

    @Test
    void execute_WhenStudentBuysForThemself_ShouldCreateStudentPurchase() {
        givenValidContextForStudentPurchase();

        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        CreateAdvancePurchaseResponse response = whenExecute(request);

        thenPurchaseIsCreatedForStudent(response);
    }

    @Test
    void execute_WhenBuffetIdIsMissing_ShouldUseStudentSchoolBuffet() {
        givenValidContextForStudentSchoolPurchase();

        CreateAdvancePurchaseRequest request = givenRequest(null, null);
        whenExecute(request);

        thenInventoryIsReserved();
    }

    @Test
    void execute_WhenTutorDoesNotOwnStudent_ShouldThrowSecurityException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExistsForDifferentStudent();
        givenStudentExists(STUDENT_ID);

        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        
        thenThrowsSecurityExceptionAndNoPurchase(request);
    }

    @Test
    void execute_WhenStudentBuysForAnotherStudent_ShouldThrowSecurityException() {
        givenUserExists(USER_ID, RolUsuario.ALUMNO);
        givenUserIsDifferentStudent();
        givenStudentExists(STUDENT_ID);

        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        
        thenThrowsSecurityExceptionForStudentRole(request);
    }

    @Test
    void execute_WhenBalanceIsInsufficient_ShouldThrowException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();
        givenProductExists(PRODUCT_ID, DEFAULT_PRICE);
        givenWalletWithInsufficientBalance();

        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        
        thenThrowsInsufficientBalanceException(request);
    }

    @Test
    void execute_WhenOrdersAreEmpty_ShouldThrowException() {
        CreateAdvancePurchaseRequest request = new CreateAdvancePurchaseRequest(List.of());
        thenThrowsIllegalArgumentExceptionBeforeProcessing(request);
    }

    @Test
    void execute_WhenRequestIsNull_ShouldThrowException() {
        thenThrowsIllegalArgumentExceptionBeforeProcessing(null);
    }

    @Test
    void execute_WhenOrdersAreNull_ShouldThrowException() {
        CreateAdvancePurchaseRequest request = new CreateAdvancePurchaseRequest(null);
        thenThrowsIllegalArgumentExceptionBeforeProcessing(request);
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowSecurityException() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUB)).thenReturn(Optional.empty());
        
        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        
        thenThrowsSecurityException(request);
    }

    @Test
    void execute_WhenStudentDoesNotExist_ShouldThrowAlumnoNoEncontradoException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
        
        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        
        thenThrowsAlumnoNoEncontradoException(request);
    }

    @Test
    void execute_WhenRoleIsNotAllowed_ShouldThrowSecurityException() {
        givenUserExists(USER_ID, RolUsuario.VENDEDOR);
        givenStudentExists(STUDENT_ID);
        
        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        
        thenThrowsSecurityException(request);
    }

    @Test
    void execute_WhenTutorProfileDoesNotExist_ShouldThrowSecurityException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        when(tutorRepository.findByUsuarioId(USER_ID)).thenReturn(Optional.empty());
        givenStudentExists(STUDENT_ID);
        
        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        
        thenThrowsSecurityException(request);
    }

    @Test
    void execute_WhenDefaultBuffetDoesNotExist_ShouldThrowException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentWithSchoolExists(STUDENT_ID, SCHOOL_ID);
        when(buffetRepository.findByColegioId(SCHOOL_ID)).thenReturn(List.of());

        CreateAdvancePurchaseRequest request = givenRequest(null, null);
        
        thenThrowsIllegalArgumentException(request);
    }

    @Test
    void execute_WhenStudentSchoolIsMissing_ShouldThrowException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExistsWithoutSchool();
        
        CreateAdvancePurchaseRequest request = givenRequest(null, null);
        
        thenThrowsIllegalArgumentException(request);
    }

    @Test
    void execute_WhenDateIsMissing_ShouldThrowException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);

        CreateAdvancePurchaseRequest request = givenRequestWithNoDate();
        
        thenThrowsIllegalArgumentException(request);
    }

    @Test
    void execute_WhenDateIsInvalid_ShouldThrowException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);

        CreateAdvancePurchaseRequest request = givenRequestWithInvalidDate();
        
        thenThrowsIllegalArgumentException(request);
    }

    @Test
    void execute_WhenItemsAreMissing_ShouldThrowException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();

        CreateAdvancePurchaseRequest request = givenRequestWithNoItems();
        
        thenThrowsIllegalArgumentException(request);
    }

    @Test
    void execute_WhenProductDoesNotExist_ShouldThrowException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();
        when(productRepository.findActiveByIdWithStock(PRODUCT_ID, BUFFET_ID))
                .thenReturn(Optional.empty());

        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        
        thenThrowsProductNotFoundException(request);
    }

    @Test
    void execute_WhenWalletDoesNotExist_ShouldThrowException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();
        givenProductExists(PRODUCT_ID, DEFAULT_PRICE);
        when(walletRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());

        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        
        thenThrowsIllegalStateException(request);
    }

    @Test
    void execute_WhenProductPriceIsNull_ShouldUseZeroTotal() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();
        givenProductExists(PRODUCT_ID, null);
        givenWalletExists(STUDENT_ID, DEFAULT_BALANCE);
        givenWithdrawalCodeGenerated("ABC123");
        givenPurchaseIsSaved();

        CreateAdvancePurchaseRequest request = givenRequest(BUFFET_ID, null);
        CreateAdvancePurchaseResponse response = whenExecute(request);

        thenResponseTotalIsZero(response);
    }

    @Test
    void execute_WithShortConstructor_ShouldCreatePurchaseWithoutMovementRepository() {
        CreateAdvancePurchase localUseCase = buildUseCaseWithoutMovementRepository();
        
        givenValidContextForTutorPurchase();

        CreateAdvancePurchaseRequest request = givenRequestWithCognitoSub(BUFFET_ID, null);
        CreateAdvancePurchaseResponse response = localUseCase.execute(request);

        thenOrderIdMatches(response);
    }

    @Test
    void execute_WhenMovementRepositoryExists_ShouldRegisterPurchaseMovement() {
        MovimientoBilleteraRepository movementRepository = mock(MovimientoBilleteraRepository.class);
        CreateAdvancePurchase localUseCase = buildUseCaseWithMovementRepository(movementRepository);

        givenValidContextForTutorPurchase();
        
        CreateAdvancePurchaseRequest request = givenRequestWithCognitoSub(BUFFET_ID, null);
        localUseCase.execute(request);

        thenMovementIsRegistered(movementRepository);
    }

    @Test
    void execute_WhenPromotionIsNotActive_ShouldThrowIllegalArgumentException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();
        when(productRepository.findActiveByIdWithStock(PROMOTION_ID, BUFFET_ID))
                .thenReturn(Optional.empty());
        givenPromotionExists(
                PROMOTION_ID, 
                PromotionStatus.INACTIVE, 
                BUFFET_ID, 
                PICKUP_DATE.atStartOfDay().minusDays(1), 
                PICKUP_DATE.atStartOfDay().plusDays(1)
        );

        CreateAdvancePurchaseRequest request = givenPromotionRequest();
        
        thenThrowsIllegalArgumentExceptionWithMessage(request, "Promotion is not active");
    }

    @Test
    void execute_WhenPromotionDoesNotBelongToBuffet_ShouldThrowIllegalArgumentException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();
        when(productRepository.findActiveByIdWithStock(PROMOTION_ID, BUFFET_ID))
                .thenReturn(Optional.empty());
        givenPromotionExists(
                PROMOTION_ID, 
                PromotionStatus.ACTIVE, 
                UUID.randomUUID(), 
                PICKUP_DATE.atStartOfDay().minusDays(1), 
                PICKUP_DATE.atStartOfDay().plusDays(1)
        );

        CreateAdvancePurchaseRequest request = givenPromotionRequest();
        
        thenThrowsIllegalArgumentExceptionWithMessage(request, "Promotion does not belong to this buffet");
    }

    @Test
    void execute_WhenPromotionContainsInactiveProduct_ShouldThrowProductNotFoundException() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();
        when(productRepository.findActiveByIdWithStock(PROMOTION_ID, BUFFET_ID))
                .thenReturn(Optional.empty());
        
        Promotion promotion = givenPromotionExists(
                PROMOTION_ID, 
                PromotionStatus.ACTIVE, 
                BUFFET_ID, 
                PICKUP_DATE.atStartOfDay().minusDays(1), 
                PICKUP_DATE.atStartOfDay().plusDays(1)
        );
        givenPromotionWithInactiveProduct(promotion);

        CreateAdvancePurchaseRequest request = givenPromotionRequest();
        
        thenThrowsProductNotFoundException(request);
    }

    private void thenPurchaseIsCreatedForTutor(CreateAdvancePurchaseResponse response) {
        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase savedPurchase = purchaseCaptor.getValue();

        assertEquals(PURCHASE_ID.toString(), response.orderId());
        assertEquals(50.0, response.total());
        assertEquals("ABC123", response.codes().get(STUDENT_ID));
        assertEquals(BuyerType.TUTOR, savedPurchase.getBuyerType());
        assertEquals(USER_ID, savedPurchase.getBuyerId());
        assertEquals(TUTOR_ID, savedPurchase.getTutorId());
        assertEquals(BUFFET_ID, savedPurchase.getBuffetId());
        assertEquals(SLOT_ID, savedPurchase.getPickupSlotId());
        assertEquals(PICKUP_DATE, savedPurchase.getPickupDate());
        assertEquals(SUGGESTION_ID, savedPurchase.getSuggestionId());
        assertEquals(new BigDecimal("50.00"), savedPurchase.getTotalAmount());

        verify(purchaseValidator).validate(savedPurchase);
        verify(inventoryAvailabilityService).reserve(eq(BUFFET_ID), anyMap());
        verify(walletRepository).save(any(Billetera.class));
    }

    private void thenPurchaseIsCreatedForStudent(CreateAdvancePurchaseResponse response) {
        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase savedPurchase = purchaseCaptor.getValue();

        assertEquals(PURCHASE_ID.toString(), response.orderId());
        assertEquals(BuyerType.STUDENT, savedPurchase.getBuyerType());
        assertEquals(USER_ID, savedPurchase.getBuyerId());
        assertNull(savedPurchase.getTutorId());
        verifyNoInteractions(tutorRepository);
    }

    private void thenInventoryIsReserved() {
        verify(inventoryAvailabilityService).reserve(eq(BUFFET_ID), anyMap());
    }

    private void thenThrowsSecurityExceptionAndNoPurchase(CreateAdvancePurchaseRequest request) {
        assertThrows(SecurityException.class, () -> whenExecute(request));
        verifyNoInteractions(productRepository, walletRepository, inventoryAvailabilityService);
        verify(purchaseRepository, never()).save(any());
    }

    private void thenThrowsSecurityExceptionForStudentRole(CreateAdvancePurchaseRequest request) {
        assertThrows(SecurityException.class, () -> whenExecute(request));
        verifyNoInteractions(tutorRepository, productRepository, walletRepository);
        verify(purchaseRepository, never()).save(any());
    }

    private void thenThrowsInsufficientBalanceException(CreateAdvancePurchaseRequest request) {
        assertThrows(InsufficientBalanceException.class, () -> whenExecute(request));
        verify(inventoryAvailabilityService, never()).reserve(any(), anyMap());
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    private void thenThrowsIllegalArgumentExceptionBeforeProcessing(CreateAdvancePurchaseRequest req) {
        assertThrows(IllegalArgumentException.class, () -> whenExecute(req));
        verifyNoInteractions(usuarioRepository);
    }

    private void thenThrowsSecurityException(CreateAdvancePurchaseRequest request) {
        assertThrows(SecurityException.class, () -> whenExecute(request));
    }

    private void thenThrowsAlumnoNoEncontradoException(CreateAdvancePurchaseRequest request) {
        assertThrows(AlumnoNoEncontradoException.class, () -> whenExecute(request));
    }

    private void thenThrowsIllegalArgumentException(CreateAdvancePurchaseRequest request) {
        assertThrows(IllegalArgumentException.class, () -> whenExecute(request));
    }

    private void thenThrowsProductNotFoundException(CreateAdvancePurchaseRequest request) {
        assertThrows(ProductNotFoundException.class, () -> whenExecute(request));
    }

    private void thenThrowsIllegalStateException(CreateAdvancePurchaseRequest request) {
        assertThrows(IllegalStateException.class, () -> whenExecute(request));
    }

    private void thenResponseTotalIsZero(CreateAdvancePurchaseResponse response) {
        assertEquals(0.0, response.total());
    }

    private void thenOrderIdMatches(CreateAdvancePurchaseResponse response) {
        assertEquals(PURCHASE_ID.toString(), response.orderId());
    }

    private void thenMovementIsRegistered(MovimientoBilleteraRepository movementRepository) {
        verify(movementRepository).registrarMovimiento(
                any(),
                eq(TipoMovimientoBilletera.COMPRA),
                eq(new BigDecimal("-50.00")),
                eq("Compra anticipada en buffet"),
                eq(USER_ID)
        );
    }

    private void thenThrowsIllegalArgumentExceptionWithMessage(
            CreateAdvancePurchaseRequest request, String message) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> whenExecute(request)
        );
        assertEquals(message, exception.getMessage());
    }

    // --- Helpers: When ---

    private CreateAdvancePurchaseResponse whenExecute(CreateAdvancePurchaseRequest request) {
        return useCase.execute(request == null ? null : request.withCognitoSub(COGNITO_SUB));
    }

    // --- Helpers: Given ---

    private void givenValidContextForTutorPurchase() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();
        givenProductExists(PRODUCT_ID, DEFAULT_PRICE);
        givenWalletExists(STUDENT_ID, DEFAULT_BALANCE);
        givenWithdrawalCodeGenerated("ABC123");
        givenPurchaseIsSaved();
    }

    private void givenValidContextForStudentPurchase() {
        givenUserExists(USER_ID, RolUsuario.ALUMNO);
        givenUserIsStudent(STUDENT_ID);
        givenStudentExists(STUDENT_ID);
        givenSlotResolved();
        givenProductExists(PRODUCT_ID, DEFAULT_PRICE);
        givenWalletExists(STUDENT_ID, DEFAULT_BALANCE);
        givenWithdrawalCodeGenerated("ABC123");
        givenPurchaseIsSaved();
    }

    private void givenValidContextForStudentSchoolPurchase() {
        givenUserExists(USER_ID, RolUsuario.PADRE);
        givenTutorExists(USER_ID, TUTOR_ID, STUDENT_ID);
        givenStudentWithSchoolExists(STUDENT_ID, SCHOOL_ID);
        givenSchoolBuffetExists(SCHOOL_ID, BUFFET_ID);
        givenSlotResolved();
        givenProductExists(PRODUCT_ID, DEFAULT_PRICE);
        givenWalletExists(STUDENT_ID, DEFAULT_BALANCE);
        givenWithdrawalCodeGenerated("ABC123");
        givenPurchaseIsSaved();
    }

    private void givenUserExists(UUID userId, RolUsuario role) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        when(usuarioRepository.findByIdCognito(COGNITO_SUB)).thenReturn(Optional.of(user));
    }

    private void givenUserIsStudent(UUID studentId) {
        Usuario user = usuarioRepository.findByIdCognito(COGNITO_SUB).get();
        Alumno student = new Alumno();
        student.setId(studentId);
        user.setAlumno(student);
    }

    private void givenUserIsDifferentStudent() {
        givenUserIsStudent(UUID.randomUUID());
    }

    private void givenTutorExists(UUID userId, UUID tutorId, UUID childId) {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        Alumno child = new Alumno();
        child.setId(childId);
        tutor.addHijo(child);
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor));
    }

    private void givenTutorExistsForDifferentStudent() {
        givenTutorExists(USER_ID, TUTOR_ID, UUID.randomUUID());
    }

    private void givenStudentExists(UUID studentId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentExistsWithoutSchool() {
        givenStudentExists(STUDENT_ID);
    }

    private void givenStudentWithSchoolExists(UUID studentId, UUID schoolId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        Colegio school = new Colegio();
        school.setId(schoolId);
        Grado grade = new Grado();
        grade.setColegio(school);
        student.setGrado(grade);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenSlotResolved() {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setId(SLOT_ID);
        slot.setHoraInicio(LocalTime.of(10, 0));
        when(recessTimeSlotResolver.resolve(any(), any())).thenReturn(slot);
    }

    private void givenProductExists(UUID productId, BigDecimal price) {
        Producto product = new Producto();
        product.setId(productId);
        product.setPrecio(price);
        when(productRepository.findActiveByIdWithStock(productId, BUFFET_ID))
                .thenReturn(Optional.of(product));
    }

    private void givenWalletExists(UUID studentId, BigDecimal balance) {
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(balance);
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
    }

    private void givenWalletWithInsufficientBalance() {
        givenWalletExists(STUDENT_ID, new BigDecimal("10.00"));
    }

    private void givenSchoolBuffetExists(UUID schoolId, UUID buffetId) {
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        buffet.setActivo(true);
        when(buffetRepository.findByColegioId(schoolId)).thenReturn(List.of(buffet));
    }

    private void givenWithdrawalCodeGenerated(String code) {
        when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn(code);
    }

    private void givenPurchaseIsSaved() {
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase purchase = invocation.getArgument(0);
            purchase.setId(PURCHASE_ID);
            return purchase;
        });
    }

    private Promotion givenPromotionExists(
            UUID promotionId, 
            PromotionStatus status, 
            UUID buffetId, 
            LocalDateTime start, 
            LocalDateTime end) {
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setStatus(status);
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        promotion.setBuffet(buffet);
        promotion.setStartDate(start);
        promotion.setEndDate(end);
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
        return promotion;
    }

    private void givenPromotionWithInactiveProduct(Promotion promotion) {
        Producto prod = new Producto();
        prod.setId(PRODUCT_ID);
        promotion.setProducts(List.of(prod));
        promotion.setDiscountPercentage(new BigDecimal("10.00"));
        when(productRepository.findActiveByIdWithStock(PRODUCT_ID, BUFFET_ID))
                .thenReturn(Optional.empty());
    }

    private CreateAdvancePurchaseRequest givenRequest(UUID requestBuffetId, UUID suggestionId) {
        return givenRequest(
                requestBuffetId, 
                suggestionId, 
                PICKUP_DATE.toString(), 
                List.of(new CreateAdvancePurchaseRequest.ItemRequest(PRODUCT_ID, 2))
        );
    }

    private CreateAdvancePurchaseRequest givenRequest(
            UUID requestBuffetId, 
            UUID suggestionId, 
            String date, 
            List<CreateAdvancePurchaseRequest.ItemRequest> items) {
        CreateAdvancePurchaseRequest.OrderRequest orderRequest =
                new CreateAdvancePurchaseRequest.OrderRequest(
                        STUDENT_ID, 
                        USER_ID, 
                        BuyerType.TUTOR, 
                        requestBuffetId, 
                        date, 
                        RecessTime.FIRST_RECESS, 
                        items, 
                        suggestionId, 
                        TUTOR_ID
                );
        return new CreateAdvancePurchaseRequest(List.of(orderRequest));
    }

    private CreateAdvancePurchaseRequest givenRequestWithNoDate() {
        return givenRequest(
                BUFFET_ID, 
                null, 
                null, 
                List.of(new CreateAdvancePurchaseRequest.ItemRequest(PRODUCT_ID, 2))
        );
    }

    private CreateAdvancePurchaseRequest givenRequestWithInvalidDate() {
        return givenRequest(
                BUFFET_ID, 
                null, 
                "invalid-date", 
                List.of(new CreateAdvancePurchaseRequest.ItemRequest(PRODUCT_ID, 2))
        );
    }

    private CreateAdvancePurchaseRequest givenRequestWithNoItems() {
        return givenRequest(BUFFET_ID, null, PICKUP_DATE.toString(), List.of());
    }

    private CreateAdvancePurchaseRequest givenPromotionRequest() {
        return givenRequest(
                BUFFET_ID, 
                null, 
                PICKUP_DATE.toString(), 
                List.of(new CreateAdvancePurchaseRequest.ItemRequest(PROMOTION_ID, 1))
        );
    }

    private CreateAdvancePurchaseRequest givenRequestWithCognitoSub(
            UUID requestBuffetId, UUID suggestionId) {
        return givenRequest(requestBuffetId, suggestionId).withCognitoSub(COGNITO_SUB);
    }

    private CreateAdvancePurchase buildUseCaseWithoutMovementRepository() {
        return new CreateAdvancePurchase(
                purchaseValidator, alumnoRepository, productRepository, purchaseRepository,
                withdrawalCodeService, inventoryAvailabilityService, recessTimeSlotResolver,
                advancePurchaseRulesService, walletRepository, buffetRepository,
                usuarioRepository, tutorRepository, promotionRepository
        );
    }

    private CreateAdvancePurchase buildUseCaseWithMovementRepository(
            MovimientoBilleteraRepository movementRepository) {
        return new CreateAdvancePurchase(
                purchaseValidator, alumnoRepository, productRepository, purchaseRepository,
                withdrawalCodeService, inventoryAvailabilityService, recessTimeSlotResolver,
                advancePurchaseRulesService, walletRepository, buffetRepository,
                usuarioRepository, tutorRepository, promotionRepository, movementRepository
        );
    }
}
