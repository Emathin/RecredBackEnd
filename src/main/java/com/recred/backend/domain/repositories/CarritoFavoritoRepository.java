package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.CarritoFavorito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarritoFavoritoRepository {
    CarritoFavorito save(CarritoFavorito carritoFavorito);
    Optional<CarritoFavorito> findById(UUID id);
    List<CarritoFavorito> findByTutorIdAndFilters(UUID tutorId, UUID alumnoId, UUID productoId);
    void delete(UUID id);
}
