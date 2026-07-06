package com.recred.backend.unit.application;

import com.recred.backend.application.GetAllProducts;
import com.recred.backend.application.GetAllProductsRequest;
import com.recred.backend.application.GetAllProductsResponse;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;


import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllProductsTest {

    @Mock
    private ProductRepository productRepository;

    private GetAllProducts useCase;
    private GetAllProductsResponse response;
    private UUID buffetId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        useCase = new GetAllProducts(productRepository);
    }

    @Test
    void givenNoBuffetFilter_whenGettingProducts_thenReturnsAllActiveProducts() {
        givenActiveProductsExist();

        whenGetAllProductsWithoutBuffetFilter();

        thenProductsWereReturned();
        thenAllActiveProductsWereRequested();
    }

    @Test
    void givenBuffetFilter_whenGettingProducts_thenReturnsActiveProductsWithStock() {
        givenBuffetHasActiveProductsWithStock();

        whenGetAllProductsForBuffet();

        thenProductsWereReturned();
        thenActiveProductsWithStockWereRequested();
    }

    private void givenActiveProductsExist() {
        productId = randomUUID();
        when(productRepository.findAllActive()).thenReturn(List.of(product(productId)));
    }

    private void givenBuffetHasActiveProductsWithStock() {
        buffetId = randomUUID();
        productId = randomUUID();
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(product(productId)));
    }

    private void whenGetAllProductsWithoutBuffetFilter() {
        response = useCase.execute(new GetAllProductsRequest(null));
    }

    private void whenGetAllProductsForBuffet() {
        response = useCase.execute(new GetAllProductsRequest(buffetId));
    }

    private void thenProductsWereReturned() {
        assertEquals(1, response.products().size());
        assertEquals(productId, response.products().get(0).id());
    }

    private void thenAllActiveProductsWereRequested() {
        verify(productRepository).findAllActive();
    }

    private void thenActiveProductsWithStockWereRequested() {
        verify(productRepository).findAllActiveWithStock(buffetId);
    }

    private Producto product(UUID requestedProductId) {
        Producto product = new Producto();
        product.setId(requestedProductId);
        return product;
    }
}
