package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.Nivel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "niveles")
@Getter
@Setter
@NoArgsConstructor
public class NivelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    public Nivel toDomain() {
        Nivel nivel = new Nivel();

        nivel.setId(this.id);
        nivel.setDescripcion(this.descripcion);
        nivel.setActivo(this.activo);

        return nivel;
    }
}
