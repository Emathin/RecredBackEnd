package com.recred.backend.unit.application;

import com.recred.backend.application.GetTutorHistory;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
        List<Purchase> comprasMock = List.of(compra);

        when(purchaseRepository.findHistorialByTutor(tutorId)).thenReturn(comprasMock);

        List<Purchase> resultado = obtenerHistorialTutor.execute(tutorId);

        assertEquals(1, resultado.size());
        verify(purchaseRepository, times(1)).findHistorialByTutor(tutorId);
    }

    @Test
    void executeByCognitoSubject_CuandoTutorExiste_RetornaHistorial() {
        String subject = "cognito-sub";
        UUID tutorId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        List<Purchase> purchases = List.of(new Purchase());
        when(tutorRepository.findByUsuarioIdCognito(subject)).thenReturn(Optional.of(tutor));
        when(purchaseRepository.findHistorialByTutor(tutorId)).thenReturn(purchases);

        List<Purchase> result = obtenerHistorialTutor.executeByCognitoSubject(subject);

        assertEquals(purchases, result);
        verify(purchaseRepository).findHistorialByTutor(tutorId);
    }

    @Test
    void executeByCognitoSubject_CuandoTutorNoExiste_LanzaExcepcion() {
        String subject = "cognito-sub";
        when(tutorRepository.findByUsuarioIdCognito(subject)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> obtenerHistorialTutor.executeByCognitoSubject(subject));

        assertEquals("Tutor no encontrado para el usuario autenticado", exception.getMessage());
    }
}
