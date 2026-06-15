package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "micro_creditos")
@Getter
@Setter
@NoArgsConstructor
public class SchoolCreditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false)
    private AlumnoEntity alumno;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditStatus status;

    @CreationTimestamp
    @Column(name = "creado_en")
    private LocalDateTime createdAt;

    public SchoolCredit toDomain() {
        return new SchoolCredit(
                this.id,
                this.alumno != null ? this.alumno.getId() : null,
                this.amount,
                this.status,
                this.createdAt
        );
    }
}
