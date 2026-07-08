package com.recred.backend.unit.application;

import com.recred.backend.application.GetTutorHistory;
import com.recred.backend.application.GetTutorHistoryRequest;
import com.recred.backend.application.GetTutorHistoryResponse;
import com.recred.backend.application.GetHistoryByStudentResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTutorHistoryTest {

    private static final String COGNITO_SUBJECT = "cognito-sub";
    private static final UUID TUTOR_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PURCHASE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String PRODUCT_NAME = "Jugo";

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private GetTutorHistory useCase;

    @Test
    void executeShouldReturnPurchasesWhenTutorIdIsProvided() {
        givenPurchasesFound();

        GetTutorHistoryResponse result = whenExecuteWithTutorId();

        thenReturnsExpectedPurchases(result);
        thenRepositoryWasQueriedByTutorId();
    }

    @Test
    void executeShouldReturnPurchasesWhenCognitoSubjectIsProvided() {
        givenTutorFound();
        givenPurchasesFound();

        GetTutorHistoryResponse result = whenExecuteWithCognitoSubject();

        thenReturnsExpectedPurchases(result);
        thenTutorWasFoundByCognitoSubject();
        thenRepositoryWasQueriedByTutorId();
    }

    @Test
    void executeShouldThrowExceptionWhenTutorNotFoundByCognitoSubject() {
        givenTutorNotFound();

        RuntimeException exception = assertThrows(RuntimeException.class, this::whenExecuteWithCognitoSubject);

        thenExceptionContainsMessage(exception, "Tutor no encontrado para el usuario autenticado");
        thenTutorWasFoundByCognitoSubject();
    }

    @Test
    void executeShouldMapPurchaseFieldsAndItems() {
        givenPurchasesFound(List.of(buildPurchase(buildProduct())));

        GetTutorHistoryResponse result = whenExecuteWithTutorId();

        thenPurchaseAndItemAreMappedCorrectly(result);
    }

    private void thenReturnsExpectedPurchases(GetTutorHistoryResponse result) {
        assertEquals(1, result.purchases().size());
        assertEquals(PURCHASE_ID, result.purchases().get(0).id());
    }

    private void thenRepositoryWasQueriedByTutorId() {
        verify(purchaseRepository).findHistorialByTutor(TUTOR_ID);
    }

    private void thenTutorWasFoundByCognitoSubject() {
        verify(tutorRepository).findByUsuarioIdCognito(COGNITO_SUBJECT);
    }

    private void thenExceptionContainsMessage(RuntimeException exception, String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void thenPurchaseAndItemAreMappedCorrectly(GetTutorHistoryResponse result) {
        GetHistoryByStudentResponse response = result.purchases().get(0);
        GetHistoryByStudentResponse.Item item = response.items().get(0);
        
        assertEquals(STUDENT_ID, response.studentId());
        assertEquals("ENTREGADO", response.status());
        assertEquals("Entregado", response.statusLabel());
        assertEquals("CREDITOS", response.paymentMethod());
        assertEquals("PRESENCIAL", response.tipo());
        assertEquals("RETIRADO", response.withdrawalStatus());
        
        assertEquals(PRODUCT_ID, item.productId());
        assertEquals(PRODUCT_NAME, item.nombre());
        assertEquals(3, item.quantity());
        assertEquals(new BigDecimal("400.00"), item.precio());
    }

    private Purchase buildSimplePurchase() {
        Purchase purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        return purchase;
    }

    private Purchase buildPurchase(Producto product) {
        Alumno alumno = new Alumno();
        alumno.setId(STUDENT_ID);
        
        Purchase purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setStudent(alumno);
        purchase.setTotalAmount(new BigDecimal("1200.00"));
        purchase.setStatus(EstadoCompra.ENTREGADO);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 4, 12, 0));
        purchase.setTipo(TipoCompra.PRESENCIAL);
        purchase.setWithdrawalCode("T789");
        purchase.setPickupSlotDescription("Mediodia");
        purchase.setPickupDate(LocalDate.of(2026, 7, 4));
        purchase.setPickupSlotStartTime(LocalTime.of(12, 0));
        purchase.setWithdrawalStatus(EstadoRetiro.RETIRADO);
        purchase.setItems(Map.of(product, 3));
        return purchase;
    }

    private Producto buildProduct() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setNombre(PRODUCT_NAME);
        product.setPrecio(new BigDecimal("400.00"));
        product.setRequierePreparacion(false);
        product.setCupoMaximoDiario(30);
        return product;
    }

    private void givenTutorFound() {
        Tutor tutor = new Tutor();
        tutor.setId(TUTOR_ID);
        when(tutorRepository.findByUsuarioIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(tutor));
    }

    private void givenTutorNotFound() {
        when(tutorRepository.findByUsuarioIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenPurchasesFound() {
        givenPurchasesFound(List.of(buildSimplePurchase()));
    }

    private void givenPurchasesFound(List<Purchase> purchases) {
        when(purchaseRepository.findHistorialByTutor(TUTOR_ID)).thenReturn(purchases);
    }

    private GetTutorHistoryResponse whenExecuteWithTutorId() {
        return useCase.execute(GetTutorHistoryRequest.byTutorId(TUTOR_ID));
    }

    private GetTutorHistoryResponse whenExecuteWithCognitoSubject() {
        return useCase.execute(GetTutorHistoryRequest.byCognitoSubject(COGNITO_SUBJECT));
    }
}
