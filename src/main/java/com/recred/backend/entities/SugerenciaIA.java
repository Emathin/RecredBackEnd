package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sugerencias_ia")
@Getter
@Setter
@NoArgsConstructor
public class SugerenciaIA {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String justificacion;

    // Puede apuntar a un producto específico para recomendar o estar nulo si es una sugerencia general
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_sugerido_id")
    private Producto productoSugerido;

    @Column(name = "es_saludable", nullable = false)
    private Boolean esSaludable = false;

    // Ej: PENDIENTE, ACEPTADA, IGNORADA
    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}