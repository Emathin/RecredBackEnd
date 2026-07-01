package com.recred.backend.unit.application;

import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductAdditionNotificationRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.ProductStatisticsRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private AIEventRepository aiEventRepository;

    @Mock
    private ProductAdditionNotificationRepository notificationRepository;

    @Mock
    private ProductStatisticsRepository productStatisticsRepository;

    @Mock
    private SugerenciaIARepository sugerenciaIARepository;

    private AddProductRecommendation useCase;

    private Producto producto;
    private Kiosquero kiosquero;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        useCase = new AddProductRecommendation(
                productRepository,
                kiosqueroRepository,
                aiEventRepository,
                notificationRepository,
                productStatisticsRepository,
                sugerenciaIARepository
        );

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
    void WhenProductIsNotInStockShouldRegisterRecommendationWithCorrectStatistics() {
        when(kiosqueroRepository.findAll()).thenReturn(List.of(kiosquero));
        when(kiosqueroRepository.findBuffetById(kiosquero.getId())).thenReturn(buffetId);
        when(productRepository.findBestSoldProductsNotInStock(buffetId))
                .thenReturn(List.of(producto));
        
        when(productStatisticsRepository.countTotalSalesByProduct(any(), any())).thenReturn(100L);
        when(productStatisticsRepository.countDistinctCustomersByProduct(any(), any())).thenReturn(50L);
        when(productStatisticsRepository.calculateTotalRevenueByProduct(any(), any()))
                .thenReturn(new java.math.BigDecimal("5000"));

        useCase.execute();

        verify(aiEventRepository).registerProductAdditionRecommendation(producto, kiosquero, buffetId);
        verify(sugerenciaIARepository).save(any(SugerenciaIA.class));
        verify(notificationRepository).save(any(Producto.class), any(Kiosquero.class), any(SugerenciaIA.class));
    }

    @Test
    void WhenProductAlreadyInStock_ShouldNotRegisterRecommendation() {
        when(kiosqueroRepository.findAll()).thenReturn(List.of(kiosquero));
        when(kiosqueroRepository.findBuffetById(kiosquero.getId())).thenReturn(buffetId);
        when(productRepository.findBestSoldProductsNotInStock(buffetId))
                .thenReturn(List.of());

        useCase.execute();

        verify(aiEventRepository, never()).registerProductAdditionRecommendation(any(), any(), any());
        verify(notificationRepository, never()).save(any(), any(), any());
    }

}
