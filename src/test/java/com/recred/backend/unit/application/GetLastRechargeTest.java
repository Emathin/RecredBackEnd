package com.recred.backend.unit.application;

import com.recred.backend.application.GetLastRecharge;
import com.recred.backend.application.GetLastRechargeRequest;
import com.recred.backend.application.GetLastRechargeResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetLastRechargeTest {

    private final AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
    private final MovimientoBilleteraRepository movimientoBilleteraRepository =
            mock(MovimientoBilleteraRepository.class);
    private final GetLastRecharge useCase = new GetLastRecharge(
            alumnoRepository,
            movimientoBilleteraRepository
    );

    @Test
    void executeShouldReturnLastWalletRechargeAmount() {
        UUID studentId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Alumno student = new Alumno();
        Billetera wallet = new Billetera();
        wallet.setId(walletId);
        student.setBilletera(wallet);
        GetLastRechargeRequest request = new GetLastRechargeRequest(studentId);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(movimientoBilleteraRepository.obtenerUltimoMontoCarga(walletId))
                .thenReturn(new BigDecimal("250.00"));

        GetLastRechargeResponse response = useCase.execute(request);

        assertEquals(new BigDecimal("250.00"), response.amount());
        verify(movimientoBilleteraRepository).obtenerUltimoMontoCarga(walletId);
    }

    @Test
    void executeShouldReturnZeroWhenStudentHasNoWallet() {
        UUID studentId = UUID.randomUUID();
        GetLastRechargeRequest request = new GetLastRechargeRequest(studentId);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(new Alumno()));

        GetLastRechargeResponse response = useCase.execute(request);

        assertEquals(BigDecimal.ZERO, response.amount());
    }

    @Test
    void executeShouldFailWhenStudentDoesNotExist() {
        UUID studentId = UUID.randomUUID();
        GetLastRechargeRequest request = new GetLastRechargeRequest(studentId);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException missingStudent = assertThrows(
                IllegalArgumentException.class,
                execution
        );

        assertEquals("Student not found", missingStudent.getMessage());
    }

    @Test
    void executeShouldFailWhenRequestIsInvalid() {
        GetLastRechargeRequest request = new GetLastRechargeRequest(null);

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException invalidRequest = assertThrows(
                IllegalArgumentException.class,
                execution
        );

        assertEquals("studentId cannot be null", invalidRequest.getMessage());
    }
}
