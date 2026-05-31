package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataProductRepository extends JpaRepository<ProductEntity, UUID> {
    @Modifying
    @Query("""
            update ProductEntity p
            set p.price = :newPrice
            where p.id = :productId
            """)
    void updatePrice(@Param("productId") UUID productId,
                     @Param("newPrice") BigDecimal newPrice);

    @Query(""" 
            select p
            FROM ProductEntity p
            LEFT JOIN PurchaseDetailEntity cd ON cd.productEntity.id = p.id 
            LEFT JOIN PurchaseEntity c ON cd.purchase.id = c.id
            WHERE c.buffetEntity.id = CAST(:buffetId AS java.util.UUID)
            AND  p.id NOT IN(
                        SELECT AI.productEntity.id
                        FROM AIEventEntity AI
                        WHERE AI.productEntity.id IS NOT NULL
                        ) 
            GROUP BY p.id 
            ORDER BY SUM(COALESCE(cd.amount, 0)) ASC LIMIT 1
            """)
    ProductEntity findBuffetLeastSoldProducts(
            @Param("buffetId") UUID buffetId
    );

    @Query(""" 
            select p, SUM(COALESCE(cd.amount, 0)) as cantidad
            FROM ProductEntity p
            LEFT JOIN PurchaseDetailEntity cd ON cd.productEntity.id = p.id 
            AND  p.id NOT IN(
                        SELECT AI.productEntity.id
                        FROM AIEventEntity AI
                        WHERE AI.productEntity.id IS NOT NULL
                        ) 
            GROUP BY p.id 
            ORDER BY SUM(COALESCE(cd.amount, 0)) DESC LIMIT 3
            """)
    List<ProductEntity> findBestSoldProducts();
}
