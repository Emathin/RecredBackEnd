// C:/Users/emman/OneDrive/Desktop/TPI/recred-backend/src/test/java/com/recred/backend/application/CreateAdvancePurchaseUseCaseTest.java
package com.recred.backend.application;

import com.recred.backend.application.usecases.CreateAdvancePurchaseUseCase;
import com.recred.backend.domain.exceptions.DietaryRestrictionException;
import com.recred.backend.domain.models.*;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.PurchaseValidator;
import com.recred.backend.domain.services.WithdrawalCodeService;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private CreateAdvancePurchaseUseCase createAdvancePurchaseUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        createAdvancePurchaseUseCase = new CreateAdvancePurchaseUseCase(
                purchaseValidator,
                alumnoRepository,
                productRepository,
                purchaseRepository,
                withdrawalCodeService
        );
    }

    @Test
    void execute_shouldReturnPendingStatusAndWithdrawalCodes_whenPurchaseIsSuccessful() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest(productId, 1);
        AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                studentId, UUID.randomUUID(), BuyerType.TUTOR, "2024-05-23", RecessTime.FIRST_RECESS, List.of(itemRequest)
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

        Purchase purchase = new Purchase(student, items, null);
        purchase.setId(UUID.randomUUID()); // This line fixes the bug.

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);
        when(withdrawalCodeService.generateCode(any(Purchase.class))).thenReturn("test-code");

        // When
        AdvancePurchaseResponse response = createAdvancePurchaseUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals(EstadoCompra.PENDIENTE, response.status().status());
        assertEquals(10.0, response.total());
        assertTrue(response.codes().containsKey(studentId));
        assertEquals("test-code", response.codes().get(studentId));
    }

    @Test
    void execute_shouldThrowDietaryRestrictionException_whenValidationFails() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest(productId, 1);
        AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                studentId, UUID.randomUUID(), BuyerType.TUTOR, "2024-05-23", RecessTime.FIRST_RECESS, List.of(itemRequest)
        );
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(orderRequest));

        Alumno student = new Alumno();
        student.setId(studentId);

        Producto product = new Producto();
        product.setId(productId);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        doThrow(new DietaryRestrictionException("Dietary restriction violated"))
                .when(purchaseValidator).validate(any(Purchase.class));

        // When & Then
        assertThrows(DietaryRestrictionException.class, () -> {
            createAdvancePurchaseUseCase.execute(request);
        });
    }
}
