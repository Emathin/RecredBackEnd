package com.recred.backend.unit.application;

import com.recred.backend.application.ObtenerHistorialTutor;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObtenerHistorialTutorTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private ObtenerHistorialTutor obtenerHistorialTutor;

    @Test
    void dadoTutorConComprasDeHijos_cuandoSeEjecuta_entoncesRetornaListaDeCompras() {
        UUID tutorId = UUID.randomUUID();
        Purchase compra = new Purchase();
        List<Purchase> comprasMock = List.of(compra);

        when(purchaseRepository.findHistorialByTutor(tutorId)).thenReturn(comprasMock);

        List<Purchase> resultado = obtenerHistorialTutor.execute(tutorId);

        assertEquals(1, resultado.size());
        verify(purchaseRepository, times(1)).findHistorialByTutor(tutorId);
    }
}
