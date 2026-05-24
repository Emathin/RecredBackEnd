package com.recred.backend.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("ANTICIPADA")
@Getter
@Setter
@NoArgsConstructor
public class CompraEntityAnticipadaEntity extends CompraEntity {

    @Column(name = "codigo_verificacion", length = 10)
    private String codigoVerificacion;

}