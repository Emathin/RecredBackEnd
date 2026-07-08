package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;

import java.util.List;
import java.util.UUID;

public class CreateTimeRestriction {

    private final RestriccionHorariaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;

    public CreateTimeRestriction(
            RestriccionHorariaRepository repository,
            UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public CreateTimeRestrictionResponse execute(CreateTimeRestrictionRequest creationRequest) {
        Usuario creator = findCreatorByCognitoSubject(creationRequest.cognitoSub());
        Alumno student = findStudent(creationRequest.studentId());
        ensureStudentDoesNotHaveGlobalHealthRestriction(
                student,
                creationRequest.classificationId());

        RestriccionHoraria newRestriction = restrictionFrom(creationRequest, creator);
        ensureStudentDoesNotHaveOverlappingTimeRestriction(
                newRestriction,
                creationRequest.studentId());

        RestriccionHoraria savedRestriction = repository.save(newRestriction);
        return responseFrom(savedRestriction);
    }

    private Usuario findCreatorByCognitoSubject(String cognitoSub) {
        return usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
    }

    private Alumno findStudent(UUID studentId) {
        return alumnoRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));
    }

    private void ensureStudentDoesNotHaveGlobalHealthRestriction(
            Alumno student,
            UUID classificationId) {
        if (student.hasGlobalHealthRestriction(classificationId)) {
            throw new IllegalArgumentException("Student already has this " +
                    "health restriction assigned globally.");
        }
    }

    private void ensureStudentDoesNotHaveOverlappingTimeRestriction(
            RestriccionHoraria newRestriction,
            UUID studentId) {
        if (studentHasOverlappingRestriction(newRestriction, studentId)) {
            throw new IllegalArgumentException(
                    "Student already has an active restriction " +
                            "for this category or type in the selected time slot.");
        }
    }

    private boolean studentHasOverlappingRestriction(
            RestriccionHoraria newRestriction,
            UUID studentId) {
        List<RestriccionHoraria> currentRestrictions =
                repository.findAllByAlumnoEntityId(studentId);
        return currentRestrictions.stream()
                .anyMatch(restriction -> restriction.overlapsWith(newRestriction));
    }

    private RestriccionHoraria restrictionFrom(
            CreateTimeRestrictionRequest creationRequest,
            Usuario creator) {
        RestriccionHoraria restriction = new RestriccionHoraria(
                null,
                creationRequest.studentId(),
                timeSlotFrom(creationRequest.timeSlotId()),
                categoryFrom(creationRequest.categoryId()),
                classificationFrom(creationRequest.classificationId())
        );
        restriction.setCreadoPorUsuarioId(creator.getId());
        return restriction;
    }

    private FranjaHoraria timeSlotFrom(UUID timeSlotId) {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(timeSlotId);
        return timeSlot;
    }

    private CategoriaProducto categoryFrom(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        return category;
    }

    private ClasificacionSalud classificationFrom(UUID classificationId) {
        if (classificationId == null) {
            return null;
        }

        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(classificationId);
        return classification;
    }

    private CreateTimeRestrictionResponse responseFrom(RestriccionHoraria saved) {
        return new CreateTimeRestrictionResponse(
                saved.getId(),
                saved.getAlumnoId(),
                saved.getFranjaHoraria() != null ? saved.getFranjaHoraria().getId() : null,
                saved.getCategoria() != null ? saved.getCategoria().getId() : null,
                saved.getClasificacionSalud() != null 
                        ? saved.getClasificacionSalud().getId() 
                        : null,
                saved.isActiva(),
                saved.getCreadoPorUsuarioId()
        );
    }
}
