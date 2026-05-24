package com.recred.backend.infrastructure.entities;

import com.recred.backend.dominio.models.RolChatMensaje;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "chat_mensajes")
@Getter
@Setter
@NoArgsConstructor
public class ChatMensajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private ChatSesionEntity sesion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolChatMensaje rol;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();
}