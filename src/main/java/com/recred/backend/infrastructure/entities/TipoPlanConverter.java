package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.TipoPlan;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoPlanConverter implements AttributeConverter<TipoPlan, String> {

    private static final String LEGACY_PREMIUM = "PREMIUM";

    @Override
    public String convertToDatabaseColumn(TipoPlan attribute) {
        return attribute == null ? TipoPlan.GRATUITO.name() : attribute.name();
    }

    @Override
    public TipoPlan convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return TipoPlan.GRATUITO;
        }

        if (LEGACY_PREMIUM.equals(dbData)) {
            return TipoPlan.AVANZADO;
        }

        return TipoPlan.valueOf(dbData);
    }
}
