package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.infrastructure.push.FirebasePushNotificationSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test/notifications")
public class NotificationTestController {

    private final FirebasePushNotificationSender pushNotificationSender;

    public NotificationTestController(FirebasePushNotificationSender pushNotificationSender) {
        this.pushNotificationSender = pushNotificationSender;
    }

    @GetMapping("/send")
    public PushNotification sendTestNotification(@RequestParam UUID idUsuario) {
        PushNotification notification = new PushNotification(
                idUsuario,
                "Título de Prueba",
                "Este es un mensaje de prueba.",
                Map.of("tipo", "PRUEBA")
        );

        pushNotificationSender.send(notification);

        return notification;
    }
}
