package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.TipoMovimientoBilletera;

import java.math.BigDecimal;
import java.util.UUID;

public interface MovimientoBilleteraRepository {
    void registrarMovimiento(
            UUID billeteraId, 
            TipoMovimientoBilletera tipo, 
            BigDecimal monto, 
            String descripcion, 
            UUID usuarioId
    );
}
