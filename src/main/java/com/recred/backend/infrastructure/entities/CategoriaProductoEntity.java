package com.recred.backend.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "categorias_producto")
@Getter
@Setter
@NoArgsConstructor
public class CategoriaProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;
}
