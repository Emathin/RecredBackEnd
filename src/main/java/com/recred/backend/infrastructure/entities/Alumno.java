package com.recred.backend.infrastructure.entities;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "alumnos")
@Getter
@Setter
@NoArgsConstructor
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grado_id")
    private GradoEntity gradoEntity;

    @ManyToMany(mappedBy = "hijos", fetch = FetchType.LAZY)
    private Set<TutorEntity> tutores = new HashSet<>();

    @OneToOne(mappedBy = "alumno", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BilleteraEntity billeteraEntity;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "alumno_restricciones_salud",
            joinColumns = @JoinColumn(name = "alumno_id"),
            inverseJoinColumns = @JoinColumn(name = "clasificacion_salud_id")
    )
    private Set<ClasificacionSaludEntity> restriccionesSalud = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "productos_favoritos",
            joinColumns = @JoinColumn(name = "alumno_id"),
            inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private Set<ProductoEntity> productosFavoritos = new HashSet<>();

    /*public Alumno toDomain(AlumnoEntity entity) {
        if (entity == null) return null;

        Set<ClasificacionSalud> restriccionesDominio = entity.getRestriccionesSalud().stream()
                .map(restriccionEntity -> new ClasificacionSalud(
                        restriccionEntity.getId(),
                        restriccionEntity.getDescripcion()))
                .collect(Collectors.toSet());

        return new Alumno(entity.getId(), restriccionesDominio);
    }*/
}