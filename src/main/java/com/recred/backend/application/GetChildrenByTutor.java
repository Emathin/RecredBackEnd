package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.TutorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GetChildrenByTutor {

    private final TutorRepository tutorRepository;

    public GetChildrenByTutor(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    @Transactional(readOnly = true)
    public List<GetChildrenByTutorResponse> execute(GetChildrenByTutorRequest request) {
        Set<Alumno> hijos = this.findHijos(request);
        return this.toResponseList(hijos);
    }

    private Set<Alumno> findHijos(GetChildrenByTutorRequest request) {
        if (request.tutorId() != null) {
            return this.findHijosByTutorId(request.tutorId());
        }
        if (request.cognitoSubject() != null) {
            return this.findHijosByCognitoSubject(request.cognitoSubject());
        }
        throw new IllegalArgumentException("Debe proporcionar tutorId o cognitoSubject");
    }

    private Set<Alumno> findHijosByTutorId(UUID tutorId) {
        return tutorRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException(
                        "Tutor no encontrado con ID: " + tutorId))
                .getHijos();
    }

    private Set<Alumno> findHijosByCognitoSubject(String cognitoSubject) {
        return tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new RuntimeException(
                        "Tutor no encontrado para el usuario autenticado"))
                .getHijos();
    }

    private List<GetChildrenByTutorResponse> toResponseList(Set<Alumno> hijos) {
        return hijos.stream()
                .map(this::toResponse)
                .toList();
    }

    private GetChildrenByTutorResponse toResponse(Alumno alumno) {
        String nombre = alumno.getUsuario() != null ?
                alumno.getUsuario().getNombre() : "Sin nombre";
        String apellido = alumno.getUsuario() != null ?
                alumno.getUsuario().getApellido() : "Sin apellido";
        String urlFotoPerfil = alumno.getUsuario() != null ?
                alumno.getUsuario().getUrlFotoPerfil() : null;

        String grado = null;
        UUID colegioId = null;
        if (alumno.getGrado() != null) {
            grado = alumno.getGrado().getAnio() + " " + alumno.getGrado().getDivision();
            if (alumno.getGrado().getColegio() != null) {
                colegioId = alumno.getGrado().getColegio().getId();
            }
        }

        BigDecimal saldo = alumno.getBilletera() != null ?
                alumno.getBilletera().getSaldoCreditos() : BigDecimal.ZERO;

        return new GetChildrenByTutorResponse(
                alumno.getId(),
                nombre,
                apellido,
                alumno.getActivo(),
                grado,
                colegioId,
                saldo,
                urlFotoPerfil
        );
    }
}
