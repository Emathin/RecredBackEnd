package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;

import java.util.List;

public class GetLoggedUserNotifications {

    private static final int MAX_SIZE = 100;

    private final UsuarioRepository usuarioRepository;
    private final NotificationRepository notificationRepository;

    public GetLoggedUserNotifications(
            UsuarioRepository usuarioRepository,
            NotificationRepository notificationRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.notificationRepository = notificationRepository;
    }

    public GetLoggedUserNotificationsResponse execute(String cognitoSubject, boolean unreadOnly,
                                      int page, int size) {
        validatePagination(page, size);

        Usuario usuario = usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));

        List<Notification> notifications = notificationRepository.findByUserId(
                usuario.getId(),
                unreadOnly,
                page,
                size
        );
        
        List<GetLoggedUserNotificationsResponse.NotificationDTO> dtos = notifications.stream()
                .map(n -> new GetLoggedUserNotificationsResponse.NotificationDTO(
                        n.id(),
                        n.type(),
                        n.title(),
                        n.message(),
                        n.read(),
                        n.createdAt(),
                        n.readAt()
                ))
                .toList();
                
        return new GetLoggedUserNotificationsResponse(dtos);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }

        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_SIZE);
        }
    }
}
