package com.recred.backend.infrastructure.entities;

import com.recred.backend.dominio.models.EstadoPagoExterno;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pagos_mercado_pago")
@Getter
@Setter
@NoArgsConstructor
public class PagoMercadoPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billetera_id", nullable = false)
    private BilleteraEntity billeteraEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuarioEntity;

    @Column(name = "mercado_pago_payment_id", unique = true)
    private String mercadoPagoPaymentId;

    @Column(name = "mercado_pago_preference_id", unique = true)
    private String mercadoPagoPreferenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPagoExterno estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 10)
    private String moneda;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_respuesta", columnDefinition = "jsonb")
    private Map<String, Object> datosRespuesta;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "aprobado_en")
    private LocalDateTime aprobadoEn;
}