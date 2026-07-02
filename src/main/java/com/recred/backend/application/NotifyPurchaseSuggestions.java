package com.recred.backend.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.UsuarioService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyPurchaseSuggestions {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyPurchaseSuggestions.class);

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

    public NotifyPurchaseSuggestions(
            PurchaseSuggestIA purchaseSuggestIA,
            PushNotificationSender pushNotificationSender,
            UsuarioService usuarioService,
            ObjectMapper objectMapper
    ) {
        this.purchaseSuggestIA = purchaseSuggestIA;
        this.pushNotificationSender = pushNotificationSender;
        this.usuarioService = usuarioService;
        this.objectMapper = objectMapper;
    }

    public NotifyPurchaseSuggestionsResponse execute(NotifyPurchaseSuggestionsRequest request) {
        LOGGER.info("Buscando sugerencias para el usuario: {}", request.cognitoId());
        Optional<PurchaseSuggestionResponse> suggestion = purchaseSuggestIA.execute(
                new PurchaseSuggestIARequest(request.cognitoId())
        );

        if (suggestion.isEmpty()) {
            LOGGER.info("No se encontró sugerencia activa para notificar al usuario: {}",
                    request.cognitoId());
            return new NotifyPurchaseSuggestionsResponse(null);
        }

        return new NotifyPurchaseSuggestionsResponse(
                sendNotification(request.cognitoId(), suggestion.get())
        );
    }

    private PushNotificationResponse sendNotification(
            String cognitoId,
            PurchaseSuggestionResponse suggestion
    ) {
        Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoId);
        LOGGER.info("Enviando notificación de sugerencia al usuario id: {}", usuario.getId());

        PushNotification notification = createNotification(
                usuario.getId(),
                suggestion,
                rolName(usuario)
        );
        pushNotificationSender.send(notification);
        return PushNotificationResponse.fromDomain(notification);
    }

    public PushNotification createNotification(
            UUID userId,
            PurchaseSuggestionResponse suggestion,
            String rol
    ) {
        String productoJson = productJson(suggestion);

        return PushNotification.dataOnly(
                userId,
                Map.of(
                        "type", "PURCHASE_SUGGESTION",
                        "rol", rol,
                        "sugerenciaId", suggestion.id().toString(),
                        "titulo", "Recomendación para tu recreo",
                        "mensaje", randomMessage(),
                        "producto", productoJson,
                        "alumnoId", suggestion.alumnoId().toString()
                )
        );
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
                "clasificacionesSalud", healthClassifications(product)
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
