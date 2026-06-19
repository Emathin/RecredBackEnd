package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.RestriccionHoraria;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "restricciones_horarias")
@Getter
@Setter
@NoArgsConstructor
public class RestriccionHorariaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private AlumnoEntity alumnoEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franja_id", nullable = false)
    private FranjaHorariaEntity franjaHorariaEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaProductoEntity categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clasificacion_salud_id")
    private ClasificacionSaludEntity clasificacionSaludEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_usuario_id", nullable = false)
    private UsuarioEntity creadoPorUsuarioEntity;

    @Column(nullable = false)
    private Boolean activa = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    public RestriccionHoraria toDomain() {
        RestriccionHoraria domain = new RestriccionHoraria();

        domain.setId(this.getId());
        domain.setAlumnoId(this.getAlumnoEntity().getId());
        domain.setActiva(this.getActiva());

        if (this.getCreadoPorUsuarioEntity() != null) {
            domain.setCreadoPorUsuarioId(this.getCreadoPorUsuarioEntity().getId());
        }

        if (this.getFranjaHorariaEntity() != null) {
            domain.setFranjaHoraria(this.getFranjaHorariaEntity().toDomain());
        }

        if (this.getCategoria() != null) {
            domain.setCategoria(this.getCategoria().toDomain());
        }

        if (this.getClasificacionSaludEntity() != null) {
            domain.setClasificacionSalud(this.getClasificacionSaludEntity().toDomain());
        }

        return domain;
    }
}