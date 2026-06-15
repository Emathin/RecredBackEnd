package com.recred.backend.application;

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
import com.recred.backend.domain.repositories.TutorRepository;
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
        Alumno alumnoMock = new Alumno();
        alumnoMock.setBilletera(wallet);
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(new Tutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumnoMock));
        when(schoolCreditRepository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(purchaseRepository.sumarGastosPorAlumnoEnRango(eq(studentId), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        
        SchoolCredit saved = new SchoolCredit();
        saved.setId(UUID.randomUUID());
        saved.setAmount(BigDecimal.valueOf(55.0));
        when(schoolCreditRepository.save(any(SchoolCredit.class))).thenReturn(saved);

        SchoolCredit result = enableSchoolCredit.execute(tutorId, studentId);

        assertNotNull(result);
        assertEquals(0, wallet.getSaldoCreditos().compareTo(BigDecimal.valueOf(55.0)));
        assertEquals(true, wallet.getCreditoHabilitado());
        verify(alumnoRepository).save(alumnoMock);
    }

    @Test
    void execute_successfulWithExpenses() {
        Alumno alumnoMock = new Alumno();
        alumnoMock.setBilletera(wallet);
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(new Tutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumnoMock));
        when(schoolCreditRepository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(purchaseRepository.sumarGastosPorAlumnoEnRango(eq(studentId), any(), any()))
                .thenReturn(BigDecimal.valueOf(100.0));

        SchoolCredit saved = new SchoolCredit();
        saved.setId(UUID.randomUUID());
        saved.setAmount(BigDecimal.valueOf(110.0));
        when(schoolCreditRepository.save(any(SchoolCredit.class))).thenReturn(saved);

        SchoolCredit result = enableSchoolCredit.execute(tutorId, studentId);

        assertNotNull(result);
        assertEquals(0, wallet.getSaldoCreditos().compareTo(BigDecimal.valueOf(110.0)));
        assertEquals(true, wallet.getCreditoHabilitado());
        verify(alumnoRepository).save(alumnoMock);
    }

    @Test
    void execute_throwsWhenActiveCreditExists() {
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(new Tutor()));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(new Alumno()));
        when(schoolCreditRepository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE))
                .thenReturn(Optional.of(new SchoolCredit()));

        assertThrows(ActiveCreditExistsException.class, () ->
                enableSchoolCredit.execute(tutorId, studentId)
        );
    }
}
