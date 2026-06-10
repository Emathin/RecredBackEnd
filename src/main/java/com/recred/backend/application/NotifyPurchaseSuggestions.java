package com.recred.backend.application;

import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class NotifyPurchaseSuggestions {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyPurchaseSuggestions.class);

    private final PurchaseSuggestIA purchaseSuggestIA;
    private final AlumnoRepository alumnoRepository;
    private final PushNotificationSender pushNotificationSender;
    private final UsuarioService usuarioService;

    private static final String[] SUGGESTION_MESSAGES = {
            "¡Elegí mejor, ganá más! Comprá esta opción saludable y "
                    + "sumá puntos para tu próximo premio. 🍏✨",
            "Tu cuerpo te lo agradece y nosotros también. ¡Elegí esta "
                    + "recomendación y desbloqueá recompensas! 🏆🍌",
            "¡Subí de nivel tu recreo! Canjeá este producto saludable y "
                    + "acercate a tu gran premio de la semana. 🚀🍓",
            "Comer sano tiene premio. Aprovechá esta sugerencia y "
                    + "acumulá beneficios exclusivos. 🎖️🥕"
    };

    public NotifyPurchaseSuggestions(
            PurchaseSuggestIA purchaseSuggestIA,
            AlumnoRepository alumnoRepository,
            PushNotificationSender pushNotificationSender,
            UsuarioService usuarioService) {
        this.purchaseSuggestIA = purchaseSuggestIA;
        this.alumnoRepository = alumnoRepository;
        this.pushNotificationSender = pushNotificationSender;
        this.usuarioService = usuarioService;
    }

    public void execute(String cognitoId) {
        LOGGER.info("Buscando sugerencias para el usuario: {}", cognitoId);
        Optional<SugerenciaIA> sugerenciaOpt = purchaseSuggestIA.execute(cognitoId);

        if (sugerenciaOpt.isPresent()) {
            SugerenciaIA sugerencia = sugerenciaOpt.get();
            Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoId);
            
            LOGGER.info("Enviando notificación de sugerencia al usuario id: {}",
                    usuario.getId());
            sendNotification(usuario.getId(), sugerencia);
        } else {
            LOGGER.info("No se encontró sugerencia activa para notificar al usuario: {}",
                    cognitoId);
        }
    }

    private void sendNotification(UUID userId, SugerenciaIA sugerencia) {
        Random random = new Random();
        String randomMessage = SUGGESTION_MESSAGES[random.nextInt(SUGGESTION_MESSAGES.length)];

        PushNotification notification = new PushNotification(
                userId,
                "Recomendación para tu recreo",
                randomMessage,
                Map.of(
                        "type", "PURCHASE_SUGGESTION",
                        "suggestionId", sugerencia.getId().toString(),
                        "productId", sugerencia.getProductoId().toString()
                )
        );
        pushNotificationSender.send(notification);
    }
}
