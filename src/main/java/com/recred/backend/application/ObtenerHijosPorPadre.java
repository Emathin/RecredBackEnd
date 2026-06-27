package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.TutorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ObtenerHijosPorPadre {

    private final TutorRepository tutorRepository;

    public ObtenerHijosPorPadre(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    @Transactional(readOnly = true)
    public List<ObtenerHijosPorPadreResponse> execute(ObtenerHijosPorPadreRequest request) {
        Set<Alumno> hijos;
        if (request.tutorId() != null) {
            hijos = tutorRepository.findById(request.tutorId())
                    .orElseThrow(() -> new RuntimeException(
                            "Tutor no encontrado con ID: " + request.tutorId()))
                    .getHijos();
        } else if (request.cognitoSubject() != null) {
            hijos = tutorRepository.findByUsuarioIdCognito(request.cognitoSubject())
                    .orElseThrow(() -> new RuntimeException(
                            "Tutor no encontrado para el usuario autenticado"))
                    .getHijos();
        } else {
            throw new IllegalArgumentException("Debe proporcionar tutorId o cognitoSubject");
        }

        return hijos.stream()
                .map(this::toResponse)
                .toList();
    }

    private ObtenerHijosPorPadreResponse toResponse(Alumno alumno) {
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

        return new ObtenerHijosPorPadreResponse(
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
