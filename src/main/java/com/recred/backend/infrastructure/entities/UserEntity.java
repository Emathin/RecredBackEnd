package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.UserRole;
import com.recred.backend.domain.models.User;
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
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id")
    private SchoolEntity schoolEntity;

    @Column(name = "cognito_sub", unique = true, nullable = false)
    private String idCognito;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false)
    private String email;

    private String telephone;

    private String dni;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole rol;

    @Column(nullable = false)
    private Boolean checkedByCognito = false;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "actualizado_en", updatable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private StudentEntity studentEntity;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TutorEntity tutor;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MerchantEntity merchant;

    public void setKiosqueroEntity(MerchantEntity merchantEntity) {
        this.merchant = merchantEntity;
        if (merchantEntity != null) {
            merchantEntity.setUser(this);
        }
    }

    public void setStudentEntity(StudentEntity studentEntity) {
        this.studentEntity = studentEntity;
        if (studentEntity != null) {
            studentEntity.setUser(this);
        }
    }

    public void setTutorEntity(TutorEntity tutorEntity) {
        this.tutor = tutorEntity;
        if (tutorEntity != null) {
            tutorEntity.setUser(this);
        }
    }

    public User toDomain() {
        User user = new User();

        user.setId(this.id);
        user.setIdCognito(this.idCognito);
        user.setName(this.name);
        user.setSurname(this.surname);
        user.setEmail(this.email);
        user.setTelephone(this.telephone);
        user.setDni(this.dni);
        user.setRole(this.rol);
        user.setCheckedByCognito(this.checkedByCognito);
        user.setActive(this.active);
        user.setCreatedAt(this.createdAt);
        user.setUpdatedAt(this.updatedAt);

        if (this.schoolEntity != null) {
            user.setSchool(this.schoolEntity.toDomain());
        }

        if (this.studentEntity != null) {
            user.setStudent(this.studentEntity.toDomain());
        }

        if (this.tutor != null) {
            user.setTutor(this.tutor.toDomain());
        }

        if (this.merchant != null) {
            user.setMerchant(this.merchant.toDomain());
        }

        return user;
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

        if (student.getWallet() != null) {
            WalletEntity walletEntity = WalletEntity.fromDomain(student.getWallet());
            walletEntity.setStudentEntity(entity);
            entity.setWalletEntity(walletEntity);
        }

        return entity;
    }
}
