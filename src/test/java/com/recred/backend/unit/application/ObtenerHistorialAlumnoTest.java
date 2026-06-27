package com.recred.backend.unit.application;

import com.recred.backend.application.GetHistoryByStudent;
import com.recred.backend.application.GetHistoryByStudentRequest;
import com.recred.backend.application.GetHistoryByStudentResponse;
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
}
