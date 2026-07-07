package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.Grado;
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

import java.util.UUID;

@Entity
@Table(name = "grados")
@Getter
@Setter
@NoArgsConstructor
public class GradoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private ColegioEntity colegioEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id")
    private NivelEntity nivelEntity;

    @Column(nullable = false)
    private String anio;

    @Column(nullable = false)
    private String division;

    @Column(nullable = false)
    private Boolean activo = true;

    public Grado toDomain() {
        Grado gradoDominio = new Grado();

        gradoDominio.setId(this.id);
        gradoDominio.setAnio(this.anio);
        gradoDominio.setDivision(this.division);
        gradoDominio.setActivo(this.activo);

        if (this.colegioEntity != null) {
            gradoDominio.setColegio(this.colegioEntity.toDomain());
        }

        if (this.nivelEntity != null) {
            gradoDominio.setNivel(this.nivelEntity.toDomain());
        }

        return gradoDominio;
    }
}
