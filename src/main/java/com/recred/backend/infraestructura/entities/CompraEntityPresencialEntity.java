package com.recred.backend.infraestructura.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("PRESENCIAL")
@Getter
@Setter
@NoArgsConstructor
public class CompraEntityPresencialEntity extends CompraEntity {
}