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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLastRechargeTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID WALLET_ID = UUID.randomUUID();
    private static final BigDecimal RECHARGE_AMOUNT = new BigDecimal("250.00");

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private MovimientoBilleteraRepository movimientoBilleteraRepository;

    @InjectMocks
    private GetLastRecharge useCase;

    @Test
    void executeShouldReturnLastWalletRechargeAmount() {
        Alumno student = buildStudentWithWallet();
        givenStudentFound(student);
        givenLastRechargeAmountFound();
        GetLastRechargeRequest request = new GetLastRechargeRequest(STUDENT_ID);

        GetLastRechargeResponse response = whenExecute(request);

        thenResponseMatchesAmount(response, RECHARGE_AMOUNT);
        thenWalletRepositoryWasQueried();
    }

    @Test
    void executeShouldReturnZeroWhenStudentHasNoWallet() {
        Alumno student = buildStudentWithoutWallet();
        givenStudentFound(student);
        GetLastRechargeRequest request = new GetLastRechargeRequest(STUDENT_ID);

        GetLastRechargeResponse response = whenExecute(request);

        thenResponseMatchesAmount(response, BigDecimal.ZERO);
    }

    @Test
    void executeShouldFailWhenStudentDoesNotExist() {
        givenStudentNotFound();
        GetLastRechargeRequest request = new GetLastRechargeRequest(STUDENT_ID);

        Executable execution = () -> whenExecute(request);

        thenThrowsStudentNotFoundException(execution);
    }

    private Alumno buildStudentWithWallet() {
        Alumno student = new Alumno();
        student.setId(STUDENT_ID);
        Billetera wallet = new Billetera();
        wallet.setId(WALLET_ID);
        student.setBilletera(wallet);
        return student;
    }

    private Alumno buildStudentWithoutWallet() {
        Alumno student = new Alumno();
        student.setId(STUDENT_ID);
        return student;
    }

    private void givenStudentFound(Alumno student) {
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    }

    private void givenStudentNotFound() {
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private void givenLastRechargeAmountFound() {
        when(movimientoBilleteraRepository.obtenerUltimoMontoCarga(WALLET_ID))
                .thenReturn(RECHARGE_AMOUNT);
    }

    private GetLastRechargeResponse whenExecute(GetLastRechargeRequest request) {
        return useCase.execute(request);
    }

    private void thenResponseMatchesAmount(GetLastRechargeResponse response, BigDecimal expectedAmount) {
        assertEquals(expectedAmount, response.amount());
    }

    private void thenWalletRepositoryWasQueried() {
        verify(movimientoBilleteraRepository).obtenerUltimoMontoCarga(WALLET_ID);
    }

    private void thenThrowsStudentNotFoundException(Executable execution) {
        IllegalArgumentException missingStudent = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("Student not found", missingStudent.getMessage());
    }
}
