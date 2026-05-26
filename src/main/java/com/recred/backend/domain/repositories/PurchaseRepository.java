package com.recred.backend.domain.repositories;

<<<<<<< feature/compra-espontanea
import com.recred.backend.domain.models.Purchase;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository {
    void save(Purchase purchase);
    Optional<Purchase> findById(UUID id);
}
=======
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface PurchaseRepository {

    BigDecimal sumarGastosPorAlumnoEnRango(UUID alumnoId,
                                           LocalDateTime inicio, LocalDateTime fin);

    BigDecimal sumarGastosPorCategoriaEnRango(UUID alumnoId,
                                              UUID categoriaId, LocalDateTime inicio,
                                              LocalDateTime fin);
}
>>>>>>> control-presupuesto
