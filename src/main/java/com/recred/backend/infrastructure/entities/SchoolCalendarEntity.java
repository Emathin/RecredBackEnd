package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.DayType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "calendario_escolar",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"colegio_id", "fecha"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SchoolCalendarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private SchoolEntity schoolEntity;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dia", nullable = false)
    private DayType dayType;

    @Column(columnDefinition = "TEXT")
    private String description;
}