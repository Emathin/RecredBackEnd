package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.FranjaHoraria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "franjas_horarias")
@Getter
@Setter
@NoArgsConstructor
public class FranjaHorariaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private ColegioEntity colegioEntity;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "cupo_maximo")
    private Integer cupoMaximo;

    @Column(name = "minutos_corte")
    private Integer minutosCorte = 0;

    public FranjaHoraria toDomain() {
        FranjaHoraria dominio = new FranjaHoraria();

        dominio.setId(this.id);
        dominio.setDescripcion(this.descripcion);
        dominio.setHoraInicio(this.horaInicio);
        dominio.setHoraFin(this.horaFin);
        dominio.setCupoMaximo(this.cupoMaximo);
        dominio.setMinutosCorte(this.minutosCorte);

        dominio.setActivo(this.activo != null ? this.activo : false);

        if (this.colegioEntity != null) {
            dominio.setColegioId(this.colegioEntity.getId());
        }

        return dominio;
    }
}
