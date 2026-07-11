package com.recred.backend.application;

import java.util.UUID;

public record UpdateBuffetSettingsRequest(
        UUID buffetId,
        Boolean habilitarVentasAnticipadasNoLaborables
) {
}
