package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.ClasificacionTipo;
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
@Table(name = "clasificaciones_tipo")
@Getter
@Setter
@NoArgsConstructor
public class ClasificacionTipoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    public ClasificacionTipo toDomain() {
        ClasificacionTipo dominio = new ClasificacionTipo();

        dominio.setId(this.id);
        dominio.setDescripcion(this.descripcion);
        dominio.setActivo(this.activo);

        return dominio;
    }
}