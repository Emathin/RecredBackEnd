package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "compras")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public abstract class CompraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", insertable = false, updatable = false)
    private TipoCompra tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buffet_id", nullable = false)
    private BuffetEntity buffetEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private AlumnoEntity alumnoEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "padre_id")
    private TutorEntity tutorEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCompra estado;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "suggestion_id")
    private UUID suggestionId;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraDetalleEntity> detalles = new ArrayList<>();

    public void addDetalle(CompraDetalleEntity detalle) {
        detalles.add(detalle);
        detalle.setCompra(this);
    }

    public void removeDetalle(CompraDetalleEntity detalle) {
        detalles.remove(detalle);
        detalle.setCompra(null);
    }

    public Purchase toDomain() {
        Purchase domain = new Purchase();
        domain.setId(this.id);
        domain.setTotalAmount(this.montoTotal);
        domain.setStatus(this.estado);
        domain.setPaymentMethod(this.medioPago);
        domain.setDate(this.fechaHora);
        domain.setSuggestionId(this.suggestionId);
        domain.setTipo(this.tipo);
        if (this.buffetEntity != null) {
            domain.setBuffetId(this.buffetEntity.getId());
        }

        if (this instanceof CompraAnticipadaEntity compraAnticipadaEntity) {
            domain.setWithdrawalCode(compraAnticipadaEntity.getCodigoVerificacion());
            if (compraAnticipadaEntity.getRetiroProgramadoEntity() != null) {
                RetiroProgramadoEntity retiroProgramadoEntity =
                        compraAnticipadaEntity.getRetiroProgramadoEntity();
                domain.setPickupDate(retiroProgramadoEntity.getFechaProgramada());
                if (retiroProgramadoEntity.getFranja() != null) {
                    domain.setPickupSlotId(retiroProgramadoEntity.getFranja().getId());
                    domain.setPickupSlotDescription(
                            retiroProgramadoEntity.getFranja().getDescripcion() + " (" +
                            retiroProgramadoEntity.getFranja().getHoraInicio() + " - " +
                            retiroProgramadoEntity.getFranja().getHoraFin() + ")"
                    );
                }
                domain.setWithdrawalStatus(retiroProgramadoEntity.getEstado());
            }
        }

        if (this.alumnoEntity != null) {
            domain.setStudent(this.alumnoEntity.toDomain());
        }

        if (this.tutorEntity != null) {
            domain.setTutorId(this.tutorEntity.getId());
        }

        if (this.detalles != null) {
            domain.setItems(this.detalles.stream()
                    .collect(Collectors.toMap(
                            d -> d.getProductoEntity().toDomain(),
                            CompraDetalleEntity::getCantidad
                    )));
        }

        return domain;
    }
}
