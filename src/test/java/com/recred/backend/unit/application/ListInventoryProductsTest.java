package com.recred.backend.unit.application;

import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListInventoryProductsRequest;
import com.recred.backend.application.ListInventoryProductsResponse;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListInventoryProductsTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ListInventoryProducts useCase = new ListInventoryProducts(productRepository);
    private UUID buffetId;
    private Producto inventoryProduct;
    private Producto dailyCapacityProduct;
    private ListInventoryProductsResponse response;
    private IllegalArgumentException exception;

    @Test
    void execute_ReturnsOnlyProductsWithInventoryData() {
        givenBuffet();
        givenRepositoryHasCatalogAndInventoryProducts();

        whenListingInventoryProducts();

        thenOnlyInventoryProductsAreReturned();
        thenProductsWereLoaded();
    }

    @Test
    void execute_WhenProductOnlyHasDailyCapacity_ReturnsProductAsInventoryProduct() {
        givenBuffet();
        givenProductOnlyHasDailyCapacity();

        whenListingInventoryProducts();

        thenDailyCapacityProductIsReturned();
        thenProductsWereLoaded();
    }

    @Test
    void execute_WhenRequestIsNull_ThrowsException() {
        whenListingInventoryProductsWith(null);

        thenExceptionMessageIs("request is required");
        thenProductsWereNotLoaded();
    }

    @Test
    void execute_WhenBuffetIdIsNull_ThrowsException() {
        whenListingInventoryProductsWith(new ListInventoryProductsRequest(null));

        thenExceptionMessageIs("buffetId is required");
        thenProductsWereNotLoaded();
    }

    private void givenBuffet() {
        buffetId = randomUUID();
    }

    private void givenRepositoryHasCatalogAndInventoryProducts() {
        inventoryProduct = inventoryProduct();
        Producto catalogProduct = catalogProduct();
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(inventoryProduct, catalogProduct));
    }

    private void givenProductOnlyHasDailyCapacity() {
        dailyCapacityProduct = catalogProduct();
        dailyCapacityProduct.setCupoMaximoDiario(20);
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(dailyCapacityProduct));
    }

    private void whenListingInventoryProducts() {
        response = useCase.execute(new ListInventoryProductsRequest(buffetId));
    }

    private void whenListingInventoryProductsWith(ListInventoryProductsRequest request) {
        Executable execution = () -> useCase.execute(request);

        exception = assertThrows(IllegalArgumentException.class, execution);
    }

    private void thenOnlyInventoryProductsAreReturned() {
        assertEquals(1, response.products().size());
        assertEquals(inventoryProduct.getId(), response.products().get(0).id());
        assertEquals("DISPONIBLE", response.products().get(0).estadoInventario());
        assertEquals("STOCK_EXACTO", response.products().get(0).tipoManejoInventario());
    }

    private void thenDailyCapacityProductIsReturned() {
        assertEquals(1, response.products().size());
        assertEquals(dailyCapacityProduct.getId(), response.products().get(0).id());
        assertEquals(20, response.products().get(0).cupoMaximoDiario());
    }

    private void thenExceptionMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void thenProductsWereLoaded() {
        verify(productRepository).findAllActiveWithStock(buffetId);
    }

    private void thenProductsWereNotLoaded() {
        verifyNoInteractions(productRepository);
    }

    private Producto inventoryProduct() {
        Producto product = catalogProduct();
        product.setStockActual(10);
        product.setStockReservado(2);
        product.setStockDisponible(8);
        product.setEstadoInventario(EstadoInventario.DISPONIBLE);
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        return product;
    }

    private Producto catalogProduct() {
        Producto product = new Producto();
        product.setId(randomUUID());
        product.setNombre("Producto");
        return product;
    }
}
