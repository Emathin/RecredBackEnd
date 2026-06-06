package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetLoggedUserNotifications;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final GetLoggedUserNotifications getLoggedUserNotifications;

    public NotificationController(GetLoggedUserNotifications getLoggedUserNotifications) {
        this.getLoggedUserNotifications = getLoggedUserNotifications;
    }

    @GetMapping("/me")
    public List<NotificationResponse> getMyNotifications(
            Authentication authentication,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(name = "page", defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        return getLoggedUserNotifications
                .execute(cognitoSubject(authentication), unreadOnly, page, size)
                .stream()
                .map(NotificationResponse::fromDomain)
                .toList();
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
