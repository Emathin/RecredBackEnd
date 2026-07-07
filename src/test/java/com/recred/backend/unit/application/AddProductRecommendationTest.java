package com.recred.backend.unit.application;

import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.application.AddProductRecommendationRequest;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddProductRecommendationTest {

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

    @Captor
    private ArgumentCaptor<SugerenciaIA> suggestionCaptor;

    private AddProductRecommendation addProductRecommendation;
    private AddProductRecommendationRequest addRecommendationRequest;
    private Producto product;
    private Kiosquero kiosquero;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        addProductRecommendation = new AddProductRecommendation(
                productRepository,
                kiosqueroRepository,
                aiEventRepository,
                notificationRepository,
                productStatisticsRepository,
                sugerenciaIARepository
        );
    }

    @Test
    void givenMissingProductInStock_whenAddingRecommendations_thenRegistersSuggestion() {
        givenProductRecommendationRequest();
        givenKiosqueroExists();
        givenKiosqueroBuffet();
        givenBuffetHasMissingBestSoldProduct();
        givenProductStatistics();

        whenAddProductRecommendation();

        thenProductAdditionRecommendationWasRegistered();
        thenProductSuggestionWasSaved();
        thenProductNotificationWasSent();
    }

    @Test
    void givenNoMissingProducts_whenAddingRecommendations_thenDoesNotRegisterSuggestion() {
        givenProductRecommendationRequest();
        givenKiosqueroExists();
        givenKiosqueroBuffet();
        givenBuffetHasNoMissingProducts();

        whenAddProductRecommendation();

        thenNoProductRecommendationWasRegistered();
        thenNoProductSuggestionWasSaved();
        thenNoProductNotificationWasSent();
    }

    @Test
    void givenNoKiosqueros_whenAddingRecommendations_thenDoesNotSearchProducts() {
        givenProductRecommendationRequest();
        givenNoKiosqueros();

        whenAddProductRecommendation();

        thenNoMissingProductsWereSearched();
        thenNoProductRecommendationWasRegistered();
        thenNoProductSuggestionWasSaved();
        thenNoProductNotificationWasSent();
    }

    private void givenProductRecommendationRequest() {
        addRecommendationRequest = new AddProductRecommendationRequest();
        product = product(randomUUID(), "Producto Test", new BigDecimal("100.00"));
        kiosquero = kiosquero(randomUUID());
        buffetId = randomUUID();
    }

    private void givenKiosqueroExists() {
        when(kiosqueroRepository.findAll()).thenReturn(List.of(kiosquero));
    }

    private void givenNoKiosqueros() {
        when(kiosqueroRepository.findAll()).thenReturn(List.of());
    }

    private void givenKiosqueroBuffet() {
        when(kiosqueroRepository.findBuffetById(kiosquero.getId())).thenReturn(buffetId);
    }

    private void givenBuffetHasMissingBestSoldProduct() {
        when(productRepository.findBestSoldProductsNotInStock(buffetId))
                .thenReturn(List.of(product));
    }

    private void givenBuffetHasNoMissingProducts() {
        when(productRepository.findBestSoldProductsNotInStock(buffetId))
                .thenReturn(List.of());
    }

    private void givenProductStatistics() {
        when(productStatisticsRepository.countTotalSalesByProduct(any(), any()))
                .thenReturn(100L);
        when(productStatisticsRepository.countDistinctCustomersByProduct(any(), any()))
                .thenReturn(50L);
        when(productStatisticsRepository.calculateTotalRevenueByProduct(any(), any()))
                .thenReturn(new BigDecimal("5000"));
    }

    private void whenAddProductRecommendation() {
        addProductRecommendation.execute(addRecommendationRequest);
    }

    private void thenProductAdditionRecommendationWasRegistered() {
        verify(aiEventRepository).registerProductAdditionRecommendation(
                product,
                kiosquero,
                buffetId);
    }

    private void thenProductSuggestionWasSaved() {
        verify(sugerenciaIARepository).save(suggestionCaptor.capture());
        SugerenciaIA suggestion = suggestionCaptor.getValue();

        assertEquals(TipoSugerenciaIA.AGREGAR_PRODUCTO, suggestion.getTipo());
        assertEquals(buffetId, suggestion.getBuffetId());
        assertEquals(product.getId(), suggestion.getProductoId());
        assertEquals("Sugerencia de producto", suggestion.getTitulo());
        assertEquals("Te recomendamos agregar Producto Test a tu stock",
                suggestion.getMensaje());
        assertEquals(product.getNombre(), suggestion.getMetadata().get("productName"));
        assertEquals(product.getPrecio(), suggestion.getMetadata().get("productPrice"));
        assertEquals(100L, suggestion.getMetadata().get("totalSales"));
        assertEquals(50L, suggestion.getMetadata().get("totalCustomers"));
        assertEquals(new BigDecimal("5000"), suggestion.getMetadata().get("totalRevenue"));
    }

    private void thenProductNotificationWasSent() {
        verify(notificationRepository).save(product, kiosquero, suggestionCaptor.getValue());
    }

    private void thenNoProductRecommendationWasRegistered() {
        verify(aiEventRepository, never()).registerProductAdditionRecommendation(
                any(),
                any(),
                any());
    }

    private void thenNoProductSuggestionWasSaved() {
        verify(sugerenciaIARepository, never()).save(any());
    }

    private void thenNoProductNotificationWasSent() {
        verify(notificationRepository, never()).save(any(), any(), any());
    }

    private void thenNoMissingProductsWereSearched() {
        verify(productRepository, never()).findBestSoldProductsNotInStock(any());
    }

    private Producto product(UUID productId, String name, BigDecimal price) {
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre(name);
        product.setPrecio(price);
        return product;
    }

    private Kiosquero kiosquero(UUID kiosqueroId) {
        Kiosquero kiosquero = new Kiosquero();
        kiosquero.setId(kiosqueroId);
        kiosquero.setUsuario(user(randomUUID()));
        return kiosquero;
    }

    private Usuario user(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        return user;
    }
}
