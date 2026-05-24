package com.recred.backend.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "grados")
@Getter
@Setter
@NoArgsConstructor
public class GradoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private ColegioEntity colegioEntity;

    @Column(nullable = false)
    private String anio;

    @Column(nullable = false)
    private String division;

    @Column(nullable = false)
    private Boolean activo = true;
}