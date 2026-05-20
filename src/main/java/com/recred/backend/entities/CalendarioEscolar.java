package com.recred.backend.entities;

import jakarta.persistence.*;
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
public class CalendarioEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private Colegio colegio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dia", nullable = false)
    private TipoDiaEscolar tipoDia;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}