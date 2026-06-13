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
    @JoinColumn(name = "vendedor_id", nullable = false)
    private KiosqueroEntity vendedor;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_vencimiento_habilitacion")
    private LocalDate fechaVencimientoHabilitacion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "permitir_ventas_fin_de_semana", nullable = false, 
            columnDefinition = "boolean default false")
    private Boolean permitirVentasFinDeSemana = false;

    @Column(name = "permitir_ventas_feriados", nullable = false, 
            columnDefinition = "boolean default false")
    private Boolean permitirVentasFeriados = false;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public Buffet toDomain() {
        Buffet buffet = new Buffet();

        buffet.setId(this.id);
        buffet.setNombre(this.nombre);
        buffet.setFechaVencimientoHabilitacion(this.fechaVencimientoHabilitacion);
        buffet.setActivo(this.activo);
        buffet.setPermitirVentasFinDeSemana(this.permitirVentasFinDeSemana);
        buffet.setPermitirVentasFeriados(this.permitirVentasFeriados);
        buffet.setCreadoEn(this.creadoEn);
        buffet.setActualizadoEn(this.actualizadoEn);

        if (this.colegioEntity != null) {
            buffet.setColegio(this.colegioEntity.toDomain());
        }

        if (this.vendedor != null) {

            Kiosquero kiosqueroDominio = new Kiosquero();
            kiosqueroDominio.setId(this.vendedor.getId());
            kiosqueroDominio.setCuit(this.vendedor.getCuit());

            buffet.setVendedor(kiosqueroDominio);
        }

        return buffet;
    }
}