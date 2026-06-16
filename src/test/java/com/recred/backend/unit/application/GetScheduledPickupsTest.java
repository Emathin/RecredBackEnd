package com.recred.backend.unit.application;

import com.recred.backend.application.GetScheduledPickups;
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

        when(purchaseRepository.findScheduledPickups(buffetId, fecha, estadoRetiro, franjaId, search))
                .thenReturn(List.of(purchase));

        List<Purchase> resultado = getScheduledPickups.execute(buffetId, fecha, estadoRetiro, franjaId, search);

        assertEquals(1, resultado.size());
        assertEquals(purchase, resultado.get(0));
        verify(purchaseRepository, times(1))
                .findScheduledPickups(buffetId, fecha, estadoRetiro, franjaId, search);
    }
}
