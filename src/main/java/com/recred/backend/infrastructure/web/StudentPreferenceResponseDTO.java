package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DetectStudentPreferencesResponse;
import com.recred.backend.application.GetStudentPreferencesResponse;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;

import java.util.UUID;

public record StudentPreferenceResponseDTO(
    UUID sugerenciaId,
    UUID alumnoId,
    UUID alumnoUserId,
    String alumnoNombre,
    TipoSugerenciaIA tipo,
    String titulo,
    String mensaje,
    String productoId,
    String productoNombre,
    String productoImagen,
    java.math.BigDecimal productoPrecio,
    String razonIA
) {
    public static StudentPreferenceResponseDTO fromDomain(PreferenciaDetectada domain) {
        return new StudentPreferenceResponseDTO(
            null,
            null,
            null,
            null,
            TipoSugerenciaIA.PREFERENCIA_DETECTADA,
            domain.titulo(),
            domain.mensaje(),
            domain.productoId(),
            null,
            null,
            null,
            domain.razonIA()
        );
    }

    public static StudentPreferenceResponseDTO fromUseCaseResponse(
            DetectStudentPreferencesResponse response
    ) {
        return new StudentPreferenceResponseDTO(
            null,
            null,
            null,
            null,
            TipoSugerenciaIA.PREFERENCIA_DETECTADA,
            response.titulo(),
            response.mensaje(),
            response.productoId().toString(),
            null,
            null,
            null,
            response.razonIA()
        );
    }

    public static StudentPreferenceResponseDTO fromApplication(
            GetStudentPreferencesResponse.Preference preference
    ) {
        String studentName = studentName(
                preference.studentFirstName(),
                preference.studentLastName());
        String title = titleForView(preference, studentName);
        String message = messageForView(preference, studentName);

        return new StudentPreferenceResponseDTO(

                preference.suggestionId(),
                preference.studentId(),
                preference.studentUserId(),
                studentName,
                preference.type(),
                title,
                message,
                preference.productId(),
                preference.productName(),
                preference.productImageUrl(),
                preference.productPrice(),
                preference.aiReason()
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String studentName(String firstName, String lastName) {
        String fullName = String.join(" ",
                nullToEmpty(firstName),
                nullToEmpty(lastName)).trim();
        return fullName.isBlank() ? null : fullName;
    }

    private static String titleForView(
            GetStudentPreferencesResponse.Preference preference,
            String studentName) {
        if (!preference.parentView() || studentName == null) {
            return preference.title();
        }
        return studentName + ": " + preference.title();
    }

    private static String messageForView(
            GetStudentPreferencesResponse.Preference preference,
            String studentName) {
        if (!preference.parentView() || studentName == null) {
            return preference.message();
        }
        return studentName + ": " + preference.message();
    }
}
