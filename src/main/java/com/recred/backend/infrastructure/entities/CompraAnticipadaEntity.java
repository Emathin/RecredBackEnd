package com.recred.backend.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("ANTICIPADA")
@Getter
@Setter
@NoArgsConstructor
public class CompraAnticipadaEntity extends CompraEntity {

    @Column(name = "codigo_verificacion", length = 50)
    private String codigoVerificacion;

    @OneToOne(mappedBy = "compraEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private RetiroProgramadoEntity retiroProgramadoEntity;

}
