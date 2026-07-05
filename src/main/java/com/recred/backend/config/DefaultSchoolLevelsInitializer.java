package com.recred.backend.config;

import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.repositories.NivelRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DefaultSchoolLevelsInitializer implements ApplicationRunner {

    private static final List<String> DEFAULT_LEVELS = List.of(
            "Inicial",
            "Primario",
            "Secundario"
    );

    private final NivelRepository nivelRepository;

    public DefaultSchoolLevelsInitializer(NivelRepository nivelRepository) {
        this.nivelRepository = nivelRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DEFAULT_LEVELS.forEach(this::ensureLevelExists);
    }

    private void ensureLevelExists(String descripcion) {
        nivelRepository.findByDescripcionIgnoreCase(descripcion)
                .ifPresentOrElse(this::activateLevel, () -> saveLevel(descripcion));
    }

    private void activateLevel(Nivel nivel) {
        if (Boolean.TRUE.equals(nivel.getActivo())) {
            return;
        }

        nivel.setActivo(true);
        nivelRepository.save(nivel);
    }

    private void saveLevel(String descripcion) {
        Nivel nivel = new Nivel();
        nivel.setDescripcion(descripcion);
        nivel.setActivo(true);
        nivelRepository.save(nivel);
    }
}
