package com.recred.backend.unit.application;

import com.recred.backend.application.ProductResponse;
import com.recred.backend.application.SaveBulkProducts;
import com.recred.backend.application.SaveBulkProductsRequest;
import com.recred.backend.application.SaveBulkProductsResponse;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SaveProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveBulkProductsTest {

    @Mock
    private SaveProduct saveProduct;

    private SaveBulkProducts useCase;

    private SaveProductRequest firstProductRequest;
    private SaveProductRequest secondProductRequest;
    private ProductResponse firstProduct;
    private ProductResponse secondProduct;
    private SaveBulkProductsResponse response;

    @BeforeEach
    void setUp() {
        useCase = new SaveBulkProducts(saveProduct);
    }

    @Test
    void givenProductRequests_whenSavingBulkProducts_thenSavesEveryProduct() {
        givenBulkProductRequests();
        givenProductsAreSaved();

        whenSavingBulkProducts();

        thenSavedProductsAreReturned();
        thenEveryProductWasSaved();
    }

    private void givenBulkProductRequests() {
        firstProductRequest = productRequest("Producto 1", "100", "500", 10);
        secondProductRequest = productRequest("Producto 2", "200", "200", 5);
        firstProduct = productResponse(UUID.randomUUID(), "Producto 1");
        secondProduct = productResponse(UUID.randomUUID(), "Producto 2");
    }

    private void givenProductsAreSaved() {
        when(saveProduct.execute(firstProductRequest)).thenReturn(firstProduct);
        when(saveProduct.execute(secondProductRequest)).thenReturn(secondProduct);
    }

    private void whenSavingBulkProducts() {
        response = useCase.execute(new SaveBulkProductsRequest(List.of(
                firstProductRequest,
                secondProductRequest
        )));
    }

    private void thenSavedProductsAreReturned() {
        assertEquals(2, response.products().size());
        assertEquals("Producto 1", response.products().get(0).nombre());
        assertEquals("Producto 2", response.products().get(1).nombre());
    }

    private void thenEveryProductWasSaved() {
        verify(saveProduct).execute(firstProductRequest);
        verify(saveProduct).execute(secondProductRequest);
    }

    private SaveProductRequest productRequest(
            String name,
            String price,
            String cost,
            int stock
    ) {
        return new SaveProductRequest(
                name,
                "Desc " + name,
                new BigDecimal(price),
                new BigDecimal(cost),
                false,
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                stock,
                null,
                null
        );
    }

    private ProductResponse productResponse(UUID id, String name) {
        return new ProductResponse(
                id,
                name,
                null,
                null,
                null,
                false,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Set.of(),
                Set.of()
        );
    }
}
