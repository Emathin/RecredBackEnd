package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PreferenciaPeriodoGasto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PreferenciaGastoRepository {
    List<PreferenciaPeriodoGasto> buscarPreferenciasActivas(UUID alumnoId, LocalDate fecha);
}
