package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoStudentIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateStudentFromParent {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final AlumnoRepository alumnoRepository;
    private final GradoRepository gradoRepository;
    private final CognitoStudentIdentityService cognitoStudentIdentityService;

    public CreateStudentFromParent(UsuarioRepository usuarioRepository,
                                   TutorRepository tutorRepository,
                                   AlumnoRepository alumnoRepository,
                                   GradoRepository gradoRepository,
                                   CognitoStudentIdentityService cognitoStudentIdentityService) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.alumnoRepository = alumnoRepository;
        this.gradoRepository = gradoRepository;
        this.cognitoStudentIdentityService = cognitoStudentIdentityService;
    }

    @Transactional
    public Alumno execute(String parentCognitoSubject, String username, String nombre,
                          String apellido,
                          String email, String dni, UUID gradoId, String urlFotoPerfil) {
        validate(username, nombre, apellido, email);

        Usuario parent = usuarioRepository.findByIdCognito(parentCognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario autenticado no encontrado"));

        if (parent.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario autenticado no tiene rol PADRE");
        }

        Tutor tutor = tutorRepository.findByUsuarioId(parent.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor no encontrado para el usuario autenticado"));

        usuarioRepository.findByEmail(email.trim())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Ya existe un usuario con el email indicado");
                });

        Grado grado = null;
        if (gradoId != null) {
            grado = gradoRepository.findById(gradoId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontro el grado indicado"));
        }

        CreatedCognitoUser cognitoUser = cognitoStudentIdentityService.createStudentUser(
                normalizeOptional(username),
                email.trim(),
                nombre.trim(),
                apellido.trim()
        );

        Usuario childUser = buildChildUser(nombre, apellido, email, dni, cognitoUser,
                urlFotoPerfil);
        Usuario savedChildUser = usuarioRepository.save(childUser);

        Alumno child = new Alumno();
        child.setUsuario(savedChildUser);
        child.setUsuarioId(savedChildUser.getId());
        child.setActivo(true);
        child.setGrado(grado);

        Alumno savedChild = alumnoRepository.save(child);
        tutor.addHijo(savedChild);
        tutorRepository.save(tutor);

        return savedChild;
    }

    private Usuario buildChildUser(String nombre, String apellido, String email, String dni,
                                   CreatedCognitoUser cognitoUser, String urlFotoPerfil) {
        LocalDateTime now = LocalDateTime.now();

        Usuario childUser = new Usuario();
        childUser.setIdCognito(cognitoUser.sub());
        childUser.setEmail(email.trim());
        childUser.setNombre(nombre.trim());
        childUser.setApellido(apellido.trim());
        childUser.setDni(normalizeOptional(dni));
        childUser.setRol(RolUsuario.ALUMNO);
        childUser.setAutenticadoConCognito(true);
        childUser.setActivo(true);
        childUser.setCreadoEn(now);
        childUser.setActualizadoEn(now);
        childUser.setUrlFotoPerfil(urlFotoPerfil);
        return childUser;
    }

    private void validate(String username, String nombre, String apellido, String email) {
        requireNonBlank(nombre, "El nombre del alumno es obligatorio");
        requireNonBlank(apellido, "El apellido del alumno es obligatorio");
        requireNonBlank(email, "El email del alumno es obligatorio");

        if (username != null && username.contains("@")) {
            throw new IllegalArgumentException(
                    "El username no puede tener formato de email");
        }
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
