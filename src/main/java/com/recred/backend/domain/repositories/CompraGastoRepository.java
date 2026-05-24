package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.GastoCompra;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CompraGastoRepository {

    List<GastoCompra> buscarGastos(
            UUID alumnoId,
            Collection<EstadoCompra> estados,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<CategoriaConsumo> buscarCategoriasMasConsumidas(
            UUID alumnoId,
            Collection<EstadoCompra> estados,
            LocalDateTime desde,
            LocalDateTime hasta
    );
}
