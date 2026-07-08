package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.exceptions.ActiveCreditExistsException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.springframework.security.access.AccessDeniedException;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.TipoPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class EnableSchoolCreditTest {

    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private SchoolCreditRepository schoolCreditRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private Clock clock;

    @InjectMocks
    private EnableSchoolCredit enableSchoolCredit;

    private UUID tutorId;
    private UUID studentId;
    private Billetera wallet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tutorId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        when(clock.instant()).thenReturn(Instant.parse("2026-06-15T10:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        wallet = new Billetera();
        wallet.setSaldoCreditos(BigDecimal.ZERO);
    }

    private Tutor createPremiumTutor() {
        Tutor tutor = new Tutor();
        Usuario usuario = new Usuario();
        usuario.setPlan(TipoPlan.PREMIUM);
        tutor.setUsuario(usuario);
        return tutor;
    }

    @Test
    void execute_successfulWithZeroExpenses() {
        Alumno alumnoMock = new Alumno();
        alumnoMock.setId(studentId);
        alumnoMock.setBilletera(wallet);
        SchoolCredit saved = new SchoolCredit();
        saved.setId(UUID.randomUUID());
        saved.setStudentId(studentId);
        saved.setAmount(new BigDecimal("2000.00"));
        saved.setStatus(CreditStatus.ACTIVE);
        saved.setInstallments(3);
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest(
                tutorId,
                studentId,
                new BigDecimal("2000.00"),
                3
        );

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(createPremiumTutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumnoMock));
        when(schoolCreditRepository.save(any(SchoolCredit.class))).thenReturn(saved);

        EnableSchoolCreditResponse result = enableSchoolCredit.execute(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("2000.00"), result.amount());
        assertEquals(CreditStatus.ACTIVE.name(), result.status());
        assertEquals(3, result.installments());
        assertEquals(true, wallet.getCreditoHabilitado());
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_successfulWithExpenses() {
        Alumno alumnoMock = new Alumno();
        alumnoMock.setId(studentId);
        alumnoMock.setBilletera(wallet);
        SchoolCredit saved = new SchoolCredit();
        saved.setId(UUID.randomUUID());
        saved.setAmount(BigDecimal.valueOf(110.0));
        saved.setStatus(CreditStatus.ACTIVE);
        saved.setInstallments(1);
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest(
                tutorId,
                studentId,
                BigDecimal.valueOf(110.0),
                1
        );

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(createPremiumTutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumnoMock));
        when(schoolCreditRepository.save(any(SchoolCredit.class))).thenReturn(saved);

        EnableSchoolCreditResponse result = enableSchoolCredit.execute(request);

        assertNotNull(result);
        assertEquals(0, wallet.getSaldoCreditos().compareTo(BigDecimal.valueOf(110.0)));
        assertEquals(true, wallet.getCreditoHabilitado());
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_throwsWhenActiveCreditExists() {
        Alumno alumnoMock2 = new Alumno();
        alumnoMock2.setId(studentId);
        Billetera activeWallet = new Billetera();
        activeWallet.setCreditoHabilitado(true);
        alumnoMock2.setBilletera(activeWallet);
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest(
                tutorId,
                studentId,
                new BigDecimal("1000.00"),
                1
        );

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(createPremiumTutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumnoMock2));

        Executable execution = () -> enableSchoolCredit.execute(request);

        assertThrows(ActiveCreditExistsException.class, execution);
    }

    @Test
    void execute_whenStudentHasNoWallet_createsWalletAndUsesDefaultInstallments() {
        Alumno alumno = new Alumno();
        alumno.setId(studentId);
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest(
                tutorId,
                studentId,
                new BigDecimal("1500.00"),
                0
        );

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(createPremiumTutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(schoolCreditRepository.save(any(SchoolCredit.class))).thenAnswer(invocation -> {
            SchoolCredit credit = invocation.getArgument(0);
            credit.setId(UUID.randomUUID());
            return credit;
        });

        EnableSchoolCreditResponse result = enableSchoolCredit.execute(request);

        assertEquals(1, result.installments());
        assertEquals(true, alumno.getBilletera().getCreditoHabilitado());
        assertEquals(new BigDecimal("1500.00"), alumno.getBilletera().getSaldoCreditos());
        verify(alumnoRepository).save(alumno);
        verify(walletRepository).save(alumno.getBilletera());
    }

    @Test
    void execute_whenTutorOrStudentDoesNotExist_throwsBusinessError() {
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest(
                tutorId,
                studentId,
                new BigDecimal("1000.00"),
                1
        );

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.empty(),
                Optional.of(createPremiumTutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        Executable missingTutorExecution = () -> enableSchoolCredit.execute(request);
        IllegalArgumentException missingTutor = assertThrows(
                IllegalArgumentException.class,
                missingTutorExecution
        );
        Executable missingStudentExecution = () -> enableSchoolCredit.execute(request);
        IllegalArgumentException missingStudent = assertThrows(
                IllegalArgumentException.class,
                missingStudentExecution
        );

        assertEquals("Tutor not found", missingTutor.getMessage());
        assertEquals("Student not found", missingStudent.getMessage());
        verifyNoInteractions(schoolCreditRepository, walletRepository);
    }

    @Test
    void execute_whenRequestIsInvalid_throwsBeforeRepositories() {
        EnableSchoolCreditRequest missingTutorRequest = new EnableSchoolCreditRequest(
                null,
                studentId,
                new BigDecimal("1000.00"),
                1
        );
        EnableSchoolCreditRequest missingStudentRequest = new EnableSchoolCreditRequest(
                tutorId,
                null,
                new BigDecimal("1000.00"),
                1
        );
        EnableSchoolCreditRequest invalidAmountRequest = new EnableSchoolCreditRequest(
                tutorId,
                studentId,
                BigDecimal.ZERO,
                1
        );

        Executable missingTutorExecution = () -> enableSchoolCredit.execute(missingTutorRequest);
        IllegalArgumentException missingTutor = assertThrows(
                IllegalArgumentException.class,
                missingTutorExecution
        );
        Executable missingStudentExecution = () -> enableSchoolCredit.execute(
                missingStudentRequest
        );
        IllegalArgumentException missingStudent = assertThrows(
                IllegalArgumentException.class,
                missingStudentExecution
        );
        Executable invalidAmountExecution = () -> enableSchoolCredit.execute(
                invalidAmountRequest
        );
        IllegalArgumentException invalidAmount = assertThrows(
                IllegalArgumentException.class,
                invalidAmountExecution
        );

        assertEquals("tutorId cannot be null", missingTutor.getMessage());
        assertEquals("studentId cannot be null", missingStudent.getMessage());
        assertEquals("Credit amount must be greater than zero", invalidAmount.getMessage());
        verifyNoInteractions(tutorRepository, alumnoRepository, schoolCreditRepository);
    }

    @Test
    void execute_throwsWhenTutorHasFreePlan_throwsAccessDenied() {
        Tutor tutorMock = new Tutor();
        Usuario usuarioMock = new Usuario();
        usuarioMock.setPlan(TipoPlan.GRATUITO);
        tutorMock.setUsuario(usuarioMock);

        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest(
                tutorId,
                studentId,
                new BigDecimal("1000.00"),
                1
        );

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutorMock));

        Executable execution = () -> enableSchoolCredit.execute(request);

        assertThrows(AccessDeniedException.class, execution);
    }

    @Test
    void execute_throwsWhenTutorHasNullUser_throwsAccessDenied() {
        Tutor tutorMock = new Tutor();
        tutorMock.setUsuario(null);

        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest(
                tutorId,
                studentId,
                new BigDecimal("1000.00"),
                1
        );

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutorMock));

        Executable execution = () -> enableSchoolCredit.execute(request);

        assertThrows(AccessDeniedException.class, execution);
    }
}
