package com.recred.backend.unit.application;

import com.recred.backend.application.GetScheduledPickups;
import com.recred.backend.application.GetScheduledPickupsRequest;
import com.recred.backend.application.GetScheduledPickupsResponse;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
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
        UUID franjaId = UUID.randomUUID();
        String search = "test";
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());

        when(purchaseRepository.findScheduledPickups(buffetId, fecha, estadoRetiro, franjaId, search))
                .thenReturn(List.of(purchase));

        GetScheduledPickupsRequest request = new GetScheduledPickupsRequest(
                buffetId, fecha, estadoRetiro, franjaId, search);

        List<GetScheduledPickupsResponse> resultado = getScheduledPickups.execute(request);

        assertEquals(1, resultado.size());
        assertEquals(purchase.getId(), resultado.get(0).id());
        verify(purchaseRepository, times(1))
                .findScheduledPickups(buffetId, fecha, estadoRetiro, franjaId, search);
    }

    @Test
    void execute_CuandoNoHayRetirosProgramados_RetornaListaVacia() {
        UUID buffetId = UUID.randomUUID();
        LocalDate fecha = LocalDate.now();
        EstadoRetiro estadoRetiro = EstadoRetiro.PROGRAMADO;
        UUID franjaId = UUID.randomUUID();
        String search = "test";

        when(purchaseRepository.findScheduledPickups(buffetId, fecha, estadoRetiro, franjaId, search))
                .thenReturn(List.of());

        GetScheduledPickupsRequest request = new GetScheduledPickupsRequest(
                buffetId, fecha, estadoRetiro, franjaId, search);

        List<GetScheduledPickupsResponse> resultado = getScheduledPickups.execute(request);

        assertEquals(0, resultado.size());
        verify(purchaseRepository, times(1))
                .findScheduledPickups(buffetId, fecha, estadoRetiro, franjaId, search);
    }
}
