package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RewardLevel;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrizeHealthyPurchaseTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private PrizeHealthyPurchase prizeHealthyPurchase;

    @Test
    void execute_DebePremiarConNivelBronce_CuandoEsLaPrimeraCompraSaludable() {
        UUID alumnoId = UUID.randomUUID();
        UUID compraId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        PrizeHealthyPurchaseCommand command = new PrizeHealthyPurchaseCommand(alumnoId, compraId);

        Purchase purchase = new Purchase();
        purchase.setSuggestionId(suggestionId);
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setPuntosRecompensa(0);

        when(purchaseRepository.findById(compraId)).thenReturn(Optional.of(purchase));
        when(purchaseRepository.countHealthyPurchasesByAlumno(eq(alumnoId), any())).thenReturn(1L);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        prizeHealthyPurchase.execute(command);

        ArgumentCaptor<Alumno> alumnoCaptor = ArgumentCaptor.forClass(Alumno.class);
        verify(alumnoRepository).save(alumnoCaptor.capture());
        assertEquals(RewardLevel.BRONZE.getPoints(), alumnoCaptor.getValue().getPuntosRecompensa());
    }

    @Test
    void execute_DebePremiarConNivelPlata_CuandoTieneTresComprasEnSieteDias() {
        UUID alumnoId = UUID.randomUUID();
        UUID compraId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        PrizeHealthyPurchaseCommand command = new PrizeHealthyPurchaseCommand(alumnoId, compraId);

        Purchase purchase = new Purchase();
        purchase.setSuggestionId(suggestionId);
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setPuntosRecompensa(10);

        when(purchaseRepository.findById(compraId)).thenReturn(Optional.of(purchase));
        when(purchaseRepository.countHealthyPurchasesByAlumno(eq(alumnoId), any())).thenReturn(3L);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        prizeHealthyPurchase.execute(command);

        ArgumentCaptor<Alumno> alumnoCaptor = ArgumentCaptor.forClass(Alumno.class);
        verify(alumnoRepository).save(alumnoCaptor.capture());
        assertEquals(10 + RewardLevel.SILVER.getPoints(), alumnoCaptor.getValue().getPuntosRecompensa());
    }

    @Test
    void execute_DebePremiarConNivelOro_CuandoTieneMasDeCincoComprasEnQuinceDias() {
        UUID alumnoId = UUID.randomUUID();
        UUID compraId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        PrizeHealthyPurchaseCommand command = new PrizeHealthyPurchaseCommand(alumnoId, compraId);

        Purchase purchase = new Purchase();
        purchase.setSuggestionId(suggestionId);
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setPuntosRecompensa(50);

        when(purchaseRepository.findById(compraId)).thenReturn(Optional.of(purchase));
        // Simulamos que en los últimos 7 días tiene 2, pero en los últimos 15 días tiene 6
        when(purchaseRepository.countHealthyPurchasesByAlumno(eq(alumnoId), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    LocalDateTime since = invocation.getArgument(1);
                    if (since.isAfter(LocalDateTime.now().minusDays(8))) {
                        return 2L;
                    }
                    return 6L;
                });
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        prizeHealthyPurchase.execute(command);

        ArgumentCaptor<Alumno> alumnoCaptor = ArgumentCaptor.forClass(Alumno.class);
        verify(alumnoRepository).save(alumnoCaptor.capture());
        assertEquals(50 + RewardLevel.GOLD.getPoints(), alumnoCaptor.getValue().getPuntosRecompensa());
    }

    @Test
    void execute_NoDebeHacerNada_CuandoLaCompraNoTieneSugerencia() {
        UUID alumnoId = UUID.randomUUID();
        UUID compraId = UUID.randomUUID();
        PrizeHealthyPurchaseCommand command = new PrizeHealthyPurchaseCommand(alumnoId, compraId);

        Purchase purchase = new Purchase();
        purchase.setSuggestionId(null);

        when(purchaseRepository.findById(compraId)).thenReturn(Optional.of(purchase));

        prizeHealthyPurchase.execute(command);

        verify(purchaseRepository, never()).countHealthyPurchasesByAlumno(any(), any());
        verify(alumnoRepository, never()).save(any());
    }
}
