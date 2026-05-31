package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.IaSuggestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;
import static org.hibernate.type.SqlTypes.JSON;

@Entity
@Table(name = "sugerencias_ia")
@Getter
@Setter
@NoArgsConstructor
public class AISuggestionEntity {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @Enumerated(STRING)
    @Column(nullable = false)
    private IaSuggestionType type;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "alumno_id")
    private StudentEntity studentEntity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "buffet_id")
    private BuffetEntity buffetEntity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "producto_id")
    private ProductEntity productEntity;

    @Column(nullable = false)
    private String tittle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @JdbcTypeCode(JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column
    private Boolean accepted;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "respondida_en")
    private LocalDateTime answeredAt;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "evento_ia_id")
    private AIEventEntity eventoIaEntity;
}
