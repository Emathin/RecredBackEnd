package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.SuscripcionColegio;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "suscripciones_colegios")
@Getter
@Setter
@NoArgsConstructor
public class SuscripcionColegioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private ColegioEntity colegioEntity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSuscripcion estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDateTime fechaVencimiento;

    @Column(name = "fecha_gracia_hasta", nullable = false)
    private LocalDateTime fechaGraciaHasta;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public SuscripcionColegio toDomain() {
        SuscripcionColegio subscription = new SuscripcionColegio();
        subscription.setId(this.id);
        subscription.setColegioId(this.colegioEntity != null ? this.colegioEntity.getId() : null);
        subscription.setPrecio(this.precio);
        subscription.setMoneda(this.moneda);
        subscription.setEstado(this.estado);
        subscription.setFechaInicio(this.fechaInicio);
        subscription.setFechaVencimiento(this.fechaVencimiento);
        subscription.setFechaGraciaHasta(this.fechaGraciaHasta);
        subscription.setCreadoEn(this.creadoEn);
        subscription.setActualizadoEn(this.actualizadoEn);
        return subscription;
    }
}
