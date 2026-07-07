package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateLowBalanceAlerts;
import com.recred.backend.application.GenerateLowBalanceAlertsRequest;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateLowBalanceAlertsTest {

    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("5000.00");
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_USER_ID = UUID.randomUUID();
    private static final UUID PARENT_USER_ID = UUID.randomUUID();

    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private AlumnoRepository alumnoRepository;
    
    @Mock
    private TutorRepository tutorRepository;
    
    @Mock
    private PushNotificationSender pushNotificationSender;

    @Captor
    private ArgumentCaptor<PushNotification> notificationCaptor;

    @InjectMocks
    private GenerateLowBalanceAlerts useCase;

    @Test
    void sendNotificationsToStudentAndTutor() {
        givenStudentWithLowBalance();
        givenStudentUserExists();
        givenTutorUserExists();

        whenExecuteAlerts();

        thenNotificationsAreSentToStudentAndTutor();
    }

    @Test
    void doNothingWhenNoLowBalances() {
        givenNoStudentsWithLowBalance();

        whenExecuteAlerts();

        thenNoNotificationsAreSent();
    }

    private void givenStudentWithLowBalance() {
        when(walletRepository.findAlumnoIdsWithLowBalance(LOW_BALANCE_THRESHOLD))
                .thenReturn(List.of(STUDENT_ID));
    }

    private void givenNoStudentsWithLowBalance() {
        when(walletRepository.findAlumnoIdsWithLowBalance(any()))
                .thenReturn(List.of());
    }

    private void givenStudentUserExists() {
        when(alumnoRepository.findUserID(STUDENT_ID))
                .thenReturn(STUDENT_USER_ID);
    }

    private void givenTutorUserExists() {
        Usuario parentUser = new Usuario();
        parentUser.setId(PARENT_USER_ID);

        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setUsuario(parentUser);

        when(tutorRepository.findByAlumnoId(STUDENT_ID))
                .thenReturn(List.of(tutor));
    }

    private void whenExecuteAlerts() {
        useCase.execute(new GenerateLowBalanceAlertsRequest());
    }

    private void thenNotificationsAreSentToStudentAndTutor() {
        verify(pushNotificationSender, times(2)).send(notificationCaptor.capture());
        List<PushNotification> sentNotifications = notificationCaptor.getAllValues();

        boolean studentNotificationSent = sentNotifications.stream()
                .anyMatch(n -> n.userId().equals(STUDENT_USER_ID) && 
                        n.message().contains("Tu saldo es bajo"));
        boolean parentNotificationSent = sentNotifications.stream()
                .anyMatch(n -> n.userId().equals(PARENT_USER_ID) && 
                        n.message().contains("El saldo de tu hijo es bajo"));

        assertTrue(studentNotificationSent, "Should send notification to student");
        assertTrue(parentNotificationSent, "Should send notification to tutor");
    }

    private void thenNoNotificationsAreSent() {
        verifyNoInteractions(alumnoRepository, tutorRepository, pushNotificationSender);
    }
}
