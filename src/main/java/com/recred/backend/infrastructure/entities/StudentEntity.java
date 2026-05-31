package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.TimeRestriction;
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
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "grado_id")
    private GradeEntity gradeEntity;

    @ManyToMany(mappedBy = "children", fetch = LAZY)
    private Set<TutorEntity> tutors = new HashSet<>();

    @OneToOne(mappedBy = "alumnoEntity", cascade = CascadeType.ALL, fetch = LAZY)
    private WalletEntity walletEntity;

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "alumno_restricciones_salud",
            joinColumns = @JoinColumn(name = "alumno_id"),
            inverseJoinColumns = @JoinColumn(name = "clasificacion_salud_id")
    )
    private Set<HealthClassificationEntity> healthRestrictions = new HashSet<>();

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "productos_favoritos",
            joinColumns = @JoinColumn(name = "alumno_id"),
            inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private Set<ProductEntity> favouriteProducts = new HashSet<>();

    @OneToMany(mappedBy = "alumnoEntity",
            cascade = CascadeType.ALL, orphanRemoval = true, fetch = LAZY)
    private Set<TimeRestrictionEntity> timeRestrictions = new HashSet<>();

    public Student toDomain() {
        Student studentDominio = new Student();

        studentDominio.setId(this.id);
        studentDominio.setActive(this.active);

        if (this.healthRestrictions != null && !this.healthRestrictions.isEmpty()) {
            Set<HealthClassification> restriccionesDominio = this.healthRestrictions.stream()
                    .map(entidadSalud -> {
                        HealthClassification clasificacion = new HealthClassification();
                        clasificacion.setId(entidadSalud.getId());
                        clasificacion.setDescription(entidadSalud.getDescription());
                        clasificacion.setActive(entidadSalud.getActive());
                        return clasificacion;
                    })
                    .collect(Collectors.toSet());

            studentDominio.setHealthRestrictions(restriccionesDominio);
        } else {
            studentDominio.setHealthRestrictions(new HashSet<>());
        }

        if (this.timeRestrictions != null) {
            Set<TimeRestriction> horariasDominio = this.timeRestrictions.stream()
                    .map(entity -> {
                        TimeRestriction rh = new TimeRestriction();
                        rh.setId(entity.getId());
                        return rh;
                    })
                    .collect(Collectors.toSet());
            studentDominio.setTimeRestrictions(horariasDominio);
        }

        return studentDominio;
    }

    public static StudentEntity fromDomain(Student student) {
        if (student == null) {
            return null;
        }

        StudentEntity entity = new StudentEntity();

        entity.setId(student.getId());
        entity.setActive(student.getActive() != null ? student.getActive() : true);

        if (student.getGrade() != null && student.getGrade().getId() != null) {
            GradeEntity gradoProxy = new GradeEntity();
            gradoProxy.setId(student.getGrade().getId());
            entity.setGradeEntity(gradoProxy);
        }

        if (student.getWallet() != null) {

            WalletEntity walletEntity = WalletEntity.fromDomain(student.getWallet());
            if (walletEntity != null) {
                walletEntity.setStudentEntity(entity);
                entity.setWalletEntity(walletEntity);
            }
        }

        if (student.getHealthRestrictions() != null && !student.getHealthRestrictions().isEmpty()) {
            Set<HealthClassificationEntity> saludProxies = student.getHealthRestrictions().stream()
                    .filter(rs -> rs.getId() != null)
                    .map(rs -> {
                        HealthClassificationEntity proxy = new HealthClassificationEntity();
                        proxy.setId(rs.getId());
                        return proxy;
                    })
                    .collect(Collectors.toSet());
            entity.setHealthRestrictions(saludProxies);
        }

        if (student.getFavouriteProducts() != null && !student.getFavouriteProducts().isEmpty()) {
            Set<ProductEntity> productosProxies = student.getFavouriteProducts().stream()
                    .filter(p -> p.getId() != null)
                    .map(p -> {
                        ProductEntity proxy = new ProductEntity();
                        proxy.setId(p.getId());
                        return proxy;
                    })
                    .collect(Collectors.toSet());
            entity.setFavouriteProducts(productosProxies);
        }

        if (student.getTimeRestrictions() != null && !student.
                getTimeRestrictions().isEmpty()) {
            Set<TimeRestrictionEntity> horariasEntities = student.
                    getTimeRestrictions().stream()
                    .map(rh -> {

                        TimeRestrictionEntity rhEntity = new TimeRestrictionEntity();
                        rhEntity.setId(rh.getId());
                        rhEntity.setStudentEntity(entity);
                        return rhEntity;
                    })
                    .collect(Collectors.toSet());
            entity.setTimeRestrictions(horariasEntities);
        }

        return entity;
    }
}