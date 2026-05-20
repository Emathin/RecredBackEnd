package com.recred.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagos_mercado_pago")
@Getter
@Setter
@NoArgsConstructor
public class PagoMercadoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // La restricción unique=true es la clave para la idempotencia del webhook
    @Column(name = "payment_id", unique = true, nullable = false)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billetera_id", nullable = false)
    private Billetera billetera;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    // Guardará estados nativos de MP como "approved", "pending", "rejected"
    @Column(nullable = false, length = 50)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}