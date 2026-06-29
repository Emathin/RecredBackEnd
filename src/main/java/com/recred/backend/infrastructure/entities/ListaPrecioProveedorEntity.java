package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.ListaPrecioProveedor;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "listas_precio_proveedor")
@Getter
@Setter
@NoArgsConstructor
public class ListaPrecioProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private ProveedorEntity proveedor;

    @Column(name = "url_archivo", nullable = false, length = 512)
    private String urlArchivo;

    @Column(name = "nombre_original", nullable = false, length = 256)
    private String nombreOriginal;

    @Column(nullable = false)
    private boolean activa = true;

    @CreationTimestamp
    @Column(name = "fecha_carga", updatable = false)
    private LocalDateTime fechaCarga;

    @OneToMany(mappedBy = "listaPrecio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemListaPrecioProveedorEntity> items = new ArrayList<>();

    public static ListaPrecioProveedorEntity fromDomain(ListaPrecioProveedor domain) {
        if (domain == null) {
            return null;
        }
        ListaPrecioProveedorEntity entity = new ListaPrecioProveedorEntity();
        entity.setId(domain.getId());
        entity.setUrlArchivo(domain.getUrlArchivo());
        entity.setNombreOriginal(domain.getNombreOriginal());
        entity.setActiva(domain.isActiva());
        entity.setFechaCarga(domain.getCreadoEn());

        if (domain.getProveedorId() != null) {
            ProveedorEntity proveedorProxy = new ProveedorEntity();
            proveedorProxy.setId(domain.getProveedorId());
            entity.setProveedor(proveedorProxy);
        }

        if (domain.getItems() != null) {
            entity.setItems(domain.getItems().stream()
                    .map(item -> {
                        ItemListaPrecioProveedorEntity itemEntity =
                                ItemListaPrecioProveedorEntity.fromDomain(item);
                        if (itemEntity != null) {
                            itemEntity.setListaPrecio(entity);
                        }
                        return itemEntity;
                    })
                    .collect(Collectors.toList()));
        }
        return entity;
    }

    public ListaPrecioProveedor toDomain() {
        ListaPrecioProveedor domain = new ListaPrecioProveedor();
        domain.setId(this.id);
        domain.setUrlArchivo(this.urlArchivo);
        domain.setNombreOriginal(this.nombreOriginal);
        domain.setActiva(this.activa);
        domain.setCreadoEn(this.fechaCarga);

        if (this.proveedor != null) {
            domain.setProveedorId(this.proveedor.getId());
        }

        if (this.items != null) {
            domain.setItems(this.items.stream()
                    .map(ItemListaPrecioProveedorEntity::toDomain)
                    .collect(Collectors.toList()));
        }
        return domain;
    }
}
