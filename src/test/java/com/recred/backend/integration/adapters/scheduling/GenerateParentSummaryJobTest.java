package com.recred.backend.integration.adapters.scheduling;

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
}
