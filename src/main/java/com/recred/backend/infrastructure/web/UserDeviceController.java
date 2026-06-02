package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.services.UserDeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dispositivos")
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    public UserDeviceController(UserDeviceService userDeviceService) {
        this.userDeviceService = userDeviceService;
    }

    @PostMapping
    public ResponseEntity<Void> registerDeviceToken(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DeviceTokenRequestDTO request) {
        
        String cognitoId = jwt.getSubject();
        userDeviceService.registerDeviceToken(cognitoId, request.fcmToken());
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
