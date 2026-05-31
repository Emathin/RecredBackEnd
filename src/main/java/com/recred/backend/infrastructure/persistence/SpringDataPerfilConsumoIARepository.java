package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.IAConsumptionProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPerfilConsumoIARepository 
        extends JpaRepository<IAConsumptionProfileEntity, UUID> {
    List<IAConsumptionProfileEntity> findByProductoEntityFavoritoId(UUID productoId);

    @Query("""
            select distinct student
            from StudentEntity student
            join student.favouriteProducts product
            where product.id = :productoId
            """)
    List<StudentEntity> findStudentsWithManualFavoriteProduct(UUID productoId);
}
