package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductStatisticsJpaRepository extends JpaRepository<CompraDetalleEntity, UUID> {

    @Query("SELECT COALESCE(SUM(cd.cantidad), 0) FROM CompraDetalleEntity cd JOIN cd.compra c " +
            "WHERE cd.productoEntity.id = :productoId AND c.fechaHora >= :since")
    Long countTotalSalesByProduct(@Param("productoId") UUID productoId,
                                  @Param("since") java.time.LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT c.alumnoEntity.id) FROM CompraDetalleEntity cd " +
            "JOIN cd.compra c WHERE cd.productoEntity.id = :productoId AND c.fechaHora >= :since")
    Long countDistinctCustomersByProduct(@Param("productoId") UUID productoId,
                                         @Param("since") java.time.LocalDateTime since);

    @Query("SELECT COALESCE(SUM(cd.cantidad * cd.precioUnitario), 0) " +
            "FROM CompraDetalleEntity cd JOIN cd.compra c " +
            "WHERE cd.productoEntity.id = :productoId AND c.fechaHora >= :since")
    java.math.BigDecimal calculateTotalRevenueByProduct(
            @Param("productoId") UUID productoId,
            @Param("since") java.time.LocalDateTime since);

    @Query("SELECT SUM(cd.cantidad) FROM CompraDetalleEntity cd JOIN cd.compra c " +
            "WHERE cd.productoEntity.id = :productoId AND c.buffetEntity.id = :buffetId " +
            "AND c.fechaHora >= :since")
    Long countTotalSalesByProductAndBuffet(@Param("productoId") UUID productoId,
                                           @Param("buffetId") UUID buffetId,
                                           @Param("since") java.time.LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT c.alumnoEntity.id) FROM CompraDetalleEntity cd " +
            "JOIN cd.compra c WHERE cd.productoEntity.id = :productoId " +
            "AND c.buffetEntity.id = :buffetId AND c.fechaHora >= :since")
    Long countDistinctCustomersByProductAndBuffet(@Param("productoId") UUID productoId,
                                                  @Param("buffetId") UUID buffetId,
                                                  @Param("since") java.time.LocalDateTime since);

    @Query("SELECT DISTINCT CAST(c.fechaHora AS LocalDate) FROM CompraDetalleEntity cd " +
            "JOIN cd.compra c WHERE cd.productoEntity.id = :productoId " +
            "AND c.buffetEntity.id = :buffetId " +
            "AND c.fechaHora >= :since")
    java.util.List<java.time.LocalDate> getSalesDatesByProduct(
            @Param("productoId") UUID productoId,
            @Param("buffetId") UUID buffetId,
            @Param("since") java.time.LocalDateTime since);

    @Query("SELECT cd.productoEntity.id, SUM(cd.cantidad) as totalVentas " +
            "FROM CompraDetalleEntity cd JOIN cd.compra c " +
            "WHERE c.buffetEntity.id = :buffetId AND c.fechaHora >= :since " +
            "GROUP BY cd.productoEntity.id " +
            "ORDER BY totalVentas DESC")
    java.util.List<Object[]> getSalesRankingsGeneral(@Param("buffetId") UUID buffetId,
                                                     @Param("since") java.time.LocalDateTime since);

    @Query("SELECT cd.productoEntity.id, SUM(cd.cantidad) as totalVentas " +
            "FROM CompraDetalleEntity cd JOIN cd.compra c JOIN cd.productoEntity p " +
            "WHERE c.buffetEntity.id = :buffetId AND p.categoria.id = :categoriaId " +
            "AND c.fechaHora >= :since " +
            "GROUP BY cd.productoEntity.id " +
            "ORDER BY totalVentas DESC")
    java.util.List<Object[]> getSalesRankingsByCategory(
            @Param("buffetId") UUID buffetId,
            @Param("categoriaId") UUID categoriaId,
            @Param("since") java.time.LocalDateTime since);

    @Query("SELECT i.stockActual FROM InventarioBuffetEntity i " +
            "WHERE i.productoEntity.id = :productoId AND i.buffetEntity.id = :buffetId " +
            "ORDER BY i.actualizadoEn DESC LIMIT 1")
    Integer getStockActual(@Param("productoId") UUID productoId,
                           @Param("buffetId") UUID buffetId);

    @Query("SELECT AVG(p.precio) FROM ProductoEntity p " +
            "WHERE p.categoria.id = :categoriaId AND p.activo = true")
    java.math.BigDecimal getAveragePriceByCategory(@Param("categoriaId") UUID categoriaId);

    @Query("SELECT AVG(cast(ventasPorProducto.total as double)) FROM ( " +
            "SELECT SUM(cd.cantidad) as total " +
            "FROM CompraDetalleEntity cd JOIN cd.compra c JOIN cd.productoEntity p " +
            "WHERE c.buffetEntity.id = :buffetId AND p.categoria.id = :categoriaId " +
            "AND c.fechaHora >= :since " +
            "GROUP BY cd.productoEntity.id) as ventasPorProducto")
    java.math.BigDecimal getAverageSalesVolumeByCategory(
            @Param("buffetId") UUID buffetId,
            @Param("categoriaId") UUID categoriaId,
            @Param("since") java.time.LocalDateTime since);

    @Query("SELECT AVG(cast(i.stockActual as double)) FROM InventarioBuffetEntity i " +
            "JOIN i.productoEntity p " +
            "WHERE p.categoria.id = :categoriaId AND i.buffetEntity.id = :buffetId")
    java.math.BigDecimal getAverageStockByCategory(@Param("categoriaId") UUID categoriaId,
                                                  @Param("buffetId") UUID buffetId);
}
