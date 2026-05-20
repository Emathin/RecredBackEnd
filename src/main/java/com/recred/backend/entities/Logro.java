package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "logros")
@Getter
@Setter
@NoArgsConstructor
public class Logro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String titulo;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "puntos_recompensa", nullable = false)
    private Integer puntosRecompensa = 0;

    @Column(name = "url_icono")
    private String urlIcono;
}