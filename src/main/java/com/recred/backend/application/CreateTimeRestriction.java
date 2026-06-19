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

    public RestriccionHoraria execute(TimeRestrictionCommand command, String cognitoSub) {
        Usuario usuario = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        Alumno alumno = alumnoRepository.findById(command.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

        if (command.classificationId() != null) {
            boolean saludGlobalActiva = alumno.getRestriccionesSalud().stream()
                    .anyMatch(r -> r.getId().equals(command.classificationId()));
            if (saludGlobalActiva) {
                throw new IllegalArgumentException(
                        "El alumno ya posee esta restricción de salud asignada globalmente.");
            }
        }

        List<RestriccionHoraria> restriccionesActuales = 
                repository.findAllByAlumnoEntityId(command.studentId());
        
        boolean yaExiste = restriccionesActuales.stream().anyMatch(r -> 
            r.isActiva() &&
            r.getFranjaHoraria() != null && 
            r.getFranjaHoraria().getId().equals(command.timeSlotId()) &&
            ((command.categoryId() != null && 
              r.getCategoria() != null && 
              r.getCategoria().getId().equals(command.categoryId())) ||
             (command.classificationId() != null && 
              r.getClasificacionSalud() != null && 
              r.getClasificacionSalud().getId().equals(command.classificationId())))
        );

        if (yaExiste) {
            throw new IllegalArgumentException(
                    "El alumno ya posee una restricción activa para esta " +
                    "categoría o tipo en la franja horaria seleccionada.");
        }

        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(command.timeSlotId());

        CategoriaProducto category = null;
        if (command.categoryId() != null) {
            category = new CategoriaProducto();
            category.setId(command.categoryId());
        }

        ClasificacionSalud classification = null;
        if (command.classificationId() != null) {
            classification = new ClasificacionSalud();
            classification.setId(command.classificationId());
        }

        RestriccionHoraria restriction = new RestriccionHoraria(
                null,
                command.studentId(),
                timeSlot,
                category,
                classification
        );
        restriction.setCreadoPorUsuarioId(usuario.getId());

        return repository.save(restriction);
    }
}