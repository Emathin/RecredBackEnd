package com.recred.backend.unit.application;

import com.recred.backend.application.ScheduleWalletTopUp;
import com.recred.backend.application.ScheduleWalletTopUpRequest;
import com.recred.backend.application.ScheduleWalletTopUpResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleWalletTopUpTest {

    private static final String COGNITO_SUB = "sub-123";
    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID TUTOR_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final String PARENT_EMAIL = "parent@test.com";
    private static final String PREAPPROVAL_URL = "https://pay.test/preapproval";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private ExternalPaymentService externalPaymentService;

    private ScheduleWalletTopUp scheduleWalletTopUp;

    @BeforeEach
    void setUp() {
        scheduleWalletTopUp = new ScheduleWalletTopUp(
                usuarioRepository, tutorRepository, externalPaymentService);
    }

    @Test
    void execute_successfulSchedule() {
        ScheduleWalletTopUpRequest request = new ScheduleWalletTopUpRequest(
                STUDENT_ID, new BigDecimal("1000.00"), 1, "months");
                
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(USUARIO_ID);
        when(usuario.getEmail()).thenReturn(PARENT_EMAIL);
        when(usuarioRepository.findByIdCognito(COGNITO_SUB)).thenReturn(Optional.of(usuario));

        Tutor tutor = mock(Tutor.class);
        Alumno student = mock(Alumno.class);
        when(tutor.childOrThrow(eq(STUDENT_ID), anyString())).thenReturn(student);
        when(tutorRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(tutor));

        when(externalPaymentService.createScheduledTopUpPreapproval(
                STUDENT_ID, request.amount(), request.frequency(), request.frequencyType(), PARENT_EMAIL))
                .thenReturn(PREAPPROVAL_URL);

        ScheduleWalletTopUpResponse response = scheduleWalletTopUp.execute(COGNITO_SUB, request);

        assertEquals(PREAPPROVAL_URL, response.preapprovalUrl());
        verify(usuario).validateCanLoadBalanceToChild();
        verify(externalPaymentService).createScheduledTopUpPreapproval(
                STUDENT_ID, request.amount(), request.frequency(), request.frequencyType(), PARENT_EMAIL);
    }

    @Test
    void execute_userNotFound_throwsSecurityException() {
        ScheduleWalletTopUpRequest request = new ScheduleWalletTopUpRequest(
                STUDENT_ID, new BigDecimal("1000.00"), 1, "months");
                
        when(usuarioRepository.findByIdCognito(COGNITO_SUB)).thenReturn(Optional.empty());

        SecurityException ex = assertThrows(SecurityException.class, 
                () -> scheduleWalletTopUp.execute(COGNITO_SUB, request));
        assertEquals("Usuario no encontrado", ex.getMessage());
        
        verifyNoInteractions(tutorRepository, externalPaymentService);
    }

    @Test
    void execute_tutorNotFound_throwsSecurityException() {
        ScheduleWalletTopUpRequest request = new ScheduleWalletTopUpRequest(
                STUDENT_ID, new BigDecimal("1000.00"), 1, "months");
                
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(USUARIO_ID);
        when(usuarioRepository.findByIdCognito(COGNITO_SUB)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        SecurityException ex = assertThrows(SecurityException.class, 
                () -> scheduleWalletTopUp.execute(COGNITO_SUB, request));
        assertEquals("El usuario no es un tutor válido", ex.getMessage());
        
        verifyNoInteractions(externalPaymentService);
    }
}
