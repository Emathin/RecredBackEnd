package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "items_lista_precio_proveedor")
@Getter
@Setter
@NoArgsConstructor
public class ItemListaPrecioProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_id", nullable = false)
    private ListaPrecioProveedorEntity listaPrecio;

    @Column(name = "nombre_producto_proveedor", nullable = false, length = 300)
    private String nombreProductoProveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_inventario_id")
    private ProductoEntity productoInventario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(length = 100)
    private String unidad;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "mapping_confirmado", nullable = false)
    private boolean mappingConfirmado = false;

    public static ItemListaPrecioProveedorEntity fromDomain(ItemListaPrecioProveedor domain) {
        if (domain == null) {
            return null;
        }
        ItemListaPrecioProveedorEntity entity = new ItemListaPrecioProveedorEntity();
        entity.setId(domain.getId());
        entity.setNombreProductoProveedor(domain.getNombreProductoProveedor());
        entity.setPrecio(domain.getPrecio());
        entity.setUnidad(domain.getUnidad());
        entity.setNotas(domain.getNotas());
        entity.setMappingConfirmado(domain.isMappingConfirmado());

        if (domain.getListaPrecioId() != null) {
            ListaPrecioProveedorEntity proxy = new ListaPrecioProveedorEntity();
            proxy.setId(domain.getListaPrecioId());
            entity.setListaPrecio(proxy);
        }

        if (domain.getProductoInventarioId() != null) {
            ProductoEntity proxy = new ProductoEntity();
            proxy.setId(domain.getProductoInventarioId());
            entity.setProductoInventario(proxy);
        }
        return entity;
    }

    public ItemListaPrecioProveedor toDomain() {
        ItemListaPrecioProveedor domain = new ItemListaPrecioProveedor();
        domain.setId(this.id);
        domain.setNombreProductoProveedor(this.nombreProductoProveedor);
        domain.setPrecio(this.precio);
        domain.setUnidad(this.unidad);
        domain.setNotas(this.notas);
        domain.setMappingConfirmado(this.mappingConfirmado);

        if (this.listaPrecio != null) {
            domain.setListaPrecioId(this.listaPrecio.getId());
        }

        if (this.productoInventario != null) {
            domain.setProductoInventarioId(this.productoInventario.getId());
            domain.setNombreProductoInventario(this.productoInventario.getNombre());
        }
        return domain;
    }
}
