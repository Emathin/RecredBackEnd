package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioRepositoryImplTest {

    @Mock
    private SpringDataInventarioRepository springDataRepository;

    private InventarioRepositoryImpl inventarioRepository;

    @BeforeEach
    void setUp() {
        inventarioRepository = new InventarioRepositoryImpl(springDataRepository);
    }

    @Test
    void recuperaProductosDisponibles_Correctamente_MapeandoADominio() {
        UUID buffetId = UUID.randomUUID();
        
        ProductoEntity productoEntity = new ProductoEntity();
        productoEntity.setId(UUID.randomUUID());
        productoEntity.setNombre("Jugo");
        
        CategoriaProductoEntity categoria = new CategoriaProductoEntity();
        categoria.setDescripcion("Bebidas");
        productoEntity.setCategoria(categoria);

        InventarioBuffetEntity inventario = new InventarioBuffetEntity();
        inventario.setProductoEntity(productoEntity);

        when(springDataRepository.findAvailableByBuffetId(buffetId)).thenReturn(List.of(inventario));

        List<Producto> result = inventarioRepository.findAvailableProductsByBuffet(buffetId);

        assertEquals(1, result.size());
        assertEquals("Jugo", result.get(0).getNombre());
        assertEquals("Bebidas", result.get(0).getCategoria().getDescripcion());
    }

    @Test
    void manejaCategoriaNula_Correctamente() {
        UUID buffetId = UUID.randomUUID();
        
        ProductoEntity productoEntity = new ProductoEntity();
        productoEntity.setId(UUID.randomUUID());
        productoEntity.setNombre("Fruta");
        productoEntity.setCategoria(null);

        InventarioBuffetEntity inventario = new InventarioBuffetEntity();
        inventario.setProductoEntity(productoEntity);

        when(springDataRepository.findAvailableByBuffetId(buffetId)).thenReturn(List.of(inventario));

        List<Producto> result = inventarioRepository.findAvailableProductsByBuffet(buffetId);

        assertNull(result.get(0).getCategoria());
    }
}
