package com.recred.backend.entities;

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
public class CompraPresencial extends Compra {
    // Hereda todo lo necesario para una compra en mostrador
}