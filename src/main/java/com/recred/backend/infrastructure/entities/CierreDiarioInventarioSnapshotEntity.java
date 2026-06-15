package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "cierres_diarios_inventario_snapshot",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cierre_id", "producto_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class CierreDiarioInventarioSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cierre_id", nullable = false)
    private CierreDiarioBuffetEntity cierreDiario;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "producto_nombre", nullable = false)
    private String productoNombre;

    @Column(name = "stock_actual")
    private Integer stockActual;

    @Column(name = "stock_reservado")
    private Integer stockReservado;

    @Column(name = "stock_disponible")
    private Integer stockDisponible;

    @Column(name = "stock_minimo")
    private Integer stockMinimo;

    @Column(name = "cupo_maximo_diario")
    private Integer cupoMaximoDiario;

    @Column(name = "cupo_disponible_dia")
    private Integer cupoDisponibleDia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_inventario")
    private EstadoInventario estadoInventario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_manejo_inventario")
    private TipoManejoInventario tipoManejoInventario;

    public static CierreDiarioInventarioSnapshotEntity fromDomain(
            DailyInventorySnapshot snapshot
    ) {
        CierreDiarioInventarioSnapshotEntity entity =
                new CierreDiarioInventarioSnapshotEntity();
        entity.setProductoId(snapshot.productId());
        entity.setProductoNombre(snapshot.productName());
        entity.setStockActual(snapshot.stockActual());
        entity.setStockReservado(snapshot.stockReservado());
        entity.setStockDisponible(snapshot.stockDisponible());
        entity.setStockMinimo(snapshot.stockMinimo());
        entity.setCupoMaximoDiario(snapshot.cupoMaximoDiario());
        entity.setCupoDisponibleDia(snapshot.cupoDisponibleDia());
        entity.setEstadoInventario(snapshot.estadoInventario());
        entity.setTipoManejoInventario(snapshot.tipoManejoInventario());
        return entity;
    }

    public DailyInventorySnapshot toDomain() {
        return new DailyInventorySnapshot(
                productoId,
                productoNombre,
                stockActual,
                stockReservado,
                stockDisponible,
                stockMinimo,
                cupoMaximoDiario,
                cupoDisponibleDia,
                estadoInventario,
                tipoManejoInventario
        );
    }
}
