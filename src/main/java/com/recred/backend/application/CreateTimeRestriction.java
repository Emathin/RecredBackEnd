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

        validateGlobalHealthRestriction(alumno, request.classificationId());
        validateExistingRestriction(request);

        RestriccionHoraria restriction = buildRestriction(request, usuario);
        RestriccionHoraria saved = repository.save(restriction);

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

    private void validateGlobalHealthRestriction(Alumno alumno, java.util.UUID classificationId) {
        if (classificationId != null) {
            boolean saludGlobalActiva = alumno.getRestriccionesSalud().stream()
                    .anyMatch(r -> r.getId().equals(classificationId));
            if (saludGlobalActiva) {
                throw new IllegalArgumentException(
                        "El alumno ya posee esta restriccion de salud asignada globalmente.");
            }
        }
    }

    private void validateExistingRestriction(CreateTimeRestrictionRequest request) {
        List<RestriccionHoraria> restriccionesActuales = 
                repository.findAllByAlumnoEntityId(request.studentId());
        
        boolean yaExiste = restriccionesActuales.stream().anyMatch(r -> 
            r.isActiva() &&
            r.getFranjaHoraria() != null && 
            r.getFranjaHoraria().getId().equals(request.timeSlotId()) &&
            ((request.categoryId() != null && 
              r.getCategoria() != null && 
              r.getCategoria().getId().equals(request.categoryId())) ||
             (request.classificationId() != null && 
              r.getClasificacionSalud() != null && 
              r.getClasificacionSalud().getId().equals(request.classificationId())))
        );

        if (yaExiste) {
            throw new IllegalArgumentException(
                    "El alumno ya posee una restriccion activa para esta " +
                    "categoria o tipo en la franja horaria seleccionada.");
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
