package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;

public class ActualizarFotoAlumno {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final AlumnoRepository alumnoRepository;

    public ActualizarFotoAlumno(UsuarioRepository usuarioRepository,
                                TutorRepository tutorRepository,
                                AlumnoRepository alumnoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public ActualizarFotoAlumnoResponse execute(ActualizarFotoAlumnoRequest request) {
        Usuario parent = usuarioRepository.findByIdCognito(request.parentCognitoSubject())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario autenticado no encontrado"));

        if (parent.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario autenticado no tiene rol PADRE");
        }

        Tutor tutor = tutorRepository.findByUsuarioId(parent.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor no encontrado para el usuario autenticado"));

        Alumno alumno = alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new AlumnoNoEncontradoException(request.alumnoId()));

        boolean esHijo = tutor.getHijos().stream()
                .anyMatch(hijo -> hijo.getId().equals(request.alumnoId()));

        if (!esHijo) {
            throw new AccessDeniedException(
                    "El alumno no pertenece al tutor autenticado");
        }

        String idCognitoAlumno = alumno.getUsuario().getIdCognito();
        usuarioRepository.updateFotoPerfil(idCognitoAlumno, request.urlFotoPerfil());

        alumno.getUsuario().setUrlFotoPerfil(request.urlFotoPerfil());

        return toResponse(alumno);
    }

    private ActualizarFotoAlumnoResponse toResponse(Alumno alumno) {
        String nombre = alumno.getUsuario() != null ?
                alumno.getUsuario().getNombre() : "Sin nombre";
        String apellido = alumno.getUsuario() != null ?
                alumno.getUsuario().getApellido() : "Sin apellido";
        String urlFotoPerfil = alumno.getUsuario() != null ?
                alumno.getUsuario().getUrlFotoPerfil() : null;

        String grado = null;
        java.util.UUID colegioId = null;
        if (alumno.getGrado() != null) {
            grado = alumno.getGrado().getAnio() + " " + alumno.getGrado().getDivision();
            if (alumno.getGrado().getColegio() != null) {
                colegioId = alumno.getGrado().getColegio().getId();
            }
        }

        BigDecimal saldo = alumno.getBilletera() != null ?
                alumno.getBilletera().getSaldoCreditos() : BigDecimal.ZERO;

        return new ActualizarFotoAlumnoResponse(
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
