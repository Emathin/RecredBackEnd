package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateLowBalanceAlerts;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateLowBalanceAlertsTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private PushNotificationSender pushNotificationSender;

    @InjectMocks
    private GenerateLowBalanceAlerts generateLowBalanceAlerts;

    @Captor
    private ArgumentCaptor<PushNotification> notificationCaptor;

    @Test
    void execute_DeberiaEnviarNotificacion_AAlumnoYTutor() {
        UUID studentId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID parentUserId = UUID.randomUUID();

        Usuario parentUser = new Usuario();
        parentUser.setId(parentUserId);

        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setUsuario(parentUser);

        when(walletRepository.findAlumnoIdsWithLowBalance(new BigDecimal("5000.00")))
                .thenReturn(List.of(studentId));
        when(alumnoRepository.findUserID(studentId))
                .thenReturn(studentUserId);
        when(tutorRepository.findByAlumnoId(studentId))
                .thenReturn(List.of(tutor));

        generateLowBalanceAlerts.execute();

        verify(pushNotificationSender, times(2)).send(notificationCaptor.capture());
        List<PushNotification> sentNotifications = notificationCaptor.getAllValues();

        boolean studentNotificationSent = sentNotifications.stream()
                .anyMatch(n -> n.userId().equals(studentUserId) && 
                        n.message().contains("Tu saldo es bajo"));
        boolean parentNotificationSent = sentNotifications.stream()
                .anyMatch(n -> n.userId().equals(parentUserId) && 
                        n.message().contains("El saldo de tu hijo es bajo"));

        assertTrue(studentNotificationSent, "Deberia enviar notificacion al alumno");
        assertTrue(parentNotificationSent, "Deberia enviar notificacion al tutor");
    }

    @Test
    void execute_NoDeberiaHacerNada_CuandoNoHaySaldosBajos() {
        when(walletRepository.findAlumnoIdsWithLowBalance(any()))
                .thenReturn(List.of());

        generateLowBalanceAlerts.execute();

        verifyNoInteractions(alumnoRepository, tutorRepository, pushNotificationSender);
    }
}
