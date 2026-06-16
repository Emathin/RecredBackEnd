package com.recred.backend.unit.application;

import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
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
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.persistence.SpringDataBuffetRepository;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAdvancePurchaseUseCaseTest {

    @Mock
    private PurchaseValidator purchaseValidator;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private WithdrawalCodeService withdrawalCodeService;
    @Mock
    private InventoryAvailabilityService inventoryAvailabilityService;
    @Mock
    private RecessTimeSlotResolver recessTimeSlotResolver;
    @Mock
    private AdvancePurchaseRulesService advancePurchaseRulesService;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private SpringDataBuffetRepository buffetRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private CreateAdvancePurchase useCase;

    private UUID studentId;
    private UUID tutorId;
    private UUID userId;
    private UUID productId;
    private UUID buffetId;
    private UUID purchaseId;
    private UUID slotId;
    private UUID suggestionId;
    private Alumno student;
    private Tutor tutor;
    private Usuario user;
    private Producto product;
    private Billetera wallet;
    private FranjaHoraria slot;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        tutorId = UUID.randomUUID();
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        purchaseId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        suggestionId = UUID.randomUUID();

        student = new Alumno();
        student.setId(studentId);

        tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.addHijo(student);

        user = new Usuario();
        user.setId(userId);
        user.setRol(RolUsuario.PADRE);

        product = new Producto();
        product.setId(productId);
        product.setNombre("Jugo");
        product.setPrecio(new BigDecimal("25.00"));

        wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("100.00"));

        slot = new FranjaHoraria();
        slot.setId(slotId);
        slot.setDescripcion("Primer recreo");
        slot.setHoraInicio(LocalTime.of(10, 0));

        lenient().when(usuarioRepository.findByIdCognito("sub")).thenReturn(Optional.of(user));
        lenient().when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        lenient().when(recessTimeSlotResolver.resolve(student, RecessTime.FIRST_RECESS))
                .thenReturn(slot);
        lenient().when(productRepository.findActiveByIdWithStock(productId, buffetId))
                .thenReturn(Optional.of(product));
        lenient().when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        lenient().when(withdrawalCodeService.generateCode(any(Purchase.class)))
                .thenReturn("ABC123");
        lenient().when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase purchase = invocation.getArgument(0);
            purchase.setId(purchaseId);
            return purchase;
        });
    }

    @Test
    void execute_WhenTutorBuysForOwnChild_ShouldCreateAdvancePurchase() {
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor));

        AdvancePurchaseResponse response = useCase.execute(request(buffetId, suggestionId), "sub");

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase savedPurchase = purchaseCaptor.getValue();

        assertEquals(purchaseId.toString(), response.orderId());
        assertEquals(50.0, response.total());
        assertEquals("ABC123", response.codes().get(studentId));
        assertEquals(BuyerType.TUTOR, savedPurchase.getBuyerType());
        assertEquals(userId, savedPurchase.getBuyerId());
        assertEquals(tutorId, savedPurchase.getTutorId());
        assertEquals(buffetId, savedPurchase.getBuffetId());
        assertEquals(slotId, savedPurchase.getPickupSlotId());
        assertEquals(LocalDate.parse("2026-06-20"), savedPurchase.getPickupDate());
        assertEquals(suggestionId, savedPurchase.getSuggestionId());
        assertEquals(new BigDecimal("50.00"), savedPurchase.getTotalAmount());
        assertEquals(new BigDecimal("50.00"), wallet.getSaldoCreditos());

        verify(purchaseValidator).validate(savedPurchase);
        verify(advancePurchaseRulesService)
                .validate(eq(buffetId), eq(slot), eq(LocalDate.parse("2026-06-20")), anyMap());
        verify(inventoryAvailabilityService).reserve(eq(buffetId), anyMap());
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_WhenStudentBuysForThemself_ShouldCreateStudentPurchase() {
        user.setRol(RolUsuario.ALUMNO);
        user.setAlumno(student);

        AdvancePurchaseResponse response = useCase.execute(request(buffetId, null), "sub");

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        Purchase savedPurchase = purchaseCaptor.getValue();

        assertEquals(purchaseId.toString(), response.orderId());
        assertEquals(BuyerType.STUDENT, savedPurchase.getBuyerType());
        assertEquals(userId, savedPurchase.getBuyerId());
        assertNull(savedPurchase.getTutorId());
        verifyNoInteractions(tutorRepository);
    }

    @Test
    void execute_WhenBuffetIdIsMissing_ShouldUseStudentSchoolBuffet() {
        UUID schoolId = UUID.randomUUID();
        UUID defaultBuffetId = UUID.randomUUID();
        student.setGrado(grade(schoolId));

        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(defaultBuffetId);

        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor));
        when(buffetRepository.findFirstByColegioEntityIdAndActivoTrue(schoolId))
                .thenReturn(Optional.of(buffet));
        when(productRepository.findActiveByIdWithStock(productId, defaultBuffetId))
                .thenReturn(Optional.of(product));

        useCase.execute(request(null, null), "sub");

        verify(inventoryAvailabilityService).reserve(eq(defaultBuffetId), anyMap());
    }

    @Test
    void execute_WhenTutorDoesNotOwnStudent_ShouldThrowSecurityException() {
        Tutor otherTutor = new Tutor();
        otherTutor.setId(UUID.randomUUID());
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(otherTutor));

        assertThrows(SecurityException.class, () -> useCase.execute(request(buffetId, null), "sub"));

        verifyNoInteractions(productRepository, walletRepository, inventoryAvailabilityService);
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenStudentBuysForAnotherStudent_ShouldThrowSecurityException() {
        Alumno otherStudent = new Alumno();
        otherStudent.setId(UUID.randomUUID());
        user.setRol(RolUsuario.ALUMNO);
        user.setAlumno(otherStudent);

        assertThrows(SecurityException.class, () -> useCase.execute(request(buffetId, null), "sub"));

        verifyNoInteractions(tutorRepository, productRepository, walletRepository);
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenBalanceIsInsufficient_ShouldThrowException() {
        wallet.setSaldoCreditos(new BigDecimal("10.00"));
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor));

        assertThrows(InsufficientBalanceException.class,
                () -> useCase.execute(request(buffetId, null), "sub"));

        verify(inventoryAvailabilityService, never()).reserve(any(), anyMap());
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenOrdersAreEmpty_ShouldThrowException() {
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request, "sub"));

        verifyNoInteractions(usuarioRepository);
    }

    private AdvancePurchaseRequest request(UUID requestBuffetId, UUID requestSuggestionId) {
        AdvancePurchaseRequest.ItemRequest itemRequest =
                new AdvancePurchaseRequest.ItemRequest(productId, 2);
        AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                studentId,
                userId,
                BuyerType.TUTOR,
                requestBuffetId,
                "2026-06-20",
                RecessTime.FIRST_RECESS,
                List.of(itemRequest),
                requestSuggestionId,
                tutorId
        );
        return new AdvancePurchaseRequest(List.of(orderRequest));
    }

    private Grado grade(UUID schoolId) {
        Colegio school = new Colegio();
        school.setId(schoolId);

        Grado grade = new Grado();
        grade.setColegio(school);
        return grade;
    }
}
