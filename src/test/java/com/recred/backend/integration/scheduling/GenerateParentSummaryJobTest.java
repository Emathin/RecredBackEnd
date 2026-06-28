package com.recred.backend.integration.scheduling;

import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.application.GenerateParentSummaryRequest;
import com.recred.backend.application.GenerateParentSummaryResponse;
import com.recred.backend.application.GenerateSummaryNotification;
import com.recred.backend.application.GenerateSummaryNotificationRequest;
import com.recred.backend.application.GenerateSummaryNotificationResponse;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.infrastructure.scheduling.GenerateParentSummaryJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GenerateParentSummaryJobTest {

    @Mock
    private GenerateParentSummary generateParentSummary;
    @Mock
    private GenerateSummaryNotification generateSummaryNotification;
    @Mock
    private PushNotificationSender pushNotificationSender;

    @InjectMocks
    private GenerateParentSummaryJob generateParentSummaryJob;

    @Test
    void testRun() {
        UUID usuarioId = UUID.randomUUID();
        GenerateParentSummaryResponse summaryResponse = new GenerateParentSummaryResponse(List.of(usuarioId));
        when(generateParentSummary.execute(any(GenerateParentSummaryRequest.class))).thenReturn(summaryResponse);

        GenerateSummaryNotificationResponse notificationResponse = new GenerateSummaryNotificationResponse(
                usuarioId, "Title", "Message", "TYPE");
        when(generateSummaryNotification.execute(any(GenerateSummaryNotificationRequest.class)))
                .thenReturn(notificationResponse);

        generateParentSummaryJob.run();

        verify(generateParentSummary, times(1)).execute(any(GenerateParentSummaryRequest.class));
        verify(generateSummaryNotification, times(1)).execute(any(GenerateSummaryNotificationRequest.class));
        
        ArgumentCaptor<PushNotification> captor = ArgumentCaptor.forClass(PushNotification.class);
        verify(pushNotificationSender, times(1)).send(captor.capture());
        
        PushNotification captured = captor.getValue();
        assertEquals(usuarioId, captured.userId());
        assertEquals("Title", captured.title());
        assertEquals("Message", captured.message());
        assertEquals("TYPE", captured.data().get("type"));
    }

    @Test
    void run_WhenSummaryGenerationFails_ShouldContinueWithoutSendingNotification() {
        doThrow(new RuntimeException("error")).when(generateParentSummary)
                .execute(any(GenerateParentSummaryRequest.class));

        generateParentSummaryJob.run();

        verify(generateSummaryNotification, never()).execute(any());
        verify(pushNotificationSender, never()).send(any());
    }

    @Test
    void run_WhenNotificationGenerationFails_ShouldContinueProcessingOthers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        
        GenerateParentSummaryResponse summaryResponse = new GenerateParentSummaryResponse(List.of(user1, user2));
        when(generateParentSummary.execute(any(GenerateParentSummaryRequest.class))).thenReturn(summaryResponse);

        when(generateSummaryNotification.execute(new GenerateSummaryNotificationRequest(user1)))
                .thenThrow(new RuntimeException("error"));
                
        GenerateSummaryNotificationResponse notificationResponse = new GenerateSummaryNotificationResponse(
                user2, "Title", "Message", "TYPE");
        when(generateSummaryNotification.execute(new GenerateSummaryNotificationRequest(user2)))
                .thenReturn(notificationResponse);

        generateParentSummaryJob.run();

        verify(generateSummaryNotification, times(2)).execute(any(GenerateSummaryNotificationRequest.class));
        verify(pushNotificationSender, times(1)).send(any());
    }
}
