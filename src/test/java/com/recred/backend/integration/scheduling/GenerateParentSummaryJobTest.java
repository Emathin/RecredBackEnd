package com.recred.backend.integration.scheduling;

import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import com.recred.backend.infrastructure.scheduling.GenerateParentSummaryJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class GenerateParentSummaryJobTest {

    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private GenerateParentSummary generateParentSummary;
    @Mock
    private PushNotificationSender pushNotificationSender;
    @Mock
    private NotificacionJpaRepository notificacionJpaRepository;
    @Mock
    private SpringDataUsuarioRepository usuarioRepository;

    @InjectMocks
    private GenerateParentSummaryJob generateParentSummaryJob;

    @Test
    void testRun() {
        UUID usuarioId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        tutor.setUsuario(usuario);

        when(tutorRepository.findAll()).thenReturn(Collections.singletonList(tutor));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(new UsuarioEntity()));

        generateParentSummaryJob.run();

        verify(generateParentSummary, times(1)).execute(any(), any(), any());
        verify(pushNotificationSender, times(1)).send(any());
        verify(notificacionJpaRepository, times(1)).save(any());
    }

    @Test
    void run_WhenUsuarioEntityDoesNotExist_ShouldContinueWithoutSavingNotification() {
        UUID usuarioId = UUID.randomUUID();
        Tutor tutor = tutor(usuarioId);

        when(tutorRepository.findAll()).thenReturn(Collections.singletonList(tutor));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        generateParentSummaryJob.run();

        verify(generateParentSummary).execute(any(), any(), any());
        verify(pushNotificationSender).send(any());
        verify(notificacionJpaRepository, never()).save(any());
    }

    @Test
    void run_WhenSummaryGenerationFails_ShouldContinueWithoutSendingNotification() {
        UUID usuarioId = UUID.randomUUID();
        Tutor tutor = tutor(usuarioId);

        when(tutorRepository.findAll()).thenReturn(Collections.singletonList(tutor));
        doThrow(new RuntimeException("error")).when(generateParentSummary)
                .execute(any(), any(), any());

        generateParentSummaryJob.run();

        verify(pushNotificationSender, never()).send(any());
        verify(usuarioRepository, never()).findById(any());
        verify(notificacionJpaRepository, never()).save(any());
    }

    private Tutor tutor(UUID usuarioId) {
        Tutor tutor = new Tutor();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        tutor.setUsuario(usuario);
        return tutor;
    }
}
