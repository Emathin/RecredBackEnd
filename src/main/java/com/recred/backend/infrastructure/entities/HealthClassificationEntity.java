package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.HealthClassification;
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
@Table(name = "clasificaciones_salud")
@Getter
@Setter
@NoArgsConstructor
public class HealthClassificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    public HealthClassification toDomain() {
        HealthClassification modelo = new HealthClassification();
        modelo.setId(this.id);
        modelo.setDescription(this.description);
        modelo.setActive(this.active);
        return modelo;
    }
}