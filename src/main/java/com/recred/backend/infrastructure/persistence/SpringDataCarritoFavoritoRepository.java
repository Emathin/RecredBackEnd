package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.CarritoFavoritoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataCarritoFavoritoRepository
        extends JpaRepository<CarritoFavoritoEntity, UUID> {

    @Query("SELECT DISTINCT c FROM CarritoFavoritoEntity c "
            + "LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.productoEntity "
            + "WHERE c.tutor.id = :tutorId "
            + "AND (CAST(:alumnoId AS java.util.UUID) IS NULL OR c.alumno.id = :alumnoId) "
            + "AND (CAST(:productoId AS java.util.UUID) IS NULL OR EXISTS "
            + "(SELECT item FROM CarritoFavoritoItemEntity item "
            + "WHERE item.carritoFavorito = c AND item.productoEntity.id = :productoId))")
    List<CarritoFavoritoEntity> findByTutorIdAndFilters(
            @Param("tutorId") UUID tutorId,
            @Param("alumnoId") UUID alumnoId,
            @Param("productoId") UUID productoId
    );
}
