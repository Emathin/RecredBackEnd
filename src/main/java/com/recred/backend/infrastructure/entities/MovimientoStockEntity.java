package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.TipoMovimientoStock;
import com.recred.backend.domain.models.MovimientoStock;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventario_id", nullable = false)
    private InventarioBuffetEntity inventario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimientoStock tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "cantidad_anterior")
    private Integer cantidadAnterior;

    @Column(name = "cantidad_nueva")
    private Integer cantidadNueva;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "compra_id")
    private UUID compraId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_usuario_id")
    private UsuarioEntity creadoPorUsuarioEntity;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    public MovimientoStock toDomain() {
        return new MovimientoStock(
                id,
                inventario != null ? inventario.getId() : null,
                tipo,
                cantidad,
                cantidadAnterior,
                cantidadNueva,
                motivo,
                creadoPorUsuarioEntity != null ? creadoPorUsuarioEntity.getId() : null,
                compraId,
                creadoEn
        );
    }
}
