package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;
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
@Table(name = "suscripciones_usuarios")
@Getter
@Setter
@NoArgsConstructor
public class SuscripcionUsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuarioEntity;

    @Column(name = "tipo_plan", nullable = false)
    private TipoPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodoSuscripcion periodo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSuscripcion estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDateTime fechaVencimiento;

    @Column(name = "ultimo_aviso_vencimiento_en")
    private LocalDateTime ultimoAvisoVencimientoEn;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public SuscripcionUsuario toDomain() {
        SuscripcionUsuario subscription = new SuscripcionUsuario();
        subscription.setId(this.id);
        subscription.setUsuarioId(this.usuarioEntity != null ? this.usuarioEntity.getId() : null);
        subscription.setPlan(this.plan);
        subscription.setPeriodo(this.periodo);
        subscription.setPrecio(this.precio);
        subscription.setEstado(this.estado);
        subscription.setFechaInicio(this.fechaInicio);
        subscription.setFechaVencimiento(this.fechaVencimiento);
        subscription.setUltimoAvisoVencimientoEn(this.ultimoAvisoVencimientoEn);
        subscription.setCreadoEn(this.creadoEn);
        subscription.setActualizadoEn(this.actualizadoEn);
        return subscription;
    }
}
