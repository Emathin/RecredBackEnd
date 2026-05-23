package com.recred.backend.domain.models;

import com.recred.backend.control_parental.domain.enums.PeriodoPresupuesto;
import java.math.BigDecimal;

public record PreferenciaPeriodoGasto(
        PeriodoPresupuesto periodo,
        BigDecimal montoLimite
) {
}
