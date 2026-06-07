package com.recred.backend.config;

import com.recred.backend.domain.services.UserDeviceService;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import com.recred.backend.infrastructure.persistence.UserDeviceTokenJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDeviceConfig {

    @Bean
    public UserDeviceService userDeviceService(
            UserDeviceTokenJpaRepository deviceTokenRepository,
            SpringDataUsuarioRepository usuarioRepository) {
        return new UserDeviceService(deviceTokenRepository, usuarioRepository);
    }
}
