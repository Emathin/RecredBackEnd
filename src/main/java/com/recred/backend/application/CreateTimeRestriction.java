package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.List;

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

    public CreateTimeRestrictionResponse execute(CreateTimeRestrictionRequest request) {
        Usuario usuario = findUsuario(request.cognitoSub());
        Alumno alumno = findAlumno(request.studentId());

        if (alumno.hasGlobalHealthRestriction(request.classificationId())) {
            throw new IllegalArgumentException("Student already has this " +
                    "health restriction assigned globally.");
        }

        RestriccionHoraria newRestriction = buildRestriction(request, usuario);
        validateExistingRestriction(newRestriction, request.studentId());

        RestriccionHoraria saved = repository.save(newRestriction);

        return buildResponse(saved);
    }

    private Usuario findUsuario(String cognitoSub) {
        return usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
    }

    private Alumno findAlumno(java.util.UUID studentId) {
        return alumnoRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));
    }


    private void validateExistingRestriction(
            RestriccionHoraria newRestriction, java.util.UUID studentId) {
        List<RestriccionHoraria> restriccionesActuales = 
                repository.findAllByAlumnoEntityId(studentId);
        
        boolean alreadyExists = restriccionesActuales.stream().
                anyMatch(r -> r.overlapsWith(newRestriction));

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Student already has an active restriction " +
                            "for this category or type in the selected time slot.");
        }
    }

    private RestriccionHoraria buildRestriction(
            CreateTimeRestrictionRequest request, 
            Usuario usuario) {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(request.timeSlotId());

        CategoriaProducto category = null;
        if (request.categoryId() != null) {
            category = new CategoriaProducto();
            category.setId(request.categoryId());
        }

        ClasificacionSalud classification = null;
        if (request.classificationId() != null) {
            classification = new ClasificacionSalud();
            classification.setId(request.classificationId());
        }

        RestriccionHoraria restriction = new RestriccionHoraria(
                null,
                request.studentId(),
                timeSlot,
                category,
                classification
        );
        restriction.setCreadoPorUsuarioId(usuario.getId());
        return restriction;
    }

    private CreateTimeRestrictionResponse buildResponse(RestriccionHoraria saved) {
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
