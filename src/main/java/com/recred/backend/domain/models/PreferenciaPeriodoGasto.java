package com.recred.backend.domain.models;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import java.math.BigDecimal;

public record PreferenciaPeriodoGasto(
        PeriodoPresupuesto periodo,
        BigDecimal montoLimite
) {
}
