package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.InventarioRepository;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InventarioRepositoryImpl implements InventarioRepository {

    private final SpringDataInventarioRepository springDataRepository;

    public InventarioRepositoryImpl(SpringDataInventarioRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<Producto> findAvailableProductsByBuffet(UUID buffetId) {
        return springDataRepository.findAvailableByBuffetId(buffetId).stream()
                .map(InventarioBuffetEntity::getProductoEntity)
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Producto toDomain(ProductoEntity entity) {
        Producto producto = new Producto();
        producto.setId(entity.getId());
        producto.setNombre(entity.getNombre());
        
        if (entity.getCategoria() != null) {
            com.recred.backend.domain.models.CategoriaProducto cat = 
                    new com.recred.backend.domain.models.CategoriaProducto();
            cat.setId(entity.getCategoria().getId());
            cat.setDescripcion(entity.getCategoria().getDescripcion());
            producto.setCategoria(cat);
        }
        return producto;
    }
}
