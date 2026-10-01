package com.recred.backend.application;

import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoveCollaboratorUseCase {

    private final TutorRepository tutorRepository;

    @Transactional
    public void execute(UUID tutorPrincipalId, UUID colaboradorId) {
        Tutor tutorPrincipal = tutorRepository.findById(tutorPrincipalId)
                .orElseThrow(() -> new TutorNoEncontradoException("Tutor principal no encontrado"));

        if (tutorPrincipal.getEsColaborador()) {
            throw new SecurityException(
                    "Los colaboradores no pueden eliminar a otros colaboradores");
        }

        Tutor colaborador = tutorRepository.findById(colaboradorId)
                .orElseThrow(() -> new TutorNoEncontradoException("Colaborador no encontrado"));

        if (!tutorPrincipal.getId().equals(colaborador.getTutorPrincipalId())) {
            throw new SecurityException("Este colaborador no pertenece a tu cuenta");
        }

        colaborador.getHijos().clear();
        colaborador.setActiva(false); // Soft delete or we can hard delete
        colaborador.getUsuario().setActivo(false);
        tutorRepository.save(colaborador);
    }
}
