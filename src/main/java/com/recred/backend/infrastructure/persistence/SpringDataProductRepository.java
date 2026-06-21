package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataProductRepository extends JpaRepository<ProductoEntity, UUID> {

        List<ProductoEntity> findAllByActivoTrue();

        Optional<ProductoEntity> findByIdAndActivoTrue(UUID id);

        @Query("""
                        SELECT new com.recred.backend.infrastructure.persistence.ProductStockResult(
                            p,
                            i.stockActual,
                            COALESCE(i.stockReservado, 0),
                            i.stockMinimo,
                            i.estado,
                            i.tipoManejo,
                            i.cupoMaximoDiario,
                            CASE
                                WHEN i.id IS NULL THEN null
                                WHEN i.tipoManejo = 'DISPONIBLE_NO_DISPONIBLE'
                                    THEN CASE
                                        WHEN i.estado IN ('DISPONIBLE', 'BAJO_STOCK')
                                            THEN 1
                                        ELSE 0
                                    END
                                ELSE i.stockActual - COALESCE(i.stockReservado, 0)
                            END,
                            i.urlImagen
                        )
                        FROM ProductoEntity p
                        JOIN InventarioBuffetEntity i
                            ON i.productoEntity = p
                            AND i.buffetEntity.id = :buffetId
                        WHERE p.activo = true
                        """)
        List<ProductStockResult> findAllActiveWithStockParams(
                        @Param("buffetId") UUID buffetId);

        @Query("""
                        SELECT new com.recred.backend.infrastructure.persistence.ProductStockResult(
                            p,
                            i.stockActual,
                            COALESCE(i.stockReservado, 0),
                            i.stockMinimo,
                            i.estado,
                            i.tipoManejo,
                            i.cupoMaximoDiario,
                            CASE
                                WHEN i.id IS NULL THEN null
                                WHEN i.tipoManejo = 'DISPONIBLE_NO_DISPONIBLE'
                                    THEN CASE
                                        WHEN i.estado IN ('DISPONIBLE', 'BAJO_STOCK')
                                            THEN 1
                                        ELSE 0
                                    END
                                ELSE i.stockActual - COALESCE(i.stockReservado, 0)
                            END,
                            i.urlImagen
                        )
                        FROM ProductoEntity p
                        JOIN InventarioBuffetEntity i
                            ON i.productoEntity = p
                            AND i.buffetEntity.id = :buffetId
                        WHERE p.id = :id
                            AND p.activo = true
                        """)
        Optional<ProductStockResult> findActiveByIdWithStockParams(
                        @Param("id") UUID id,
                        @Param("buffetId") UUID buffetId);

        @Modifying
        @Query("""
                        update ProductoEntity p
                        set p.precio = :newPrice
                        where p.id = :productId
                        """)
        void updatePrice(@Param("productId") UUID productId,
                        @Param("newPrice") BigDecimal newPrice);

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
                        ORDER BY SUM(COALESCE(cd.cantidad, 0)) ASC LIMIT 5
                        """)
        List<ProductoEntity> findBuffetTop5LeastSoldProducts(
                        @Param("buffetId") UUID buffetId);

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
                        @Param("buffetId") UUID buffetId);

        @Query("""
                        select p, SUM(COALESCE(cd.cantidad, 0)) as cantidad
                        FROM ProductoEntity p
                        LEFT JOIN CompraDetalleEntity cd ON cd.productoEntity.id = p.id
                        GROUP BY p.id
                        ORDER BY SUM(COALESCE(cd.cantidad, 0)) DESC LIMIT 3
                        """)
        List<ProductoEntity> findBestSoldProducts();

        @Query("""
            SELECT p
            FROM ProductoEntity p
            JOIN CompraDetalleEntity cd ON cd.productoEntity.id = p.id
            JOIN CompraEntity c ON cd.compra.id = c.id
            JOIN InventarioBuffetEntity i ON i.productoEntity = p AND i.buffetEntity.id = :buffetId
            WHERE c.buffetEntity.id = :buffetId
            AND p.categoria.id <> :categoriaId
            AND (i.stockActual - COALESCE(i.stockReservado, 0)) > 0
            GROUP BY p.id
            ORDER BY SUM(cd.cantidad) DESC
            """)
        List<ProductoEntity> findBuffetTop3MostSold(
                @Param("buffetId") UUID buffetId,
                @Param("categoriaId") UUID categoriaId,
                Pageable pageable);
}
