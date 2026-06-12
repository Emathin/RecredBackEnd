package com.recred.backend.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
    private final ObjectMapper objectMapper;

    private static final String[] SUGGESTION_MESSAGES = {
            "¡Elegí mejor, ganá más! Comprá esta option saludable y "
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
            UsuarioService usuarioService,
            ObjectMapper objectMapper) {
        this.purchaseSuggestIA = purchaseSuggestIA;
        this.alumnoRepository = alumnoRepository;
        this.pushNotificationSender = pushNotificationSender;
        this.usuarioService = usuarioService;
        this.objectMapper = objectMapper;
    }

    public PushNotification execute(String cognitoId) {
        LOGGER.info("Buscando sugerencias para el usuario: {}", cognitoId);
        Optional<SugerenciaIA> sugerenciaOpt = purchaseSuggestIA.execute(cognitoId);

        if (sugerenciaOpt.isPresent()) {
            SugerenciaIA sugerencia = sugerenciaOpt.get();
            Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoId);
            
            LOGGER.info("Enviando notificación de sugerencia al usuario id: {}",
                    usuario.getId());
            String rolName = usuario.getRol() != null ? usuario.getRol().name() : "PENDIENTE";
            PushNotification notification = createNotification(
                    usuario.getId(), sugerencia, rolName);
            pushNotificationSender.send(notification);
            return notification;
        } else {
            LOGGER.info("No se encontró sugerencia activa para notificar al usuario: {}",
                    cognitoId);
            return null;
        }
    }

    public PushNotification createNotification(UUID userId, SugerenciaIA sugerencia, String rol) {
        Random random = new Random();
        String randomMessage = SUGGESTION_MESSAGES[random.nextInt(SUGGESTION_MESSAGES.length)];
        String titulo = "Recomendación para tu recreo";

        String productoJson = "{}";
        if (sugerencia.getProducto() != null) {
            Map<String, Object> productMap = Map.of(
                "id", sugerencia.getProducto().getId().toString(),
                "nombre", sugerencia.getProducto().getNombre(),
                "precio", sugerencia.getProducto().getPrecio(),
                "descripcion", sugerencia.getProducto().getDescripcion() != null 
                        ? sugerencia.getProducto().getDescripcion() : "",
                "categoria", Map.of(
                    "id", sugerencia.getProducto().getCategoria() != null 
                            ? sugerencia.getProducto().getCategoria().getId().toString() : "",
                    "descripcion", sugerencia.getProducto().getCategoria() != null 
                            ? sugerencia.getProducto().getCategoria().getDescripcion() : "General"
                ),
                "estadoStock", "DISPONIBLE",
                "clasificacionesSalud", sugerencia.getProducto().getClasificacionesSalud() != null
                        ? sugerencia.getProducto().getClasificacionesSalud().stream()
                            .map(c -> Map.of("id", c.getId().toString(),
                                    "descripcion", c.getDescripcion()))
                            .toList()
                        : List.of()
            );

            try {
                productoJson = objectMapper.writeValueAsString(productMap);
            } catch (JsonProcessingException e) {
                LOGGER.error("Error serializando producto para notificación", e);
            }
        }

        return PushNotification.dataOnly(
                userId,
                Map.of(
                        "type", "PURCHASE_SUGGESTION",
                        "rol", rol,
                        "sugerenciaId", sugerencia.getId().toString(),
                        "titulo", titulo,
                        "mensaje", randomMessage,
                        "producto", productoJson,
                        "alumnoId", sugerencia.getAlumnoId().toString()
                )
        );
    }
}
