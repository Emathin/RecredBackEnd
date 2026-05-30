package com.recred.backend.application;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddProductRecommendationTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private AIEventRepository aiEventRepository;

    @InjectMocks
    private AddProductRecommendation useCase;

    private Producto producto;
    private Kiosquero kiosquero;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Producto Test");

        kiosquero = new Kiosquero();
        kiosquero.setId(UUID.randomUUID());

        buffetId = UUID.randomUUID();
    }

    @Test
    void WhenProductIsNotInStockShouldRegisterRecommendation() {
        when(productRepository.findBestSoldProducts()).thenReturn(List.of(producto));
        when(kiosqueroRepository.findAll()).thenReturn(List.of(kiosquero));
        when(kiosqueroRepository.findBuffetById(kiosquero.getId())).thenReturn(buffetId);
        when(inventoryRepository.checkIfProductIsInStock(producto.getId(), buffetId)).thenReturn(false);

        useCase.execute();

        verify(aiEventRepository).registerProductAdditionRecommendation(producto, kiosquero, buffetId);
    }

    @Test
    void WhenProductAlreadyInStock_ShouldNotRegisterRecommendation() {
        when(productRepository.findBestSoldProducts()).thenReturn(List.of(producto));
        when(kiosqueroRepository.findAll()).thenReturn(List.of(kiosquero));
        when(kiosqueroRepository.findBuffetById(kiosquero.getId())).thenReturn(buffetId);
        when(inventoryRepository.checkIfProductIsInStock(producto.getId(), buffetId)).thenReturn(true);

        useCase.execute();

        verify(aiEventRepository, never()).registerProductAdditionRecommendation(any(), any(), any());
    }

    @Mock
    private org.mockito.MockedStatic<UUID> mockedUuid;

}
