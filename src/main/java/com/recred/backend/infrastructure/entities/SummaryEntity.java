package com.recred.backend.infrastructure.entities;

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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumenes")
@Getter
@Setter
@NoArgsConstructor
public class SummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_resumen")
    private UUID id;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDateTime start_date;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDateTime end_date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity user;

    @Column(name = "resumen", columnDefinition = "jsonb", nullable = false)
    private String summary;
}
