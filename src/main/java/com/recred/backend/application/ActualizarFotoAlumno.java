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

import java.util.UUID;

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

    public Alumno execute(String parentCognitoSubject, UUID alumnoId, String urlFotoPerfil) {
        Usuario parent = usuarioRepository.findByIdCognito(parentCognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario autenticado no encontrado"));

        if (parent.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario autenticado no tiene rol PADRE");
        }

        Tutor tutor = tutorRepository.findByUsuarioId(parent.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor no encontrado para el usuario autenticado"));

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new AlumnoNoEncontradoException(alumnoId));

        boolean esHijo = tutor.getHijos().stream()
                .anyMatch(hijo -> hijo.getId().equals(alumnoId));

        if (!esHijo) {
            throw new AccessDeniedException(
                    "El alumno no pertenece al tutor autenticado");
        }

        String idCognitoAlumno = alumno.getUsuario().getIdCognito();
        usuarioRepository.updateFotoPerfil(idCognitoAlumno, urlFotoPerfil);

        alumno.getUsuario().setUrlFotoPerfil(urlFotoPerfil);

        return alumno;
    }
}
