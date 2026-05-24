package com.recred.backend.infrastructure.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.recred.backend.dominio.models.ClasificacionSalud;

import java.util.UUID;

@Entity
@Table(name = "clasificaciones_salud")
@Getter
@Setter
@NoArgsConstructor
public class ClasificacionSaludEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    public ClasificacionSalud toDomain() {
        ClasificacionSalud modelo = new ClasificacionSalud();
        modelo.setId(this.id);
        modelo.setDescripcion(this.descripcion);
        modelo.setActivo(this.activo);
        return modelo;
    }
}