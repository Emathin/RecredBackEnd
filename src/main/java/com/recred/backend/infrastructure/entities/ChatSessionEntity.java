package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.ChatSessionState;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_sesiones")
@Getter
@Setter
@NoArgsConstructor
public class ChatSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserEntity userEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatSessionState state;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime startDate = LocalDateTime.now();

    @Column(name = "fecha_ultima_actividad", nullable = false)
    private LocalDateTime lastActivityDate = LocalDateTime.now();
}