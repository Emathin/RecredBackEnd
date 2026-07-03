package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import org.springframework.data.domain.Persistable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "configuracion_cobro_kiosqueros")
@Getter
@Setter
@NoArgsConstructor
public class KiosqueroPayoutConfigEntity implements Persistable<UUID> {

    @Id
    @Column(name = "kiosquero_id")
    private UUID kiosqueroId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "kiosquero_id")
    private KiosqueroEntity kiosquero;

    @Column(name = "cvu_destino", nullable = false, length = 50)
    private String destinationCvu;

    @Column(name = "cuit_destino", nullable = false, length = 20)
    private String destinationCuit;

    @Column(name = "titular_cuenta", nullable = false, length = 150)
    private String accountHolderName;

    @Column(name = "cantidad_intervalo", nullable = false)
    private int cantidadIntervalo;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_intervalo", nullable = false, length = 20)
    private IntervalUnit unidadIntervalo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutScheduleStatus estado;

    @Column(name = "proxima_ejecucion", nullable = false)
    private LocalDate proximaEjecucion;

    @Column(name = "ultima_ejecucion")
    private LocalDate ultimaEjecucion;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public KiosqueroPayoutConfig toDomain() {
        KiosqueroPayoutConfig domain = new KiosqueroPayoutConfig();
        domain.setKiosqueroId(this.kiosqueroId);
        domain.setDestinationCvu(this.destinationCvu);
        domain.setDestinationCuit(this.destinationCuit);
        domain.setAccountHolderName(this.accountHolderName);
        domain.setCantidadIntervalo(this.cantidadIntervalo);
        domain.setUnidadIntervalo(this.unidadIntervalo);
        domain.setEstado(this.estado);
        domain.setProximaEjecucion(this.proximaEjecucion);
        domain.setUltimaEjecucion(this.ultimaEjecucion);
        domain.setCreadoEn(this.creadoEn);
        domain.setActualizadoEn(this.actualizadoEn);
        return domain;
    }

    @Override
    public UUID getId() {
        return this.kiosqueroId;
    }

    @Override
    public boolean isNew() {
        return this.creadoEn == null;
    }
}
