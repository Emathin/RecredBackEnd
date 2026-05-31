package com.recred.backend.application;

import com.recred.backend.domain.models.Merchant;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.MerchantRepository;
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
    private MerchantRepository merchantRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private AIEventRepository aiEventRepository;

    @InjectMocks
    private AddProductRecommendation useCase;

    private Product product;
    private Merchant merchant;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Producto Test");

        merchant = new Merchant();
        merchant.setId(UUID.randomUUID());

        buffetId = UUID.randomUUID();
    }

    @Test
    void WhenProductIsNotInStockShouldRegisterRecommendation() {
        when(productRepository.findBestSoldProducts()).thenReturn(List.of(product));
        when(merchantRepository.findAll()).thenReturn(List.of(merchant));
        when(merchantRepository.findBuffetById(merchant.getId())).thenReturn(buffetId);
        when(inventoryRepository.checkIfProductIsInStock(product.getId(), buffetId)).thenReturn(false);

        useCase.execute();

        verify(aiEventRepository).registerProductAdditionRecommendation(product, merchant, buffetId);
    }

    @Test
    void WhenProductAlreadyInStock_ShouldNotRegisterRecommendation() {
        when(productRepository.findBestSoldProducts()).thenReturn(List.of(product));
        when(merchantRepository.findAll()).thenReturn(List.of(merchant));
        when(merchantRepository.findBuffetById(merchant.getId())).thenReturn(buffetId);
        when(inventoryRepository.checkIfProductIsInStock(product.getId(), buffetId)).thenReturn(true);

        useCase.execute();

        verify(aiEventRepository, never()).registerProductAdditionRecommendation(any(), any(), any());
    }

    @Mock
    private org.mockito.MockedStatic<UUID> mockedUuid;

}
