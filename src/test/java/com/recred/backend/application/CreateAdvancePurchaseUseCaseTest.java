package com.recred.backend.application;

import com.recred.backend.domain.exceptions.DietaryRestrictionException;
import com.recred.backend.domain.models.*;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.AdvancePurchaseRulesService;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.RecessTimeSlotResolver;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private CreateAdvancePurchase createAdvancePurchase;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        createAdvancePurchase = new CreateAdvancePurchase(
                purchaseValidator,
                alumnoRepository,
                productRepository,
                purchaseRepository,
                withdrawalCodeService,
                inventoryAvailabilityService,
                recessTimeSlotResolver,
                advancePurchaseRulesService
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void execute_shouldReturnPendingStatusAndWithdrawalCodes_whenPurchaseIsSuccessful() {

        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest
                (productId, 1);
        AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                studentId, UUID.randomUUID(), BuyerType.TUTOR, buffetId, "2024-05-23",
                RecessTime.FIRST_RECESS, List.of(itemRequest)
        );
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(orderRequest));

        Alumno student = new Alumno();
        student.setId(studentId);

        Producto product = new Producto();
        product.setId(productId);
        product.setPrecio(BigDecimal.TEN);
        product.setRequierePreparacion(true);

        Map<Producto, Integer> items = new HashMap<>();
        items.put(product, 1);

        Purchase purchase = new Purchase(student, items, null, studentId, BuyerType.STUDENT);
        purchase.setId(UUID.randomUUID());
        FranjaHoraria slot = timeSlot();

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(recessTimeSlotResolver.resolve(student, RecessTime.FIRST_RECESS)).thenReturn(slot);
        when(productRepository.findActiveByIdWithStock(productId, buffetId))
                .thenReturn(Optional.of(product));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);
        when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn("test-code");

        AdvancePurchaseResponse response = createAdvancePurchase.execute(request);

        assertNotNull(response);
        assertEquals(EstadoCompra.PENDIENTE, response.status().status());
        assertEquals(10.0, response.total());
        assertTrue(response.codes().containsKey(studentId));
        assertEquals("test-code", response.codes().get(studentId));
        verify(inventoryAvailabilityService).reserve(eq(buffetId), anyMap());
        verify(advancePurchaseRulesService).validate(eq(buffetId), eq(slot), any(), anyMap());
    }

    @Test
    void execute_shouldThrowDietaryRestrictionException_whenValidationFails() {

        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest
                (productId, 1);
        AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                studentId, UUID.randomUUID(), BuyerType.TUTOR, buffetId, "2024-05-23",
                RecessTime.FIRST_RECESS, List.of(itemRequest)
        );
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(orderRequest));

        Alumno student = new Alumno();
        student.setId(studentId);

        Producto product = new Producto();
        product.setId(productId);
        FranjaHoraria slot = timeSlot();

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(recessTimeSlotResolver.resolve(student, RecessTime.FIRST_RECESS)).thenReturn(slot);
        when(productRepository.findActiveByIdWithStock(productId, buffetId))
                .thenReturn(Optional.of(product));

        doThrow(new DietaryRestrictionException("Dietary restriction violated"))
                .when(purchaseValidator).validate(any(Purchase.class));

        assertThrows(DietaryRestrictionException.class, () -> createAdvancePurchase.execute(request));
    }

    @Test
    void execute_shouldProcessAllOrders() {
        UUID firstStudentId = UUID.randomUUID();
        UUID secondStudentId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(
                order(firstStudentId, firstProductId, buffetId),
                order(secondStudentId, secondProductId, buffetId)
        ));
        Alumno firstStudent = student(firstStudentId);
        Alumno secondStudent = student(secondStudentId);
        Producto firstProduct = product(firstProductId, "10.00");
        Producto secondProduct = product(secondProductId, "15.00");
        Purchase firstPurchase = savedPurchase(firstStudent, firstProduct);
        Purchase secondPurchase = savedPurchase(secondStudent, secondProduct);

        when(alumnoRepository.findById(firstStudentId)).thenReturn(Optional.of(firstStudent));
        when(alumnoRepository.findById(secondStudentId)).thenReturn(Optional.of(secondStudent));
        when(productRepository.findActiveByIdWithStock(firstProductId, buffetId))
                .thenReturn(Optional.of(firstProduct));
        when(productRepository.findActiveByIdWithStock(secondProductId, buffetId))
                .thenReturn(Optional.of(secondProduct));
        when(recessTimeSlotResolver.resolve(any(Alumno.class), eq(RecessTime.FIRST_RECESS)))
                .thenReturn(timeSlot());
        when(purchaseRepository.save(any(Purchase.class)))
                .thenReturn(firstPurchase)
                .thenReturn(secondPurchase);
        when(withdrawalCodeService.generateCode(any(Purchase.class)))
                .thenReturn("first-code")
                .thenReturn("second-code");

        AdvancePurchaseResponse response = createAdvancePurchase.execute(request);

        assertEquals(25.0, response.total());
        assertEquals("first-code", response.codes().get(firstStudentId));
        assertEquals("second-code", response.codes().get(secondStudentId));
        verify(inventoryAvailabilityService, times(2)).reserve(eq(buffetId), anyMap());
        verify(purchaseRepository, times(2)).save(any(Purchase.class));
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldThrowException() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(
                new AdvancePurchaseRequest.OrderRequest(
                        studentId,
                        UUID.randomUUID(),
                        BuyerType.TUTOR,
                        null,
                        "2024-05-23",
                        RecessTime.FIRST_RECESS,
                        List.of(new AdvancePurchaseRequest.ItemRequest(productId, 1))
                )
        ));

        assertThrows(IllegalArgumentException.class,
                () -> createAdvancePurchase.execute(request));
    }

    private AdvancePurchaseRequest.OrderRequest order(
            UUID studentId,
            UUID productId,
            UUID buffetId
    ) {
        return new AdvancePurchaseRequest.OrderRequest(
                studentId,
                UUID.randomUUID(),
                BuyerType.TUTOR,
                buffetId,
                "2024-05-23",
                RecessTime.FIRST_RECESS,
                List.of(new AdvancePurchaseRequest.ItemRequest(productId, 1))
        );
    }

    private Alumno student(UUID studentId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }

    private Producto product(UUID productId, String price) {
        Producto product = new Producto();
        product.setId(productId);
        product.setPrecio(new BigDecimal(price));
        product.setRequierePreparacion(true);
        return product;
    }

    private Purchase savedPurchase(Alumno student, Producto product) {
        Map<Producto, Integer> items = new HashMap<>();
        items.put(product, 1);
        Purchase purchase = new Purchase(student, items, null, student.getId(), BuyerType.TUTOR);
        purchase.setId(UUID.randomUUID());
        return purchase;
    }

    private FranjaHoraria timeSlot() {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setId(UUID.randomUUID());
        slot.setDescripcion("Primer recreo");
        slot.setHoraInicio(LocalTime.of(10, 0));
        slot.setHoraFin(LocalTime.of(10, 20));
        return slot;
    }
}
