package com.recred.backend.unit.application;

import com.recred.backend.application.GetScheduledPickups;
import com.recred.backend.application.GetScheduledPickupsRequest;
import com.recred.backend.application.GetScheduledPickupsResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetScheduledPickupsTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private GetScheduledPickups getScheduledPickups;

    @Test
    void execute_deberiaDelegarAlRepositorioCorrectamente() {
        UUID buffetId = UUID.randomUUID();
        LocalDate fecha = LocalDate.now();
        EstadoRetiro estadoRetiro = EstadoRetiro.PROGRAMADO;
        EstadoCompra status = EstadoCompra.PENDIENTE;
        UUID franjaId = UUID.randomUUID();
        String search = "test";
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());

        when(purchaseRepository.findScheduledPickups(
                buffetId,
                fecha,
                estadoRetiro,
                status,
                franjaId,
                search))
                .thenReturn(List.of(purchase));

        GetScheduledPickupsRequest request = new GetScheduledPickupsRequest(
                buffetId, fecha, estadoRetiro, status, franjaId, search);

        List<GetScheduledPickupsResponse> resultado = getScheduledPickups.execute(request);

        assertEquals(1, resultado.size());
        assertEquals(purchase.getId(), resultado.get(0).id());
        verify(purchaseRepository, times(1))
                .findScheduledPickups(buffetId, fecha, estadoRetiro, status, franjaId, search);
    }

    @Test
    void execute_CuandoNoHayRetirosProgramados_RetornaListaVacia() {
        UUID buffetId = UUID.randomUUID();
        LocalDate fecha = LocalDate.now();
        EstadoRetiro estadoRetiro = EstadoRetiro.PROGRAMADO;
        EstadoCompra status = EstadoCompra.LISTO;
        UUID franjaId = UUID.randomUUID();
        String search = "test";

        when(purchaseRepository.findScheduledPickups(
                buffetId,
                fecha,
                estadoRetiro,
                status,
                franjaId,
                search))
                .thenReturn(List.of());

        GetScheduledPickupsRequest request = new GetScheduledPickupsRequest(
                buffetId, fecha, estadoRetiro, status, franjaId, search);

        List<GetScheduledPickupsResponse> resultado = getScheduledPickups.execute(request);

        assertEquals(0, resultado.size());
        verify(purchaseRepository, times(1))
                .findScheduledPickups(buffetId, fecha, estadoRetiro, status, franjaId, search);
    }

    @Test
    void execute_CuandoCompraTieneAlumnoEItems_MapeaDetalleCompleto() {
        UUID buffetId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 7, 8);
        UUID franjaId = UUID.randomUUID();
        UUID pickupSlotId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Producto product = product();
        Purchase purchase = new Purchase();
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setNombre("Ana");
        student.setApellido("Perez");
        purchase.setId(UUID.randomUUID());
        purchase.setStudent(student);
        purchase.setTotalAmount(new BigDecimal("1800.00"));
        purchase.setStatus(EstadoCompra.EN_PREPARACION);
        purchase.setWithdrawalStatus(EstadoRetiro.EN_PREPARACION);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 4, 12, 30));
        purchase.setWithdrawalCode("S111");
        purchase.setPickupSlotId(pickupSlotId);
        purchase.setPickupSlotDescription("Segundo recreo");
        purchase.setPickupDate(fecha);
        purchase.setItems(Map.of(product, 2));
        when(purchaseRepository.findScheduledPickups(
                buffetId,
                fecha,
                EstadoRetiro.EN_PREPARACION,
                EstadoCompra.EN_PREPARACION,
                franjaId,
                "Ana"))
                .thenReturn(List.of(purchase));

        List<GetScheduledPickupsResponse> result = getScheduledPickups.execute(
                new GetScheduledPickupsRequest(
                        buffetId,
                        fecha,
                        EstadoRetiro.EN_PREPARACION,
                        EstadoCompra.EN_PREPARACION,
                        franjaId,
                        "Ana"
                )
        );

        GetScheduledPickupsResponse response = result.get(0);
        GetScheduledPickupsResponse.PurchaseItemResponse item = response.items().get(0);
        assertEquals(studentId, response.studentId());
        assertEquals("Ana Perez", response.studentName());
        assertEquals("EN_PREPARACION", response.status());
        assertEquals("En preparacion", response.statusLabel());
        assertEquals("EN_PREPARACION", response.withdrawalStatus());
        assertEquals("CREDITOS", response.paymentMethod());
        assertEquals("S111", response.withdrawalCode());
        assertEquals(pickupSlotId, response.pickupSlotId());
        assertEquals("Segundo recreo", response.pickupSlotDescription());
        assertEquals(fecha, response.pickupDate());
        assertEquals(product.getId(), item.productId());
        assertEquals("Muffin", item.name());
        assertEquals(2, item.quantity());
        assertEquals(new BigDecimal("900.00"), item.price());
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Muffin");
        product.setPrecio(new BigDecimal("900.00"));
        return product;
    }
}
