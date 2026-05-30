package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.Colegio;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "colegios")
@Getter
@Setter
@NoArgsConstructor
public class ColegioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    private String telefono;

    private String email;

    @Column(unique = true, nullable = false, length = 50)
    private String cue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id")
    private NivelEntity nivelEntity;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public Colegio toDomain() {
        Colegio colegio = new Colegio();

        colegio.setId(this.id);
        colegio.setNombre(this.nombre);
        colegio.setLatitud(this.latitud);
        colegio.setLongitud(this.longitud);
        colegio.setTelefono(this.telefono);
        colegio.setEmail(this.email);
        colegio.setCue(this.cue);
        colegio.setActivo(this.activo);
        colegio.setCreadoEn(this.creadoEn);
        colegio.setActualizadoEn(this.actualizadoEn);

        if (this.nivelEntity != null) {
            colegio.setNivel(this.nivelEntity.toDomain());
        }

        return colegio;
    }
}
