package com.recred.backend.config;

import com.recred.backend.application.AdministrarRestricciones;
import com.recred.backend.dominio.repositories.AlumnoRepository;
import com.recred.backend.dominio.repositories.ClasificacionSaludRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCases {

    @Bean
    public AdministrarRestricciones administrarRestricciones(
            AlumnoRepository alumnoRepository,
            ClasificacionSaludRepository clasificacionRepository) {
        return new AdministrarRestricciones(alumnoRepository, clasificacionRepository);
    }
}
