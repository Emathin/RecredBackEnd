package com.recred.backend.application;

import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListCollaboratorsUseCase {

    private final TutorRepository tutorRepository;

    @Transactional(readOnly = true)
    public List<Tutor> execute(UUID tutorId) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new TutorNoEncontradoException("Tutor principal no encontrado"));

        if (tutor.getEsColaborador()) {
            throw new SecurityException("Los colaboradores no pueden listar otros colaboradores");
        }

        // We need a repository method findByTutorPrincipalId
        return tutorRepository.findByTutorPrincipalId(tutorId);
    }
}
