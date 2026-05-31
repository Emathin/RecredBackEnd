package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.School;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "colegios")
@Getter
@Setter
@NoArgsConstructor
public class SchoolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    private String telephone;

    private String email;

    @Column(unique = true, nullable = false, length = 50)
    private String cue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id")
    private LevelEntity levelEntity;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;

    public School toDomain() {
        School school = new School();

        school.setId(this.id);
        school.setName(this.name);
        school.setLatitud(this.latitud);
        school.setLongitud(this.longitud);
        school.setTelephone(this.telephone);
        school.setEmail(this.email);
        school.setCue(this.cue);
        school.setActive(this.active);
        school.setCreatedAt(this.createdAt);
        school.setUpdatedAt(this.updatedAt);

        if (this.levelEntity != null) {
            school.setLevel(this.levelEntity.toDomain());
        }

        return school;
    }
}
