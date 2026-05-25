package com.recred.backend.application;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchedulePickupTest {

    @Mock
    private ValidadorHorarioService timeValidator;

    @Mock
    private ValidacionRestriccionesService healthValidator;

    @InjectMocks
    private SchedulePickup schedulePickup;

    @Test
    void execute_EnsamblaFechaYHoraYLlamaAValidadores() {
        UUID studentId = UUID.randomUUID();
        Set<Producto> cart = Set.of(new Producto());

        LocalDate pickupDate = LocalDate.of(2026, 6, 1);
        FranjaHoraria pickupSlot = new FranjaHoraria();
        pickupSlot.setHoraInicio(LocalTime.of(10, 0)); // Recreo de las 10:00

        schedulePickup.execute(studentId, cart, pickupSlot, pickupDate);

        LocalDateTime expectedTime = LocalDateTime.of(2026, 6, 1, 10, 0);

        // Verificamos que se haya llamado al validador horario con el momento futuro proyectado
        verify(timeValidator).validarCompra(any(), eq(cart), eq(expectedTime));

        // Verificamos que se valide la salud del producto
        verify(healthValidator).validarCompra(any(), eq(cart));
    }
}