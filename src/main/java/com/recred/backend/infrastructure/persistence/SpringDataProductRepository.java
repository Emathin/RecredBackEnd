package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataProductRepository extends JpaRepository<ProductoEntity, UUID> {
    @Query(""" 
            select p
            FROM ProductoEntity p
            LEFT JOIN CompraDetalleEntity cd ON cd.productoEntity.id = p.id 
            LEFT JOIN CompraEntity c ON cd.compra.id = c.id
            WHERE c.buffetEntity.id = CAST(:buffetId AS java.util.UUID)
            AND  p.id NOT IN(
                        SELECT AI.productoEntity.id
                        FROM AIEventEntity AI
                        WHERE AI.productoEntity.id IS NOT NULL
                        ) 
            GROUP BY p.id 
            ORDER BY SUM(COALESCE(cd.cantidad, 0)) ASC LIMIT 1
            """)
    ProductoEntity findBuffetLeastSoldProducts(
            @Param("buffetId") UUID buffetId
    );
}
