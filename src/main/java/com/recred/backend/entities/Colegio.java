package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "colegios")
@Getter
@Setter
@NoArgsConstructor
public class Colegio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    private String telefono;
    private String email;

    @Column(unique = true, nullable = false, length = 50)
    private String cue;

    // TODO: Cuando crees la entidad Nivel, descomenta esto
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "nivel_id")
    // private Nivel nivel;
}
