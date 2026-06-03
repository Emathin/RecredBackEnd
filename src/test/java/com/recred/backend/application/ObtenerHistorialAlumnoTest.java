package com.recred.backend.application;

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
class ObtenerHistorialAlumnoTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private ObtenerHistorialAlumno obtenerHistorialAlumno;

    @Test
    void dadoAlumnoConCompras_cuandoSeEjecuta_entoncesRetornaListaDeCompras() {
        UUID alumnoId = UUID.randomUUID();
        Purchase compra1 = new Purchase();
        Purchase compra2 = new Purchase();
        List<Purchase> comprasMock = List.of(compra1, compra2);

        when(purchaseRepository.findHistorialByAlumno(alumnoId)).thenReturn(comprasMock);

        List<Purchase> resultado = obtenerHistorialAlumno.execute(alumnoId);

        assertEquals(2, resultado.size());
        verify(purchaseRepository, times(1)).findHistorialByAlumno(alumnoId);
    }
}