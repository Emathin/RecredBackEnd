package com.recred.backend.dominio.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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