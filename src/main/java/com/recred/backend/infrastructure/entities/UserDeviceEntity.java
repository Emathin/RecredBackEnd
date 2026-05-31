package com.recred.backend.infrastructure.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "dispositivos_usuario")
@Getter
@Setter
@NoArgsConstructor
public class UserDeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserEntity userEntity;

    @Column(name = "fcm_token", unique = true, nullable = false)
    private String fcmToken;

    @Column(name = "sistema_operativo")
    private String operativeSystem;

    @Column(name = "ultimo_acceso_en")
    private LocalDateTime lastAccessAt;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}