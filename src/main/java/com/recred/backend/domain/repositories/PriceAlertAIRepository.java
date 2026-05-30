package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import java.math.BigDecimal;
import java.util.Optional;

public interface PriceAlertAIRepository {

    Optional<PriceAlertResult> analyzeImpact(Alumno alumno, 
                                            Producto producto, 
                                            BigDecimal oldPrice, 
                                            BigDecimal newPrice);

    record PriceAlertResult(String titulo, String mensaje, String razonIA) {}
}
