package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("REEMBOLSO")
@Getter
@Setter
@NoArgsConstructor
public class Reembolso extends Movimiento {

    @Column(name = "motivo_reembolso")
    private String motivoReembolso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_original_id")
    private Compra compraOriginal;
}