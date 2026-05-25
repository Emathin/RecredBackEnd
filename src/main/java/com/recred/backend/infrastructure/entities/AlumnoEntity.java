package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.RestriccionHoraria;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "alumnos")
@Getter
@Setter
@NoArgsConstructor
public class AlumnoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "grado_id")
    private GradoEntity gradoEntity;

    @ManyToMany(mappedBy = "hijos", fetch = LAZY)
    private Set<TutorEntity> tutores = new HashSet<>();

    @OneToOne(mappedBy = "alumnoEntity", cascade = CascadeType.ALL, fetch = LAZY)
    private BilleteraEntity billeteraEntity;

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "alumno_restricciones_salud",
            joinColumns = @JoinColumn(name = "alumno_id"),
            inverseJoinColumns = @JoinColumn(name = "clasificacion_salud_id")
    )
    private Set<ClasificacionSaludEntity> restriccionesSalud = new HashSet<>();

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "productos_favoritos",
            joinColumns = @JoinColumn(name = "alumno_id"),
            inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private Set<ProductoEntity> productosFavoritos = new HashSet<>();

    @OneToMany(mappedBy = "alumnoEntity",
            cascade = CascadeType.ALL, orphanRemoval = true, fetch = LAZY)
    private Set<RestriccionHorariaEntity> restriccionesHorarias = new HashSet<>();

    public Alumno toDomain() {
        Alumno alumnoDominio = new Alumno();

        alumnoDominio.setId(this.id);
        alumnoDominio.setActivo(this.activo);

        if (this.restriccionesSalud != null && !this.restriccionesSalud.isEmpty()) {
            Set<ClasificacionSalud> restriccionesDominio = this.restriccionesSalud.stream()
                    .map(entidadSalud -> {
                        ClasificacionSalud clasificacion = new ClasificacionSalud();
                        clasificacion.setId(entidadSalud.getId());
                        clasificacion.setDescripcion(entidadSalud.getDescripcion());
                        clasificacion.setActivo(entidadSalud.getActivo());
                        return clasificacion;
                    })
                    .collect(Collectors.toSet());

            alumnoDominio.setRestriccionesSalud(restriccionesDominio);
        } else {
            alumnoDominio.setRestriccionesSalud(new HashSet<>());
        }

        if (this.restriccionesHorarias != null) {
            Set<RestriccionHoraria> horariasDominio = this.restriccionesHorarias.stream()
                    .map(entity -> {
                        RestriccionHoraria rh = new RestriccionHoraria();
                        rh.setId(entity.getId());
                        // Aquí mapeás el resto de los campos...
                        return rh;
                    })
                    .collect(Collectors.toSet());
            alumnoDominio.setRestriccionesHorarias(horariasDominio);
        }

        return alumnoDominio;
    }
}