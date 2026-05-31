package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.TimeBadge;
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
public class ScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private SchoolEntity schoolEntity;

    @Column(nullable = false)
    private String description;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime startTime;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Boolean active = true;

    public TimeBadge toDomain() {
        TimeBadge dominio = new TimeBadge();

        dominio.setId(this.id);
        dominio.setDescription(this.description);
        dominio.setStartTime(this.startTime);
        dominio.setEndTime(this.endTime);

        dominio.setActive(this.active != null ? this.active : false);

        if (this.schoolEntity != null) {
            dominio.setSchoolId(this.schoolEntity.getId());
        }

        return dominio;
    }
}