package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.Proveedor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class ProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiosquero_id", nullable = false)
    private KiosqueroEntity kiosquero;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 100)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(name = "dias_visita", length = 200)
    private String diasVisita;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListaPrecioProveedorEntity> listasPrecios = new ArrayList<>();

    public static ProveedorEntity fromDomain(Proveedor domain) {
        if (domain == null) {
            return null;
        }
        ProveedorEntity entity = new ProveedorEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setTelefono(domain.getTelefono());
        entity.setEmail(domain.getEmail());
        entity.setDiasVisita(domain.getDiasVisita());
        entity.setNotas(domain.getNotas());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());

        if (domain.getKiosqueroId() != null) {
            KiosqueroEntity kiosqueroProxy = new KiosqueroEntity();
            kiosqueroProxy.setId(domain.getKiosqueroId());
            entity.setKiosquero(kiosqueroProxy);
        }

        if (domain.getListasPrecios() != null) {
            entity.setListasPrecios(domain.getListasPrecios().stream()
                    .map(item -> {
                        ListaPrecioProveedorEntity itemEntity =
                                ListaPrecioProveedorEntity.fromDomain(item);
                        if (itemEntity != null) {
                            itemEntity.setProveedor(entity);
                        }
                        return itemEntity;
                    })
                    .collect(Collectors.toList()));
        }
        return entity;
    }

    public Proveedor toDomain() {
        Proveedor domain = new Proveedor();
        domain.setId(this.id);
        domain.setNombre(this.nombre);
        domain.setTelefono(this.telefono);
        domain.setEmail(this.email);
        domain.setDiasVisita(this.diasVisita);
        domain.setNotas(this.notas);
        domain.setCreadoEn(this.creadoEn);
        domain.setActualizadoEn(this.actualizadoEn);

        if (this.kiosquero != null) {
            domain.setKiosqueroId(this.kiosquero.getId());
        }

        if (this.listasPrecios != null) {
            domain.setListasPrecios(this.listasPrecios.stream()
                    .map(ListaPrecioProveedorEntity::toDomain)
                    .collect(Collectors.toList()));
        }
        return domain;
    }
}
