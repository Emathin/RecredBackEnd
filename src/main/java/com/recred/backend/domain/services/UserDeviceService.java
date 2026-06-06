package com.recred.backend.domain.services;

import com.recred.backend.infrastructure.entities.DispositivoUsuarioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import com.recred.backend.infrastructure.persistence.UserDeviceTokenJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public class UserDeviceService {

    private final UserDeviceTokenJpaRepository deviceTokenRepository;
    private final SpringDataUsuarioRepository usuarioRepository;

    public UserDeviceService(UserDeviceTokenJpaRepository deviceTokenRepository,
                             SpringDataUsuarioRepository usuarioRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registerDeviceToken(String cognitoId, String fcmToken) {
        UsuarioEntity usuario = usuarioRepository.findByIdCognito(cognitoId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Usuario no encontrado con cognitoId: " + cognitoId));

        Optional<DispositivoUsuarioEntity> existingDevice = 
            deviceTokenRepository.findByFcmToken(fcmToken);

        if (existingDevice.isPresent()) {
            DispositivoUsuarioEntity device = existingDevice.get();
            device.setUsuarioEntity(usuario);
            device.setUltimoAccesoEn(LocalDateTime.now());
            device.setActivo(true);
            deviceTokenRepository.save(device);
        } else {
            DispositivoUsuarioEntity newDevice = new DispositivoUsuarioEntity();
            newDevice.setUsuarioEntity(usuario);
            newDevice.setFcmToken(fcmToken);
            newDevice.setUltimoAccesoEn(LocalDateTime.now());
            newDevice.setActivo(true);
            deviceTokenRepository.save(newDevice);
        }
    }
}
