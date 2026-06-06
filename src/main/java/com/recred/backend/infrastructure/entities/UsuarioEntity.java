package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id")
    private ColegioEntity colegioEntity;

    @Column(name = "cognito_sub", unique = true, nullable = false)
    private String idCognito;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false)
    private String email;

    private String telefono;

    private String dni;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(nullable = false)
    private Boolean autenticadoConCognito = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", updatable = false)
    private LocalDateTime actualizadoEn;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AlumnoEntity alumnoEntity;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TutorEntity tutor;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private KiosqueroEntity kiosquero;

    public void setKiosqueroEntity(KiosqueroEntity kiosqueroEntity) {
        this.kiosquero = kiosqueroEntity;
        if (kiosqueroEntity != null) {
            kiosqueroEntity.setUsuario(this);
        }
    }

    public void setAlumnoEntity(AlumnoEntity alumnoEntity) {
        this.alumnoEntity = alumnoEntity;
        if (alumnoEntity != null) {
            alumnoEntity.setUsuario(this);
        }
    }

    public void setTutorEntity(TutorEntity tutorEntity) {
        this.tutor = tutorEntity;
        if (tutorEntity != null) {
            tutorEntity.setUsuario(this);
        }
    }

    public Usuario toDomain() {
        Usuario usuario = new Usuario();

        usuario.setId(this.id);
        usuario.setIdCognito(this.idCognito);
        usuario.setNombre(this.nombre);
        usuario.setApellido(this.apellido);
        usuario.setEmail(this.email);
        usuario.setTelefono(this.telefono);
        usuario.setDni(this.dni);
        usuario.setRol(this.rol);
        usuario.setAutenticadoConCognito(this.autenticadoConCognito);
        usuario.setActivo(this.activo);
        usuario.setCreadoEn(this.creadoEn);
        usuario.setActualizadoEn(this.actualizadoEn);

        if (this.colegioEntity != null) {
            usuario.setColegio(this.colegioEntity.toDomain());
        }

        return usuario;
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

        if (alumno.getBilletera() != null) {
            BilleteraEntity billeteraEntity = BilleteraEntity.fromDomain(alumno.getBilletera());
            billeteraEntity.setAlumnoEntity(entity);
            entity.setBilleteraEntity(billeteraEntity);
        }

        return entity;
    }
}
