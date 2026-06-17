package com.recred.backend.unit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductAdditionNotificationRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.persistence.ProductStatisticsJpaRepository;
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

    @Mock
    private ProductAdditionNotificationRepository notificationRepository;

    @Mock
    private ProductStatisticsJpaRepository productStatisticsJpaRepository;

    @Mock
    private SugerenciaIARepository sugerenciaIARepository;

    @Mock
    private ObjectMapper objectMapper;

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
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        kiosquero.setUsuario(usuario);

        buffetId = UUID.randomUUID();
    }

    @Test
    void WhenProductIsNotInStockShouldRegisterRecommendationAndSendNotifications() {
        when(productRepository.findBestSoldProducts()).thenReturn(List.of(producto));
        when(kiosqueroRepository.findAll()).thenReturn(List.of(kiosquero));
        when(kiosqueroRepository.findBuffetById(kiosquero.getId())).thenReturn(buffetId);
        when(inventoryRepository.checkIfProductIsInStock(producto.getId(), buffetId)).thenReturn(false);

        useCase.execute();

        verify(aiEventRepository).registerProductAdditionRecommendation(producto, kiosquero, buffetId);
        verify(notificationRepository).save(any(Producto.class), any(Kiosquero.class), any(SugerenciaIA.class));
    }

    @Test
    void WhenProductAlreadyInStock_ShouldNotRegisterRecommendation() {
        when(productRepository.findBestSoldProducts()).thenReturn(List.of(producto));
        when(kiosqueroRepository.findAll()).thenReturn(List.of(kiosquero));
        when(kiosqueroRepository.findBuffetById(kiosquero.getId())).thenReturn(buffetId);
        when(inventoryRepository.checkIfProductIsInStock(producto.getId(), buffetId)).thenReturn(true);

        useCase.execute();

        verify(aiEventRepository, never()).registerProductAdditionRecommendation(any(), any(), any());
        verify(notificationRepository, never()).save(any(), any(), any());
    }

}
