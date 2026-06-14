package com.recred.backend.application;

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

    @Test
    void execute_ShouldReturnOnlyProductsWithInventoryData() {
        UUID buffetId = UUID.randomUUID();
        Producto inventoryProduct = product(true);
        Producto catalogProduct = product(false);

        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(inventoryProduct, catalogProduct));

        List<Producto> result = useCase.execute(buffetId);

        assertEquals(List.of(inventoryProduct), result);
        verify(productRepository).findAllActiveWithStock(buffetId);
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
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
