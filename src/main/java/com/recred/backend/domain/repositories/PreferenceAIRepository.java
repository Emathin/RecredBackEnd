package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import java.util.List;
import java.util.Optional;

public interface PreferenceAIRepository {

    Optional<PreferenciaDetectada> analizarPreferencias(Alumno alumno,
                                                        List<CompraResumen> historial,
                                                        List<Producto> stockDisponible);

    record CompraResumen(String productoNombre, String categoria, java.time.LocalDateTime fecha) {}
    
    record PreferenciaDetectada(String titulo, String mensaje, String productoId, String razonIA) {}
}
