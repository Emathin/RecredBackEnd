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
    public CreateStudentFromParentResponse execute(CreateStudentFromParentRequest request) {
        request.validate();

        Tutor tutor = findTutorForAuthenticatedParent(request.parentCognitoSubject());

        ensureEmailAvailable(request.email());

        Grado grado = resolveGrade(request.gradoId());

        Usuario savedStudentUser = createStudentUser(request);

        Alumno savedChild = alumnoRepository.save(Alumno.createFromUser(savedStudentUser, grado));
        tutor.addHijo(savedChild);
        tutorRepository.save(tutor);

        return CreateStudentFromParentResponse.fromDomain(savedChild);
    }

    private Usuario createStudentUser(CreateStudentFromParentRequest request) {
        CreatedCognitoUser cognitoUser = createCognitoStudentUser(
                request.username(),
                request.nombre(),
                request.apellido(),
                request.email()
        );

        Usuario studentUser = buildStudentUserFromCognito(
                cognitoUser.sub(),
                request.nombre(),
                request.apellido(),
                request.email(),
                request.dni(),
                request.urlFotoPerfil()
        );

        return usuarioRepository.save(studentUser);
    }

    private Usuario buildStudentUserFromCognito(String cognitoSub, String nombre, String apellido,
                                                String email, String dni, String urlFotoPerfil) {
        LocalDateTime now = LocalDateTime.now();

        Usuario studentUser = new Usuario();
        studentUser.setIdCognito(cognitoSub);
        studentUser.setEmail(email.trim());
        studentUser.setNombre(nombre.trim());
        studentUser.setApellido(apellido.trim());
        studentUser.setDni(normalizeOptional(dni));
        studentUser.setRol(RolUsuario.ALUMNO);
        studentUser.setAutenticadoConCognito(true);
        studentUser.setActivo(true);
        studentUser.setCreadoEn(now);
        studentUser.setActualizadoEn(now);
        studentUser.setUrlFotoPerfil(urlFotoPerfil);
        return studentUser;
    }

    private CreatedCognitoUser createCognitoStudentUser(String username, String nombre,
                                                        String apellido, String email) {
        return cognitoStudentIdentityService.createStudentUser(
                normalizeOptional(username),
                email.trim(),
                nombre.trim(),
                apellido.trim()
        );
    }

    private Tutor findTutorForAuthenticatedParent(String parentCognitoSubject) {
        Usuario parent = findAuthenticatedUser(parentCognitoSubject);
        ensureParentRole(parent);
        return findTutorByUserId(parent.getId());
    }

    private Usuario findAuthenticatedUser(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario autenticado no encontrado"));
    }

    private void ensureParentRole(Usuario user) {
        if (user.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario autenticado no tiene rol PADRE");
        }
    }

    private Tutor findTutorByUserId(UUID userId) {
        return tutorRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor no encontrado para el usuario autenticado"));
    }

    private void ensureEmailAvailable(String email) {
        usuarioRepository.findByEmail(email.trim())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Ya existe un usuario con el email indicado");
                });
    }

    private Grado resolveGrade(UUID gradoId) {
        if (gradoId == null) {
            return null;
        }

        return gradoRepository.findById(gradoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontro el grado indicado"));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
