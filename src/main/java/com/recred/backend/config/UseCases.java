package com.recred.backend.config;

import com.recred.backend.application.AdministrarRestricciones;
import com.recred.backend.application.AgregarRestriccionSalud;
import com.recred.backend.application.ObtenerCatalogoRestriccionesSalud;
import com.recred.backend.application.ObtenerRestriccionesSalud;
import com.recred.backend.application.RemoverRestriccionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
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

    @Bean
    public ObtenerRestriccionesSalud obtenerRestriccionesSalud(
            AlumnoRepository alumnoRepository) {
        return new ObtenerRestriccionesSalud(alumnoRepository);
    }

    @Bean
    public AgregarRestriccionSalud agregarRestriccionSalud(
            AlumnoRepository alumnoRepository,
            ClasificacionSaludRepository clasificacionRepository) {
        return new AgregarRestriccionSalud(alumnoRepository, clasificacionRepository);
    }

    @Bean
    public RemoverRestriccionSalud eliminarRestriccionSalud(
            AlumnoRepository alumnoRepository) {
        return new RemoverRestriccionSalud(alumnoRepository);
    }

    @Bean
    public ObtenerCatalogoRestriccionesSalud obtenerCatalogoRestriccionesSalud(
            ClasificacionSaludRepository clasificacionSaludRepository) {
        return new ObtenerCatalogoRestriccionesSalud(clasificacionSaludRepository);
    }
}
