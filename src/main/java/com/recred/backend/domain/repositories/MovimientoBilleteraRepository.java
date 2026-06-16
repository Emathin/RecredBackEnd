package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.WalletMovement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MovimientoBilleteraRepository {
    List<WalletMovement> findByStudentIdAndDateRange(
            UUID studentId,
            LocalDateTime from,
            LocalDateTime to
    );

    void registrarMovimiento(
            UUID billeteraId, 
            TipoMovimientoBilletera tipo, 
            BigDecimal monto, 
            String descripcion, 
            UUID usuarioId
    );

    void registrarMovimientoSistema(
            UUID billeteraId,
            TipoMovimientoBilletera tipo,
            BigDecimal monto,
            String descripcion
    );
}
