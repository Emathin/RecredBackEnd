package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryRepositoryImplTest {

    @Mock
    private SpringDataInventoryRepository springDataRepository;

    private InventoryRepositoryImpl inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository = new InventoryRepositoryImpl(springDataRepository);
    }

    @Test
    void save_DeberiaCrearNuevaEntidad_CuandoNoExiste() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Inventario inventario = new Inventario();
        inventario.setBuffetId(buffetId);
        inventario.setProductoId(productId);
        inventario.setStockActual(10);
        inventario.setEstado(EstadoInventario.DISPONIBLE);

        when(springDataRepository.findByBuffetEntityIdAndProductoEntityId(buffetId, productId))
                .thenReturn(Optional.empty());
        when(springDataRepository.save(any(InventarioBuffetEntity.class))).thenAnswer(i -> i.getArgument(0));

        inventoryRepository.save(inventario);

        verify(springDataRepository).save(any(InventarioBuffetEntity.class));
    }

    @Test
    void save_DeberiaActualizarEntidad_CuandoYaExiste() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Inventario inventario = new Inventario();
        inventario.setBuffetId(buffetId);
        inventario.setProductoId(productId);
        inventario.setStockActual(50);

        InventarioBuffetEntity existingEntity = new InventarioBuffetEntity();
        existingEntity.setStockActual(10);
        existingEntity.setBuffetEntity(new BuffetEntity());
        existingEntity.setProductoEntity(new ProductoEntity());

        when(springDataRepository.findByBuffetEntityIdAndProductoEntityId(buffetId, productId))
                .thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(any(InventarioBuffetEntity.class))).thenAnswer(i -> i.getArgument(0));

        inventoryRepository.save(inventario);

        assertEquals(50, existingEntity.getStockActual());
    }

    @Test
    void findByBuffetAndProduct_DeberiaLlamarAlSpringData() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        inventoryRepository.findByBuffetAndProduct(buffetId, productId);

        verify(springDataRepository).findByBuffetEntityIdAndProductoEntityId(buffetId, productId);
    }
}
