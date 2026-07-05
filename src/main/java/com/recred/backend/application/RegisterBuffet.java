package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.ColegioRepository;

import java.time.LocalDateTime;

import org.springframework.transaction.annotation.Transactional;

public class RegisterBuffet {

    private final ColegioRepository colegioRepository;
    private final BuffetRepository buffetRepository;

    public RegisterBuffet(ColegioRepository colegioRepository, BuffetRepository buffetRepository) {
        this.colegioRepository = colegioRepository;
        this.buffetRepository = buffetRepository;
    }

    @Transactional
    public RegisterBuffetResponse execute(RegisterBuffetRequest request) {
        Colegio colegio = colegioRepository.findById(request.schoolId())

                .orElseThrow(() -> new SchoolNotFoundException(request.schoolId()));

        Buffet buffet = buildBuffet(request, colegio);
        Buffet savedBuffet = buffetRepository.save(buffet);

        return new RegisterBuffetResponse(savedBuffet.getId());
    }

    private Buffet buildBuffet(RegisterBuffetRequest request, Colegio colegio) {
        Buffet buffet = new Buffet();
        buffet.setColegio(colegio);
        buffet.setNombre(request.name());
        buffet.setFechaVencimientoHabilitacion(request.habilitationExpirationDate());
        buffet.setActivo(true);
        buffet.setCreadoEn(LocalDateTime.now());
        buffet.setActualizadoEn(LocalDateTime.now());
        return buffet;
    }
}
