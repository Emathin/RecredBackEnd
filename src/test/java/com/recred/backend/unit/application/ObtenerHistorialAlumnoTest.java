package com.recred.backend.unit.application;

import com.recred.backend.application.GetHistoryByStudent;
import com.recred.backend.application.GetHistoryByStudentRequest;
import com.recred.backend.application.GetHistoryByStudentResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObtenerHistorialAlumnoTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private GetHistoryByStudent getHistoryByStudent;

    @Test
    void dadoAlumnoConCompras_cuandoSeEjecuta_entoncesRetornaListaDeCompras() {
        UUID alumnoId = UUID.randomUUID();
        Purchase compra1 = new Purchase();
        compra1.setId(UUID.randomUUID());
        Purchase compra2 = new Purchase();
        compra2.setId(UUID.randomUUID());
        List<Purchase> comprasMock = List.of(compra1, compra2);

        when(purchaseRepository.findHistorialByAlumno(alumnoId)).thenReturn(comprasMock);

        List<GetHistoryByStudentResponse> resultado = getHistoryByStudent.execute(
                new GetHistoryByStudentRequest(alumnoId)
        );

        assertEquals(2, resultado.size());
        assertEquals(compra1.getId(), resultado.get(0).id());
        assertEquals(compra2.getId(), resultado.get(1).id());
        verify(purchaseRepository, times(1)).findHistorialByAlumno(alumnoId);
    }

    @Test
    void dadoAlumnoSinCompras_cuandoSeEjecuta_entoncesRetornaListaVacia() {
        UUID alumnoId = UUID.randomUUID();
        when(purchaseRepository.findHistorialByAlumno(alumnoId)).thenReturn(List.of());

        List<GetHistoryByStudentResponse> resultado = getHistoryByStudent.execute(
                new GetHistoryByStudentRequest(alumnoId)
        );

        assertEquals(0, resultado.size());
        verify(purchaseRepository, times(1)).findHistorialByAlumno(alumnoId);
    }

    @Test
    void dadoCompraCompleta_cuandoSeEjecuta_entoncesMapeaCamposEItems() {
        UUID alumnoId = UUID.randomUUID();
        Producto producto = product();
        Purchase purchase = purchase(alumnoId, producto);

        when(purchaseRepository.findHistorialByAlumno(alumnoId)).thenReturn(List.of(purchase));

        List<GetHistoryByStudentResponse> resultado = getHistoryByStudent.execute(
                new GetHistoryByStudentRequest(alumnoId)
        );

        GetHistoryByStudentResponse response = resultado.get(0);
        GetHistoryByStudentResponse.Item item = response.items().get(0);
        assertEquals(alumnoId, response.studentId());
        assertEquals("LISTO", response.status());
        assertEquals("Listo para retirar", response.statusLabel());
        assertEquals("CREDITOS", response.paymentMethod());
        assertEquals("ANTICIPADA", response.tipo());
        assertEquals("R123", response.withdrawalCode());
        assertEquals("Recreo largo", response.pickupSlotDescription());
        assertEquals(LocalDate.of(2026, 7, 6), response.pickupDate());
        assertEquals(LocalTime.of(10, 30), response.pickupSlotStartTime());
        assertEquals("LISTO", response.withdrawalStatus());
        assertEquals(producto.getId(), item.productId());
        assertEquals("Agua", item.nombre());
        assertEquals(2, item.quantity());
        assertEquals(new BigDecimal("500.00"), item.precio());
        assertEquals(true, item.requierePreparacion());
        assertEquals(20, item.cupoMaximoDiario());
        assertEquals(producto.getPromocionId(), item.promocionId());
        assertEquals("Promo agua", item.promocionNombre());
    }

    private Purchase purchase(UUID alumnoId, Producto producto) {
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setStudent(alumno);
        purchase.setTotalAmount(new BigDecimal("1000.00"));
        purchase.setStatus(EstadoCompra.LISTO);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 4, 9, 15));
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setWithdrawalCode("R123");
        purchase.setPickupSlotDescription("Recreo largo");
        purchase.setPickupDate(LocalDate.of(2026, 7, 6));
        purchase.setPickupSlotStartTime(LocalTime.of(10, 30));
        purchase.setWithdrawalStatus(EstadoRetiro.LISTO);
        purchase.setItems(Map.of(producto, 2));
        return purchase;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Agua");
        product.setPrecio(new BigDecimal("500.00"));
        product.setRequierePreparacion(true);
        product.setCupoMaximoDiario(20);
        product.setPromocionId(UUID.randomUUID());
        product.setPromocionNombre("Promo agua");
        return product;
    }
}
