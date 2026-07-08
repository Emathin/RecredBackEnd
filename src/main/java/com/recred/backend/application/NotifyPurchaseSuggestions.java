package com.recred.backend.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class NotifyPurchaseSuggestions {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyPurchaseSuggestions.class);
    private static final String NOTIFICATION_TITLE = "Recomendación para tu recreo";
    private static final String PURCHASE_SUGGESTION_TYPE = "PURCHASE_SUGGESTION";

    private static final String[] SUGGESTION_MESSAGES = {
            "Elegí mejor, ganá más. Comprá esta opción saludable y sumá puntos.",
            "Tu cuerpo te lo agradece. Elegí esta recomendación y desbloqueá recompensas.",
            "Subí de nivel tu recreo. Canjeá este producto saludable y acercate al premio.",
            "Comer sano tiene premio. Aprovechá esta sugerencia y acumulá beneficios."
    };

    private final PurchaseSuggestIA purchaseSuggestIA;
    private final PushNotificationSender pushNotificationSender;
    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    public NotifyPurchaseSuggestions(
            PurchaseSuggestIA purchaseSuggestIA,
            PushNotificationSender pushNotificationSender,
            UsuarioService usuarioService,
            ObjectMapper objectMapper,
            NotificationRepository notificationRepository
    ) {
        this.purchaseSuggestIA = purchaseSuggestIA;
        this.pushNotificationSender = pushNotificationSender;
        this.usuarioService = usuarioService;
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
    }

    public NotifyPurchaseSuggestionsResponse execute(
            NotifyPurchaseSuggestionsRequest notificationRequest
    ) {
        Optional<PurchaseSuggestionResponse> suggestion = findSuggestion(notificationRequest);
        return buildNotificationResponse(notificationRequest, suggestion);
    }

    private Optional<PurchaseSuggestionResponse> findSuggestion(
            NotifyPurchaseSuggestionsRequest notificationRequest
    ) {
        LOGGER.info("Buscando sugerencias para el usuario: {}", notificationRequest.cognitoId());
        return purchaseSuggestIA.execute(
                new PurchaseSuggestIARequest(notificationRequest.cognitoId())
        );
    }

    private NotifyPurchaseSuggestionsResponse buildNotificationResponse(
            NotifyPurchaseSuggestionsRequest notificationRequest,
            Optional<PurchaseSuggestionResponse> suggestion
    ) {
        if (suggestion.isEmpty()) {
            return noSuggestionFound(notificationRequest);
        }
        return notifyUser(notificationRequest, suggestion.get());
    }

    private NotifyPurchaseSuggestionsResponse notifyUser(
            NotifyPurchaseSuggestionsRequest notificationRequest,
            PurchaseSuggestionResponse suggestion
    ) {
        return new NotifyPurchaseSuggestionsResponse(
                sendNotification(notificationRequest.cognitoId(), suggestion)
        );
    }

    private NotifyPurchaseSuggestionsResponse noSuggestionFound(
            NotifyPurchaseSuggestionsRequest notificationRequest
    ) {
        LOGGER.info("No se encontró sugerencia activa para notificar al usuario: {}",
                notificationRequest.cognitoId());
        return new NotifyPurchaseSuggestionsResponse(null);
    }

    private PushNotificationResponse sendNotification(
            String cognitoId,
            PurchaseSuggestionResponse suggestion
    ) {
        Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoId);
        LOGGER.info("Enviando notificación de sugerencia al usuario id: {}", usuario.getId());

        String message = randomMessage();

        PushNotification notification = createNotification(
                usuario.getId(),
                suggestion,
                rolName(usuario),
                message
        );
        pushNotificationSender.send(notification);
        saveNotificationForUser(usuario, suggestion, message);

        return PushNotificationResponse.fromDomain(notification);
    }

    private PushNotification createNotification(
            UUID userId,
            PurchaseSuggestionResponse suggestion,
            String rol,
            String message
    ) {
        String productoJson = productJson(suggestion);

        return PushNotification.dataOnly(
                userId,
                Map.of(
                        "type", PURCHASE_SUGGESTION_TYPE,
                        "rol", rol,
                        "sugerenciaId", suggestion.id().toString(),
                        "titulo", NOTIFICATION_TITLE,
                        "mensaje", message,
                        "producto", productoJson,
                        "alumnoId", suggestion.alumnoId().toString()
                )
        );
    }

    private void saveNotificationForUser(
            Usuario usuario,
            PurchaseSuggestionResponse suggestion,
            String message
    ) {
        notificationRepository.save(
                buildDbNotification(suggestion, message),
                usuario.getId()
        );
    }

    private Notification buildDbNotification(
            PurchaseSuggestionResponse suggestion,
            String message
    ) {
        return new Notification(
                null,
                TipoNotificacion.SUGERENCIA_IA,
                NOTIFICATION_TITLE,
                message,
                false,
                LocalDateTime.now(),
                null,
                suggestion.alumnoId(),
                productId(suggestion),
                null,
                suggestion.id()
        );
    }

    private UUID productId(PurchaseSuggestionResponse suggestion) {
        return suggestion.producto() != null ? suggestion.producto().id() : null;
    }

    private String productJson(PurchaseSuggestionResponse suggestion) {
        if (suggestion.producto() == null) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(mapProductSuggestion(suggestion));
        } catch (JsonProcessingException exception) {
            LOGGER.error("Error serializando producto para notificación", exception);
            return "{}";
        }
    }

    private String randomMessage() {
        Random random = new Random();
        return SUGGESTION_MESSAGES[random.nextInt(SUGGESTION_MESSAGES.length)];
    }

    private String rolName(Usuario usuario) {
        return usuario.getRol() != null ? usuario.getRol().name() : "PENDIENTE";
    }

    private static Map<String, Object> mapProductSuggestion(
            PurchaseSuggestionResponse suggestion
    ) {
        ProductResponse product = suggestion.producto();
        return Map.of(
                "id", product.id().toString(),
                "nombre", product.nombre(),
                "precio", product.precio(),
                "descripcion", product.descripcion() != null ? product.descripcion() : "",
                "categoria", category(product),
                "estadoStock", "DISPONIBLE",
                "clasificacionesSalud", healthClassifications(product),
                "imagen", product.urlImagen() != null ? product.urlImagen() : ""
        );
    }

    private static Map<String, String> category(ProductResponse product) {
        if (product.categoria() == null) {
            return Map.of("id", "", "descripcion", "General");
        }

        return Map.of(
                "id", product.categoria().id().toString(),
                "descripcion", product.categoria().descripcion()
        );
    }

    private static List<Map<String, String>> healthClassifications(ProductResponse product) {
        if (product.clasificacionesSalud() == null) {
            return List.of();
        }

        return product.clasificacionesSalud().stream()
                .map(classification -> Map.of(
                        "id", classification.id().toString(),
                        "descripcion", classification.descripcion()
                ))
                .toList();
    }
}
