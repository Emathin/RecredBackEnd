package com.recred.backend.application;

import com.recred.backend.domain.exceptions.OperationNotAllowedException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorSchoolStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.TutorSchoolRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoStudentIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateStudentFromParent {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final AlumnoRepository alumnoRepository;
    private final GradoRepository gradoRepository;
    private final TutorSchoolRepository tutorSchoolRepository;
    private final CognitoStudentIdentityService cognitoStudentIdentityService;

    public CreateStudentFromParent(UsuarioRepository usuarioRepository,
                                   TutorRepository tutorRepository,
                                   AlumnoRepository alumnoRepository,
                                   GradoRepository gradoRepository,
                                   TutorSchoolRepository tutorSchoolRepository,
                                   CognitoStudentIdentityService cognitoStudentIdentityService) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.alumnoRepository = alumnoRepository;
        this.gradoRepository = gradoRepository;
        this.tutorSchoolRepository = tutorSchoolRepository;
        this.cognitoStudentIdentityService = cognitoStudentIdentityService;
    }

    @Transactional
    public CreateStudentFromParentResponse execute(
            CreateStudentFromParentRequest createStudentRequest) {
        StudentCreationContext context = prepareStudentCreationContext(createStudentRequest);
        Usuario savedStudentUser = createStudentUser(createStudentRequest);
        Alumno savedChild = createStudentAndLinkToTutor(savedStudentUser, context);

        return CreateStudentFromParentResponse.fromDomain(savedChild);
    }

    private StudentCreationContext prepareStudentCreationContext(
            CreateStudentFromParentRequest createStudentRequest) {
        Tutor tutor = findTutorForAuthenticatedParent(
                createStudentRequest.parentCognitoSubject());
        ensureEmailAvailable(createStudentRequest.email());
        Grado grade = resolveGradeAllowedForTutor(createStudentRequest.gradoId(), tutor);

        return new StudentCreationContext(tutor, grade);
    }

    private Usuario createStudentUser(CreateStudentFromParentRequest createStudentRequest) {
        CreatedCognitoUser cognitoUser = createCognitoStudentUser(
                createStudentRequest.username(),
                createStudentRequest.nombre(),
                createStudentRequest.apellido(),
                createStudentRequest.email()
        );

        Usuario studentUser = buildStudentUserFromCognito(
                cognitoUser.sub(),
                createStudentRequest.nombre(),
                createStudentRequest.apellido(),
                createStudentRequest.email(),
                createStudentRequest.dni(),
                createStudentRequest.urlFotoPerfil()
        );

        return usuarioRepository.save(studentUser);
    }

    private Alumno createStudentAndLinkToTutor(Usuario studentUser,
                                               StudentCreationContext context) {
        Alumno savedStudent = alumnoRepository.save(
                Alumno.createFromUser(studentUser, context.grade()));
        context.tutor().addHijo(savedStudent);
        tutorRepository.save(context.tutor());
        return savedStudent;
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
            throw new OperationNotAllowedException(
                    "El usuario autenticado no tiene rol PADRE");
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

    private Grado resolveGradeAllowedForTutor(UUID gradoId, Tutor tutor) {
        Grado grade = resolveGrade(gradoId);
        validateTutorSchoolAccess(tutor, grade);
        return grade;
    }

    private void validateTutorSchoolAccess(Tutor tutor, Grado grade) {
        if (grade == null) {
            return;
        }
        if (grade.getColegio() == null || grade.getColegio().getId() == null) {
            throw new IllegalArgumentException("El grado indicado no tiene colegio asociado");
        }
        boolean associated = tutorSchoolRepository.existsByTutorIdAndSchoolIdAndStatus(
                tutor.getId(),
                grade.getColegio().getId(),
                TutorSchoolStatus.ACTIVE
        );
        if (!associated) {
            throw new OperationNotAllowedException(
                    "El tutor no esta asociado al colegio del grado indicado");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record StudentCreationContext(Tutor tutor, Grado grade) {
    }
}
