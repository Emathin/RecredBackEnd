package com.recred.backend.infrastructure.entities;


import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Kiosquero;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buffets")
@Getter
@Setter
@NoArgsConstructor
public class BuffetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false)
    private ColegioEntity colegioEntity;

    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "vendedor_id", nullable = true)
    private KiosqueroEntity vendedor;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_vencimiento_habilitacion")
    private LocalDate fechaVencimientoHabilitacion;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public static BuffetEntity fromDomain(Buffet buffet) {
        if (buffet == null) {
            return null;
        }

        BuffetEntity entity = new BuffetEntity();
        entity.setId(buffet.getId());
        entity.setNombre(buffet.getNombre());
        entity.setFechaVencimientoHabilitacion(buffet.getFechaVencimientoHabilitacion());
        entity.setActivo(buffet.getActivo());
        entity.setCreadoEn(buffet.getCreadoEn());
        entity.setActualizadoEn(buffet.getActualizadoEn());

        if (buffet.getColegio() != null && buffet.getColegio().getId() != null) {
            ColegioEntity colegioProxy = new ColegioEntity();
            colegioProxy.setId(buffet.getColegio().getId());
            entity.setColegioEntity(colegioProxy);
        }

        if (buffet.getVendedor() != null && buffet.getVendedor().getId() != null) {
            KiosqueroEntity kiosqueroProxy = new KiosqueroEntity();
            kiosqueroProxy.setId(buffet.getVendedor().getId());
            entity.setVendedor(kiosqueroProxy);
        }

        return entity;
    }

    public Buffet toDomain() {
        Buffet buffet = new Buffet();

        buffet.setId(this.id);
        buffet.setNombre(this.nombre);
        buffet.setFechaVencimientoHabilitacion(this.fechaVencimientoHabilitacion);
        buffet.setActivo(this.activo);
        buffet.setCreadoEn(this.creadoEn);
        buffet.setActualizadoEn(this.actualizadoEn);

        if (this.colegioEntity != null
                && org.hibernate.Hibernate.isInitialized(this.colegioEntity)) {
            buffet.setColegio(this.colegioEntity.toDomain());
        }

        if (this.vendedor != null
                && org.hibernate.Hibernate.isInitialized(this.vendedor)) {

            Kiosquero kiosqueroDominio = new Kiosquero();
            kiosqueroDominio.setId(this.vendedor.getId());
            kiosqueroDominio.setCuit(this.vendedor.getCuit());

            if (this.vendedor.getUsuario() != null
                    && org.hibernate.Hibernate.isInitialized(this.vendedor.getUsuario())) {
                kiosqueroDominio.setUsuario(this.vendedor.getUsuario().toDomain());
            }

            buffet.setVendedor(kiosqueroDominio);
        }

        return buffet;
    }
}