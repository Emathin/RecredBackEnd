package com.recred.backend.unit.application;

import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListInventoryProductsRequest;
import com.recred.backend.application.ListInventoryProductsResponse;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.ProductRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListInventoryProductsTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ListInventoryProducts useCase = new ListInventoryProducts(productRepository);
    private UUID buffetId;
    private Producto inventoryProduct;
    private ListInventoryProductsResponse response;

    @Test
    void execute_ShouldReturnOnlyProductsWithInventoryData() {
        givenBuffet();
        givenRepositoryHasCatalogAndInventoryProducts();

        whenListingInventoryProducts();

        thenOnlyInventoryProductsAreReturned();
        thenProductsWereLoaded();
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new ListInventoryProductsRequest(null)));
    }

    private void givenBuffet() {
        buffetId = UUID.randomUUID();
    }

    private void givenRepositoryHasCatalogAndInventoryProducts() {
        inventoryProduct = product(true);
        Producto catalogProduct = product(false);
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(inventoryProduct, catalogProduct));
    }

    private void whenListingInventoryProducts() {
        response = useCase.execute(new ListInventoryProductsRequest(buffetId));
    }

    private void thenOnlyInventoryProductsAreReturned() {
        assertEquals(1, response.products().size());
        assertEquals(inventoryProduct.getId(), response.products().get(0).id());
        assertEquals("DISPONIBLE", response.products().get(0).estadoInventario());
        assertEquals("STOCK_EXACTO", response.products().get(0).tipoManejoInventario());
    }

    private void thenProductsWereLoaded() {
        verify(productRepository).findAllActiveWithStock(buffetId);
    }

    private Producto product(boolean withInventory) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Producto");

        if (withInventory) {
            product.setStockActual(10);
            product.setStockReservado(2);
            product.setStockDisponible(8);
            product.setEstadoInventario(EstadoInventario.DISPONIBLE);
            product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        }

        return product;
    }
}
