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

        List<Tutor> collaborators = tutorRepository.findByTutorPrincipalId(tutorId);
        return collaborators.stream()
                .filter(c -> Boolean.TRUE.equals(c.getActiva()))
                .collect(java.util.stream.Collectors.toMap(
                        Tutor::getId,
                        c -> c,
                        (existing, replacement) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }
}
