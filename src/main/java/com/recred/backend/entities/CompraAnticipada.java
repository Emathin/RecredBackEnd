package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("ANTICIPADA")
@Getter
@Setter
@NoArgsConstructor
public class CompraAnticipada extends Compra {

    @Column(name = "codigo_verificacion", length = 10)
    private String codigoVerificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franja_retiro_id")
    private FranjaHoraria franjaRetiro;

    @Column(name = "fecha_retiro_programada")
    private LocalDateTime fechaRetiroProgramada;

    @Column(name = "fecha_retiro_efectivo")
    private LocalDateTime fechaRetiroEfectivo;
}