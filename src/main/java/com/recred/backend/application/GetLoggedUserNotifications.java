package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
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

    public GetLoggedUserNotificationsResponse execute(
            GetLoggedUserNotificationsRequest request
    ) {
        validatePagination(request.page(), request.size());
        Usuario usuario = authenticatedUser(request.cognitoSubject());
        return new GetLoggedUserNotificationsResponse(notifications(usuario, request));
    }

    private Usuario authenticatedUser(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));
    }

    private List<UserNotificationResponse> notifications(
            Usuario usuario,
            GetLoggedUserNotificationsRequest request
    ) {
        return notificationRepository.findByUserId(
                usuario.getId(),
                request.unreadOnly(),
                request.page(),
                request.size()
        ).stream()
                .map(UserNotificationResponse::fromDomain)
                .toList();
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
