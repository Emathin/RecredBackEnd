package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DetectStudentPreferencesResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;

import java.util.UUID;

public record StudentPreferenceResponse(
    UUID sugerenciaId,
    UUID alumnoId,
    UUID alumnoUserId,
    String alumnoNombre,
    TipoSugerenciaIA tipo,
    String titulo,
    String mensaje,
    String productoId,
    String razonIA
) {
    public static StudentPreferenceResponse fromDomain(PreferenciaDetectada domain) {
        return new StudentPreferenceResponse(
            null,
            null,
            null,
            null,
            TipoSugerenciaIA.PREFERENCIA_DETECTADA,
            domain.titulo(),
            domain.mensaje(),
            domain.productoId(),
            domain.razonIA()
        );
    }

    public static StudentPreferenceResponse fromUseCaseResponse(
            DetectStudentPreferencesResponse response
    ) {
        return new StudentPreferenceResponse(
            null,
            null,
            null,
            null,
            TipoSugerenciaIA.PREFERENCIA_DETECTADA,
            response.titulo(),
            response.mensaje(),
            response.productoId().toString(),
            response.razonIA()
        );
    }

    public static StudentPreferenceResponse fromEntity(SugerenciaIAEntity entity) {
        String prodId = entity.getMetadata() != null 
                ? (String) entity.getMetadata().get("productoId") : null;
        String razon = entity.getMetadata() != null 
                ? (String) entity.getMetadata().get("razonIA") : null;
        
        return new StudentPreferenceResponse(
            entity.getId(),
            entity.getAlumnoEntity() != null ? entity.getAlumnoEntity().getId() : null,
            null,
            null,
            entity.getTipo(),
            entity.getTitulo(),
            entity.getMensaje(),
            prodId,
            razon
        );
    }

    public static StudentPreferenceResponse fromParentView(
            SugerenciaIAEntity entity,
            Alumno child
    ) {
        StudentPreferenceResponse base = fromEntity(entity);
        String childName = childName(child);
        String parentTitle = childName == null
                ? base.titulo()
                : childName + ": " + base.titulo();
        String parentMessage = childName == null
                ? base.mensaje()
                : childName + ": " + base.mensaje();

        return new StudentPreferenceResponse(
                base.sugerenciaId(),
                child != null ? child.getId() : base.alumnoId(),
                child != null && child.getUsuario() != null
                        ? child.getUsuario().getId()
                        : null,
                childName,
                base.tipo(),
                parentTitle,
                parentMessage,
                base.productoId(),
                base.razonIA()
        );
    }

    public static StudentPreferenceResponse fromStudentView(
            SugerenciaIAEntity entity,
            Alumno child
    ) {
        StudentPreferenceResponse base = fromEntity(entity);
        if (child == null) {
            return base;
        }

        return new StudentPreferenceResponse(
                base.sugerenciaId(),
                child.getId(),
                child.getUsuario() != null ? child.getUsuario().getId() : null,
                childName(child),
                base.tipo(),
                base.titulo(),
                base.mensaje(),
                base.productoId(),
                base.razonIA()
        );
    }

    private static String childName(Alumno child) {
        if (child == null || child.getUsuario() == null) {
            return null;
        }
        String fullName = String.join(" ",
                nullToEmpty(child.getUsuario().getNombre()),
                nullToEmpty(child.getUsuario().getApellido())).trim();
        return fullName.isBlank() ? null : fullName;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
