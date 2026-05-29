package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.CategoriaProducto;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SaveProductDTO {
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal peso;
    private boolean requierePreparacion;
    private UUID categoriaId;  // Nuevo campo

    public Producto toModel() {
        Producto producto = new Producto();
        producto.setNombre(this.nombre);
        producto.setDescripcion(this.descripcion);
        producto.setPrecio(this.precio);
        producto.setPeso(this.peso);
        producto.setRequierePreparacion(this.requierePreparacion);
        producto.setActivo(true);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(UUID.fromString("5f1851c5-b5f2-49c1-a74a-2f1d4c480151"));
        producto.setCategoria(categoria);
        // Asignar categoría
       /* if (categoriaId != null) {
            CategoriaProducto categoria = new CategoriaProducto();
            categoria.setId(UUID.fromString("5f1851c5-b5f2-49c1-a744-2f1d4c480151"));
            producto.setCategoria(categoria);
        }*/

        return producto;
    }
}