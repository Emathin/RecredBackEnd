package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.PagoSuscripcionColegio;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagos_suscripciones_colegios")
@Getter
@Setter
@NoArgsConstructor
public class PagoSuscripcionColegioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mercado_pago_payment_id", unique = true, nullable = false)
    private String mercadoPagoPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private ColegioEntity colegioEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id")
    private SuscripcionColegioEntity suscripcionEntity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPagoExterno estado;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "aprobado_en")
    private LocalDateTime aprobadoEn;

    public PagoSuscripcionColegio toDomain() {
        PagoSuscripcionColegio payment = new PagoSuscripcionColegio();
        payment.setId(this.id);
        payment.setMercadoPagoPaymentId(this.mercadoPagoPaymentId);
        payment.setColegioId(this.colegioEntity != null ? this.colegioEntity.getId() : null);
        payment.setSuscripcionId(
                this.suscripcionEntity != null ? this.suscripcionEntity.getId() : null);
        payment.setMonto(this.monto);
        payment.setMoneda(this.moneda);
        payment.setEstado(this.estado);
        payment.setCreadoEn(this.creadoEn);
        payment.setAprobadoEn(this.aprobadoEn);
        return payment;
    }
}
