package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "billeteras")
@Getter
@Setter
@NoArgsConstructor
public class Billetera {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false, unique = true)
    private Alumno alumno;

    @Column(name = "saldo_actual", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @Column(name = "limite_diario_global", nullable = false, precision = 12, scale = 2)
    private BigDecimal limiteDiarioGlobal;

    @Column(name = "monto_base_periodo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoBasePeriodo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_periodo", nullable = false)
    private TipoPeriodo tipoPeriodo;

    @Column(name = "limite_descubierto_permitido", nullable = false, precision = 12, scale = 2)
    private BigDecimal limiteDescubiertoPermitido = BigDecimal.ZERO;

    @Column(name = "saldo_deuda", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoDeuda = BigDecimal.ZERO;
}