package com.recred.backend.unit.application;

import com.recred.backend.application.EnableSchoolCredit;
import com.recred.backend.application.EnableSchoolCreditRequest;
import com.recred.backend.application.EnableSchoolCreditResponse;
import com.recred.backend.domain.exceptions.ActiveCreditExistsException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnableSchoolCreditTest {

    private static final BigDecimal CREDIT_AMOUNT = new BigDecimal("2000.00");
    private static final BigDecimal DEFAULT_INSTALLMENT_AMOUNT = new BigDecimal("1500.00");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T10:00:00Z"),
            ZoneId.of("UTC")
    );

    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private SchoolCreditRepository schoolCreditRepository;
    @Mock
    private WalletRepository walletRepository;

    @Captor
    private ArgumentCaptor<SchoolCredit> creditCaptor;

    private EnableSchoolCredit enableSchoolCredit;
    private EnableSchoolCreditRequest enableCreditRequest;
    private EnableSchoolCreditResponse enableCreditResponse;
    private Throwable thrownException;
    private UUID tutorId;
    private UUID studentId;
    private Alumno student;
    private Billetera wallet;
    private SchoolCredit savedCredit;

    @BeforeEach
    void setUp() {
        enableSchoolCredit = new EnableSchoolCredit(
                tutorRepository,
                alumnoRepository,
                schoolCreditRepository,
                walletRepository,
                CLOCK
        );
    }

    @Test
    void givenStudentWithWallet_whenEnablingCredit_thenCreatesCreditAndUpdatesWallet() {
        givenValidCreditRequest(3);
        givenTutorExists();
        givenStudentWithWallet();
        givenCreditIsSaved();

        whenEnableSchoolCredit();

        thenCreditWasCreatedWithInstallments(3);
        thenWalletWasCredited(CREDIT_AMOUNT);
        thenResponseContainsSavedCredit(3);
    }

    @Test
    void givenStudentWithoutWallet_whenEnablingCredit_thenCreatesWalletAndUsesDefaultInstallment() {
        givenCreditRequest(DEFAULT_INSTALLMENT_AMOUNT, 0);
        givenTutorExists();
        givenStudentWithoutWallet();
        givenCreditIsSaved();

        whenEnableSchoolCredit();

        thenStudentWalletWasCreated();
        thenCreditWasCreatedWithInstallments(1);
        thenWalletWasCredited(DEFAULT_INSTALLMENT_AMOUNT);
        thenResponseContainsSavedCredit(1);
    }

    @Test
    void givenStudentWithActiveCredit_whenEnablingCredit_thenThrowsException() {
        givenValidCreditRequest(1);
        givenTutorExists();
        givenStudentWithActiveCredit();

        whenEnableSchoolCreditFails(ActiveCreditExistsException.class);

        thenErrorMessageIs("The student already has an active credit.");
        thenCreditWasNotCreated();
    }

    @Test
    void givenMissingTutor_whenEnablingCredit_thenThrowsException() {
        givenValidCreditRequest(1);
        givenTutorDoesNotExist();

        whenEnableSchoolCreditFails(IllegalArgumentException.class);

        thenErrorMessageIs("Tutor not found");
        thenCreditWasNotCreated();
    }

    @Test
    void givenMissingStudent_whenEnablingCredit_thenThrowsException() {
        givenValidCreditRequest(1);
        givenTutorExists();
        givenStudentDoesNotExist();

        whenEnableSchoolCreditFails(IllegalArgumentException.class);

        thenErrorMessageIs("Student not found");
        thenCreditWasNotCreated();
    }

    private void givenValidCreditRequest(Integer installments) {
        givenCreditRequest(CREDIT_AMOUNT, installments);
    }

    private void givenCreditRequest(BigDecimal amount, Integer installments) {
        tutorId = randomUUID();
        studentId = randomUUID();
        enableCreditRequest = new EnableSchoolCreditRequest(
                tutorId,
                studentId,
                amount,
                installments
        );
    }

    private void givenTutorExists() {
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(new Tutor()));
    }

    private void givenTutorDoesNotExist() {
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.empty());
    }

    private void givenStudentWithWallet() {
        wallet = wallet(false);
        student = studentWithWallet(wallet);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentWithActiveCredit() {
        wallet = wallet(true);
        student = studentWithWallet(wallet);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentWithoutWallet() {
        student = new Alumno();
        student.setId(studentId);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void givenCreditIsSaved() {
        when(schoolCreditRepository.save(any(SchoolCredit.class))).thenAnswer(invocation -> {
            SchoolCredit credit = invocation.getArgument(0);
            credit.setId(randomUUID());
            savedCredit = credit;
            return credit;
        });
    }

    private void whenEnableSchoolCredit() {
        enableCreditResponse = enableSchoolCredit.execute(enableCreditRequest);
    }

    private <T extends Throwable> void whenEnableSchoolCreditFails(Class<T> expectedException) {
        Executable execution = () -> enableSchoolCredit.execute(enableCreditRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenCreditWasCreatedWithInstallments(int installments) {
        verify(schoolCreditRepository).save(creditCaptor.capture());
        SchoolCredit credit = creditCaptor.getValue();
        assertEquals(studentId, credit.getStudentId());
        assertEquals(enableCreditRequest.amount(), credit.getAmount());
        assertEquals(CreditStatus.ACTIVE, credit.getStatus());
        assertEquals(installments, credit.getInstallments());
        assertEquals(LocalDateTime.of(2026, 6, 15, 10, 0), credit.getCreatedAt());
    }

    private void thenWalletWasCredited(BigDecimal expectedAmount) {
        assertEquals(0, student.getBilletera().getSaldoCreditos().compareTo(expectedAmount));
        assertEquals(Boolean.TRUE, student.getBilletera().getCreditoHabilitado());
        verify(walletRepository).save(student.getBilletera());
    }

    private void thenStudentWalletWasCreated() {
        assertNotNull(student.getBilletera());
        verify(alumnoRepository).save(student);
    }

    private void thenResponseContainsSavedCredit(int installments) {
        assertEquals(savedCredit.getId(), enableCreditResponse.creditId());
        assertEquals(enableCreditRequest.amount(), enableCreditResponse.amount());
        assertEquals(CreditStatus.ACTIVE.name(), enableCreditResponse.status());
        assertEquals(installments, enableCreditResponse.installments());
    }

    private void thenCreditWasNotCreated() {
        verify(schoolCreditRepository, never()).save(any());
        thenWalletWasNotSaved();
    }

    private void thenWalletWasNotSaved() {
        verify(walletRepository, never()).save(any());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private Alumno studentWithWallet(Billetera wallet) {
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setBilletera(wallet);
        return student;
    }

    private Billetera wallet(boolean creditEnabled) {
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(BigDecimal.ZERO);
        wallet.setCreditoHabilitado(creditEnabled);
        return wallet;
    }
}
