package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.PerfilConsumoIAEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPerfilConsumoIARepository 
        extends JpaRepository<PerfilConsumoIAEntity, UUID> {
    List<PerfilConsumoIAEntity> findByProductoEntityFavoritoId(UUID productoId);

    @Query("""
            select distinct student
            from AlumnoEntity student
            join student.productosFavoritos product
            where product.id = :productoId
            """)
    List<AlumnoEntity> findStudentsWithManualFavoriteProduct(UUID productoId);
}
