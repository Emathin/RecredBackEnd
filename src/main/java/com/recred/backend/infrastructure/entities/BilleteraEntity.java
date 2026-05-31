package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.Billetera;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "billeteras")
@Getter
@Setter
@NoArgsConstructor
public class BilleteraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false, unique = true)
    private AlumnoEntity alumnoEntity;

    @Column(name = "saldo_creditos", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoCreditos = BigDecimal.ZERO;

    @Column(name = "limite_credito", precision = 12, scale = 2)
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    @Column(name = "saldo_deuda_interno", precision = 12, scale = 2)
    private BigDecimal saldoDeudaInterno = BigDecimal.ZERO;

    @Column(name = "credito_habilitado", nullable = false)
    private Boolean creditoHabilitado = false;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public Billetera toDomain() {
        Billetera domain = new Billetera();
        domain.setId(this.id);
        domain.setSaldoCreditos(this.saldoCreditos);
        domain.setLimiteCredito(this.limiteCredito);
        domain.setSaldoDeudaInterno(this.saldoDeudaInterno);
        domain.setCreditoHabilitado(this.creditoHabilitado);
        domain.setCreadoEn(this.creadoEn);
        domain.setActualizadoEn(this.actualizadoEn);
        return domain;
    }

    public static BilleteraEntity fromDomain(Billetera billetera) {
        if (billetera == null) {
            return null;
        }

        BilleteraEntity entity = new BilleteraEntity();

        entity.setId(billetera.getId());
        entity.setSaldoCreditos(billetera.getSaldoCreditos());
        entity.setLimiteCredito(billetera.getLimiteCredito());
        entity.setSaldoDeudaInterno(billetera.getSaldoDeudaInterno());
        entity.setCreditoHabilitado(billetera.getCreditoHabilitado());
        entity.setCreadoEn(billetera.getCreadoEn());
        entity.setActualizadoEn(billetera.getActualizadoEn());

        return entity;
    }
}
