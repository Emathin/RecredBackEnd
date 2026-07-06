package com.recred.backend.unit.application;

import com.recred.backend.application.GetTutorHistory;
import com.recred.backend.application.GetTutorHistoryRequest;
import com.recred.backend.application.GetTutorHistoryResponse;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetTutorHistoryTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private GetTutorHistory obtenerHistorialTutor;

    @Test
    void dadoTutorConComprasDeHijos_cuandoSeEjecuta_entoncesRetornaListaDeCompras() {
        UUID tutorId = UUID.randomUUID();
        Purchase compra = new Purchase();
        compra.setId(UUID.randomUUID());
        List<Purchase> comprasMock = List.of(compra);

        when(purchaseRepository.findHistorialByTutor(tutorId)).thenReturn(comprasMock);

        GetTutorHistoryResponse resultado = obtenerHistorialTutor.execute(
                GetTutorHistoryRequest.byTutorId(tutorId));

        assertEquals(1, resultado.purchases().size());
        verify(purchaseRepository, times(1)).findHistorialByTutor(tutorId);
    }

    @Test
    void executeByCognitoSubject_CuandoTutorExiste_RetornaHistorial() {
        String subject = "cognito-sub";
        UUID tutorId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        List<Purchase> purchases = List.of(purchase);
        when(tutorRepository.findByUsuarioIdCognito(subject)).thenReturn(Optional.of(tutor));
        when(purchaseRepository.findHistorialByTutor(tutorId)).thenReturn(purchases);

        GetTutorHistoryResponse result = obtenerHistorialTutor.execute(
                GetTutorHistoryRequest.byCognitoSubject(subject));

        assertEquals(purchase.getId(), result.purchases().get(0).id());
        verify(purchaseRepository).findHistorialByTutor(tutorId);
    }

    @Test
    void executeByCognitoSubject_CuandoTutorNoExiste_LanzaExcepcion() {
        String subject = "cognito-sub";
        when(tutorRepository.findByUsuarioIdCognito(subject)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> obtenerHistorialTutor.execute(
                        GetTutorHistoryRequest.byCognitoSubject(subject)));

        assertEquals("Tutor no encontrado para el usuario autenticado", exception.getMessage());
    }

    @Test
    void executeShouldMapPurchaseFieldsAndItems() {
        UUID tutorId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        Producto product = product();
        Purchase purchase = purchase(alumnoId, product);
        when(purchaseRepository.findHistorialByTutor(tutorId)).thenReturn(List.of(purchase));

        GetTutorHistoryResponse result = obtenerHistorialTutor.execute(
                GetTutorHistoryRequest.byTutorId(tutorId)
        );

        var response = result.purchases().get(0);
        var item = response.items().get(0);
        assertEquals(alumnoId, response.studentId());
        assertEquals("ENTREGADO", response.status());
        assertEquals("Entregado", response.statusLabel());
        assertEquals("CREDITOS", response.paymentMethod());
        assertEquals("PRESENCIAL", response.tipo());
        assertEquals("RETIRADO", response.withdrawalStatus());
        assertEquals(product.getId(), item.productId());
        assertEquals("Jugo", item.nombre());
        assertEquals(3, item.quantity());
        assertEquals(new BigDecimal("400.00"), item.precio());
    }

    private Purchase purchase(UUID alumnoId, Producto product) {
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
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

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Jugo");
        product.setPrecio(new BigDecimal("400.00"));
        product.setRequierePreparacion(false);
        product.setCupoMaximoDiario(30);
        return product;
    }
}
