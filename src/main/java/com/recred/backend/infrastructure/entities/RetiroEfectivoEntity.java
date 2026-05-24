package com.recred.backend.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "retiros_efectivos")
@Getter
@Setter
@NoArgsConstructor
public class RetiroEfectivoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retiro_programado_id", nullable = false, unique = true)
    private RetiroProgramadoEntity retiroProgramadoEntity;

    @Column(name = "fecha_retiro_efectivo", nullable = false)
    private LocalDateTime fechaRetiroEfectivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregado_por_usuario_id", nullable = false)
    private UsuarioEntity entregadoPorUsuarioEntity;
}