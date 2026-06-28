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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    void execute_successfulWithZeroExpenses() {
        Alumno alumnoMock = new Alumno(); alumnoMock.setId(studentId);
        alumnoMock.setBilletera(wallet);
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(new Tutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumnoMock));
        when(schoolCreditRepository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(purchaseRepository.sumarGastosPorAlumnoEnRango(eq(studentId), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        SchoolCredit saved = new SchoolCredit();
        saved.setId(UUID.randomUUID());
        saved.setStudentId(studentId);
        saved.setAmount(new BigDecimal("2000.00"));
        saved.setStatus(CreditStatus.ACTIVE);
        saved.setInstallments(3);
        when(schoolCreditRepository.save(any(SchoolCredit.class))).thenReturn(saved);

        com.recred.backend.application.EnableSchoolCreditResponse result = enableSchoolCredit.execute(new com.recred.backend.application.EnableSchoolCreditRequest(tutorId, studentId, new BigDecimal("2000.00"), 3));

        assertNotNull(result);
        
        assertEquals(new BigDecimal("2000.00"), result.amount());
        assertEquals(CreditStatus.ACTIVE.name(), result.status());
        assertEquals(3, result.installments());
        assertEquals(true, wallet.getCreditoHabilitado());
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_successfulWithExpenses() {
        Alumno alumnoMock = new Alumno(); alumnoMock.setId(studentId);
        alumnoMock.setBilletera(wallet);
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(new Tutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumnoMock));
        when(schoolCreditRepository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(purchaseRepository.sumarGastosPorAlumnoEnRango(eq(studentId), any(), any()))
                .thenReturn(BigDecimal.valueOf(100.0));

        SchoolCredit saved = new SchoolCredit();
        saved.setId(UUID.randomUUID());
        saved.setAmount(BigDecimal.valueOf(110.0)); saved.setStatus(CreditStatus.ACTIVE); saved.setInstallments(1);
        when(schoolCreditRepository.save(any(SchoolCredit.class))).thenReturn(saved);

        com.recred.backend.application.EnableSchoolCreditResponse result = enableSchoolCredit.execute(new com.recred.backend.application.EnableSchoolCreditRequest(tutorId, studentId, BigDecimal.valueOf(110.0), 1));

        assertNotNull(result);
        assertEquals(0, wallet.getSaldoCreditos().compareTo(BigDecimal.valueOf(110.0)));
        assertEquals(true, wallet.getCreditoHabilitado());
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_throwsWhenActiveCreditExists() {
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(new Tutor()));
        Alumno alumnoMock2 = new Alumno(); alumnoMock2.setId(studentId); when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumnoMock2));
        when(schoolCreditRepository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.of(new SchoolCredit()));

        assertThrows(ActiveCreditExistsException.class, () -> {
            enableSchoolCredit.execute(new com.recred.backend.application.EnableSchoolCreditRequest(tutorId, studentId, new BigDecimal("1000.00"), 1));
        });
    }
}
