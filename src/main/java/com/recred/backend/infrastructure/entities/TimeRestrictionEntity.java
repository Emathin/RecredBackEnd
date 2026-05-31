package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.TimeRestriction;
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
public class TimeRestrictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private StudentEntity studentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franja_id", nullable = false)
    private ScheduleEntity timeSlotEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private ProductCategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clasificacion_tipo_id")
    private TypeClassificationEntity typeClassificationEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_usuario_id", nullable = false)
    private UserEntity createdByUserEntity;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    public TimeRestriction toDomain() {
        TimeRestriction domain = new TimeRestriction();

        domain.setId(this.getId());
        domain.setStudentId(this.getStudentEntity().getId());
        domain.setActive(this.getActive());

        if (this.getTimeSlotEntity() != null) {
            domain.setTimeBadge(this.getTimeSlotEntity().toDomain());
        }

        if (this.getCategory() != null) {
            domain.setCategory(this.getCategory().toDomain());
        }

        if (this.getTypeClassificationEntity() != null) {
            domain.setClassification(this.getTypeClassificationEntity().toDomain());
        }

        return domain;
    }
}