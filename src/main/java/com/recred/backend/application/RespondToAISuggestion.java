package com.recred.backend.application;

import com.recred.backend.domain.repositories.SugerenciaIARepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class RespondToAISuggestion {

    private final SugerenciaIARepository sugerenciaIARepository;

    public RespondToAISuggestion(SugerenciaIARepository sugerenciaIARepository) {
        this.sugerenciaIARepository = sugerenciaIARepository;
    }

    @Transactional
    public void execute(UUID sugerenciaId, boolean aceptada) {
        sugerenciaIARepository.findById(sugerenciaId).ifPresent(sugerencia -> {
            sugerencia.setAceptada(aceptada);
            sugerencia.setRespondidaEn(LocalDateTime.now());
            sugerenciaIARepository.guardar(sugerencia);
        });
    }
}
