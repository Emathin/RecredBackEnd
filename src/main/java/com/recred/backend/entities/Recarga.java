package com.recred.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("RECARGA")
@Getter
@Setter
@NoArgsConstructor
public class Recarga extends Movimiento {

    @Column(name = "metodo_recarga", length = 50)
    private String metodoRecarga;

    @Column(name = "referencia_pasarela", length = 100)
    private String referenciaPasarela;
}