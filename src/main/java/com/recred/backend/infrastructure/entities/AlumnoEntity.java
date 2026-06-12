package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RestriccionHoraria;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "alumno_productos_bloqueados",
            joinColumns = @JoinColumn(name = "alumno_id"),
            inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private Set<ProductoEntity> productosBloqueados = new HashSet<>();

    @Column(name = "puntos_recompensa", nullable = false)
    private Integer puntosRecompensa = 0;

    public Alumno toDomain() {
        Alumno alumnoDominio = new Alumno();

        alumnoDominio.setId(this.id);
        alumnoDominio.setActivo(this.activo);

        if (this.usuario != null) {
            alumnoDominio.setUsuarioId(this.usuario.getId());
            alumnoDominio.setUsuario(this.usuario.toDomain());
            alumnoDominio.setNombre(this.usuario.getNombre());
            alumnoDominio.setApellido(this.usuario.getApellido());
        }

        if (this.gradoEntity != null) {
            alumnoDominio.setGrado(this.gradoEntity.toDomain());
        }

        if (this.billeteraEntity != null) {
            alumnoDominio.setBilletera(this.billeteraEntity.toDomain());
        }

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

        if (this.productosFavoritos != null && !this.productosFavoritos.isEmpty()) {
            Set<Producto> favoritosDominio = this.productosFavoritos.stream()
                    .map(entidadProducto -> {
                        Producto producto = new Producto();
                        producto.setId(entidadProducto.getId());
                        producto.setNombre(entidadProducto.getNombre());
                        producto.setPrecio(entidadProducto.getPrecio());
                        producto.setActivo(entidadProducto.getActivo());
                        return producto;
                    })
                    .collect(Collectors.toSet());

            alumnoDominio.setProductosFavoritos(favoritosDominio);
        } else {
            alumnoDominio.setProductosFavoritos(new HashSet<>());
        }

        if (this.restriccionesHorarias != null && !this.restriccionesHorarias.isEmpty()) {
            Set<RestriccionHoraria> horariasDominio = this.restriccionesHorarias.stream()
                    .map(entity -> {
                        RestriccionHoraria rh = new RestriccionHoraria();
                        rh.setId(entity.getId());
                        return rh;
                    })
                    .collect(Collectors.toSet());
            alumnoDominio.setRestriccionesHorarias(horariasDominio);
        } else {
            alumnoDominio.setRestriccionesHorarias(new HashSet<>());
        }

        alumnoDominio.setTutores(new HashSet<>());

        if (this.productosBloqueados != null && !this.productosBloqueados.isEmpty()) {
            Set<Producto> bloqueadosDominio = this.productosBloqueados.stream()
                    .map(entidadProducto -> {
                        Producto producto = new Producto();
                        producto.setId(entidadProducto.getId());
                        producto.setNombre(entidadProducto.getNombre());
                        producto.setPrecio(entidadProducto.getPrecio());
                        producto.setActivo(entidadProducto.getActivo());
                        return producto;
                    })
                    .collect(Collectors.toSet());
            alumnoDominio.setProductosBloqueados(bloqueadosDominio);
        } else {
            alumnoDominio.setProductosBloqueados(new HashSet<>());
        }

        alumnoDominio.setPuntosRecompensa(this.puntosRecompensa != null
                ? this.puntosRecompensa : 0);

        return alumnoDominio;
    }

    public static AlumnoEntity fromDomain(Alumno alumno) {
        if (alumno == null) {
            return null;
        }

        AlumnoEntity entity = new AlumnoEntity();

        entity.setId(alumno.getId());
        entity.setActivo(alumno.getActivo() != null ? alumno.getActivo() : true);

        if (alumno.getGrado() != null && alumno.getGrado().getId() != null) {
            GradoEntity gradoProxy = new GradoEntity();
            gradoProxy.setId(alumno.getGrado().getId());
            entity.setGradoEntity(gradoProxy);
        }

        if (alumno.getBilletera() != null) {

            BilleteraEntity billeteraEntity = BilleteraEntity.fromDomain(alumno.getBilletera());
            if (billeteraEntity != null) {
                billeteraEntity.setAlumnoEntity(entity);
                entity.setBilleteraEntity(billeteraEntity);
            }
        }

        if (alumno.getRestriccionesSalud() != null && !alumno.getRestriccionesSalud().isEmpty()) {
            Set<ClasificacionSaludEntity> saludProxies = alumno.getRestriccionesSalud().stream()
                    .filter(rs -> rs.getId() != null)
                    .map(rs -> {
                        ClasificacionSaludEntity proxy = new ClasificacionSaludEntity();
                        proxy.setId(rs.getId());
                        return proxy;
                    })
                    .collect(Collectors.toSet());
            entity.setRestriccionesSalud(saludProxies);
        }

        if (alumno.getProductosFavoritos() != null && !alumno.getProductosFavoritos().isEmpty()) {
            Set<ProductoEntity> productosProxies = alumno.getProductosFavoritos().stream()
                    .filter(p -> p.getId() != null)
                    .map(p -> {
                        ProductoEntity proxy = new ProductoEntity();
                        proxy.setId(p.getId());
                        return proxy;
                    })
                    .collect(Collectors.toSet());
            entity.setProductosFavoritos(productosProxies);
        }

        if (alumno.getRestriccionesHorarias() != null && !alumno.
                getRestriccionesHorarias().isEmpty()) {
            Set<RestriccionHorariaEntity> horariasEntities = alumno.
                    getRestriccionesHorarias().stream()
                    .map(rh -> {

                        RestriccionHorariaEntity rhEntity = new RestriccionHorariaEntity();
                        rhEntity.setId(rh.getId());
                        rhEntity.setAlumnoEntity(entity);
                        return rhEntity;
                    })
                    .collect(Collectors.toSet());
            entity.setRestriccionesHorarias(horariasEntities);
        }

        if (alumno.getProductosBloqueados() != null && !alumno.getProductosBloqueados().isEmpty()) {
            Set<ProductoEntity> bloqueadosProxies = alumno.getProductosBloqueados().stream()
                    .filter(p -> p.getId() != null)
                    .map(p -> {
                        ProductoEntity proxy = new ProductoEntity();
                        proxy.setId(p.getId());
                        return proxy;
                    })
                    .collect(Collectors.toSet());
            entity.setProductosBloqueados(bloqueadosProxies);
        }

        entity.setPuntosRecompensa(alumno.getPuntosRecompensa() != null
                ? alumno.getPuntosRecompensa() : 0);

        return entity;
    }
}
