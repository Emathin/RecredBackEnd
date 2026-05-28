package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "presupuestos_alumnos")
@Getter
@Setter
@NoArgsConstructor
public class PresupuestoAlumnoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private AlumnoEntity alumnoEntity;

    @Enumerated(STRING)
    @Column(nullable = false)
    private BudgetPeriod periodo;

    @Column(name = "monto_limite", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoLimite;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_usuario_id", nullable = false)
    private UsuarioEntity creadoPorUsuarioEntity;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReglaPresupuestoCategoriaEntity> reglasCategoria = new ArrayList<>();

    public PresupuestoAlumno toDomain() {
        List<ReglaPresupuestoCategoria> listaDomain = new ArrayList<>();

        if (this.reglasCategoria != null) {
            listaDomain = this.reglasCategoria.stream()
                    .map(ReglaPresupuestoCategoriaEntity::toDomain)
                    .collect(Collectors.toList());
        }

        return new PresupuestoAlumno(
                this.id,
                this.alumnoEntity != null ? this.alumnoEntity.getId() : null,
                this.montoLimite,
                this.periodo != null ? PeriodoPresupuesto.valueOf(this.periodo.name()) : null,
                this.fechaInicio,
                this.activo != null ? this.activo : false,
                listaDomain
        );
    }
}