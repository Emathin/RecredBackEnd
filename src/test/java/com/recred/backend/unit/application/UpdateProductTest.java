package com.recred.backend.unit.application;

import com.recred.backend.application.ProductResponse;
import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductRequest;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private UpdateProduct useCase;

    private UUID productId;
    private UUID buffetId;
    private UUID categoryId;
    private UUID healthClassificationId;
    private UUID typeId;
    private Producto product;
    private Inventario inventory;
    private CategoriaProducto category;
    private UpdateProductRequest request;
    private ProductResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new UpdateProduct(productRepository, inventoryRepository, categoryRepository);
    }

    @Test
    void givenProductDoesNotExist_whenUpdatingProduct_thenFailsBeforeSaving() {
        givenProductUpdateScenario();
        givenProductUpdateRequest();
        givenProductDoesNotExist();

        whenUpdatingProductFails();

        thenProductWasNotFound();
        thenProductWasNotSaved();
    }

    @Test
    void givenProductExists_whenUpdatingProductDetails_thenUpdatesAndSavesProduct() {
        givenProductUpdateScenario();
        givenProductUpdateRequest();
        givenProductExists();
        givenProductCanBeSaved();

        whenUpdatingProduct();

        thenUpdatedProductIsReturned();
        thenProductDetailsWereUpdated();
        thenProductWasSaved();
        thenInventoryWasNotSaved();
    }

    @Test
    void givenInventoryData_whenUpdatingProduct_thenUpdatesExistingInventoryStock() {
        givenProductUpdateScenario();
        givenInventoryStockUpdateRequest();
        givenProductExists();
        givenProductCanBeSaved();
        givenInventoryExistsWithStock(10);

        whenUpdatingProduct();

        thenInventoryStockIs(100);
        thenInventoryWasSaved();
    }

    @Test
    void givenExistingCategoryName_whenUpdatingProduct_thenAssignsExistingCategory() {
        givenProductUpdateScenario();
        givenNewCategoryNameRequest("Frutas");
        givenProductExists();
        givenProductCanBeSaved();
        givenCategoryExists("Frutas");

        whenUpdatingProduct();

        thenCategoryWasAssigned();
        thenCategoryWasNotCreated();
    }

    @Test
    void givenNewCategoryName_whenUpdatingProduct_thenCreatesAndAssignsCategory() {
        givenProductUpdateScenario();
        givenNewCategoryNameRequest("Nueva Categoria");
        givenProductExists();
        givenProductCanBeSaved();
        givenCategoryDoesNotExist("Nueva Categoria");
        givenCategoryCanBeCreated("Nueva Categoria");

        whenUpdatingProduct();

        thenCategoryWasAssigned();
        thenCategoryWasCreated();
    }

    @Test
    void givenCategoryIdWithoutNewName_whenUpdatingProduct_thenAssignsCategoryReference() {
        givenProductUpdateScenario();
        givenCategoryReferenceRequest();
        givenProductExists();
        givenProductCanBeSaved();

        whenUpdatingProduct();

        thenCategoryReferenceWasAssigned();
        thenCategoryWasNotSearchedByName();
    }

    @Test
    void givenHealthClassificationsAndTypes_whenUpdatingProduct_thenReplacesBothSets() {
        givenProductUpdateScenario();
        givenClassificationsAndTypesRequest();
        givenProductExists();
        givenProductCanBeSaved();

        whenUpdatingProduct();

        thenHealthClassificationsWereUpdated();
        thenTypesWereUpdated();
    }

    @Test
    void givenInventoryDoesNotExist_whenUpdatingProduct_thenCreatesInventory() {
        givenProductUpdateScenario();
        givenNewDailyCapacityInventoryRequest(TipoManejoInventario.CUPO_DIARIO, 25);
        givenProductExists();
        givenProductCanBeSaved();
        givenInventoryDoesNotExist();

        whenUpdatingProduct();

        thenNewInventoryWasSavedWith(TipoManejoInventario.CUPO_DIARIO, 25);
    }

    @Test
    void givenOnlyDailyCapacity_whenCreatingInventory_thenUsesDefaultInventoryHandlingType() {
        givenProductUpdateScenario();
        givenNewDailyCapacityInventoryRequest(null, 8);
        givenProductExists();
        givenProductCanBeSaved();
        givenInventoryDoesNotExist();

        whenUpdatingProduct();

        thenNewInventoryWasSavedWith(TipoManejoInventario.STOCK_EXACTO, 8);
    }

    private void givenProductUpdateScenario() {
        productId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        healthClassificationId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        product = new Producto();
        product.setId(productId);
    }

    private void givenProductUpdateRequest() {
        request = baseRequest(null, null, null, null, null, null).request();
    }

    private void givenInventoryStockUpdateRequest() {
        request = baseRequest(buffetId, 100, null, null, null, null).request();
    }

    private void givenNewCategoryNameRequest(String categoryName) {
        request = baseRequest(null, null, null, null, null, null)
                .withCategory(UUID.randomUUID(), categoryName);
    }

    private void givenCategoryReferenceRequest() {
        request = baseRequest(null, null, null, null, null, null)
                .withCategory(categoryId, " ");
    }

    private void givenClassificationsAndTypesRequest() {
        request = baseRequest(
                null,
                null,
                Set.of(healthClassificationId),
                Set.of(typeId),
                null,
                null
        ).request();
    }

    private void givenNewDailyCapacityInventoryRequest(
            TipoManejoInventario inventoryHandlingType,
            Integer dailyCapacity
    ) {
        request = baseRequest(
                buffetId,
                null,
                null,
                null,
                inventoryHandlingType,
                dailyCapacity
        ).request();
    }

    private void givenProductDoesNotExist() {
        when(productRepository.findActiveById(productId)).thenReturn(Optional.empty());
    }

    private void givenProductExists() {
        when(productRepository.findActiveById(productId)).thenReturn(Optional.of(product));
    }

    private void givenProductCanBeSaved() {
        when(productRepository.save(product)).thenReturn(product);
    }

    private void givenInventoryExistsWithStock(int stock) {
        inventory = new Inventario();
        inventory.setStockActual(stock);
        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
    }

    private void givenInventoryDoesNotExist() {
        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.empty());
    }

    private void givenCategoryExists(String categoryName) {
        category = category(categoryName);
        when(categoryRepository.findByDescription(categoryName)).thenReturn(Optional.of(category));
    }

    private void givenCategoryDoesNotExist(String categoryName) {
        when(categoryRepository.findByDescription(categoryName)).thenReturn(Optional.empty());
    }

    private void givenCategoryCanBeCreated(String categoryName) {
        category = category(categoryName);
        when(categoryRepository.save(any(CategoriaProducto.class))).thenReturn(category);
    }

    private void whenUpdatingProduct() {
        response = useCase.execute(request);
    }

    private void whenUpdatingProductFails() {
        thrownException = assertThrows(RuntimeException.class, this::whenUpdatingProduct);
    }

    private void thenProductWasNotFound() {
        assertTrue(thrownException instanceof ProductNotFoundException);
    }

    private void thenProductWasNotSaved() {
        verify(productRepository, never()).save(any());
    }

    private void thenUpdatedProductIsReturned() {
        assertEquals(productId, response.id());
    }

    private void thenProductDetailsWereUpdated() {
        assertEquals("Nombre Act", product.getNombre());
        assertEquals("Desc Act", product.getDescripcion());
    }

    private void thenProductWasSaved() {
        verify(productRepository).save(product);
    }

    private void thenInventoryWasNotSaved() {
        verify(inventoryRepository, never()).save(any());
    }

    private void thenInventoryStockIs(int stock) {
        assertEquals(stock, inventory.getStockActual());
    }

    private void thenInventoryWasSaved() {
        verify(inventoryRepository).save(inventory);
    }

    private void thenCategoryWasAssigned() {
        assertEquals(category, product.getCategoria());
    }

    private void thenCategoryWasNotCreated() {
        verify(categoryRepository, never()).save(any());
    }

    private void thenCategoryWasCreated() {
        verify(categoryRepository).save(any(CategoriaProducto.class));
    }

    private void thenCategoryReferenceWasAssigned() {
        assertEquals(categoryId, product.getCategoria().getId());
    }

    private void thenCategoryWasNotSearchedByName() {
        verify(categoryRepository, never()).findByDescription(any());
    }

    private void thenHealthClassificationsWereUpdated() {
        assertEquals(healthClassificationId,
                product.getClasificacionesSalud().iterator().next().getId());
    }

    private void thenTypesWereUpdated() {
        assertEquals(typeId, product.getTipos().iterator().next().getId());
    }

    private void thenNewInventoryWasSavedWith(
            TipoManejoInventario inventoryHandlingType,
            Integer dailyCapacity
    ) {
        verify(inventoryRepository).save(argThat(inventory ->
                buffetId.equals(inventory.getBuffetId())
                        && productId.equals(inventory.getProductoId())
                        && inventoryHandlingType.equals(inventory.getTipoManejo())
                        && dailyCapacity.equals(inventory.getCupoMaximoDiario())
                        && inventory.getStockActual() == null
        ));
    }

    private ProductUpdateRequestBuilder baseRequest(
            UUID buffetId,
            Integer stock,
            Set<UUID> healthClassifications,
            Set<UUID> types,
            TipoManejoInventario inventoryHandlingType,
            Integer dailyCapacity
    ) {
        return new ProductUpdateRequestBuilder(
                productId,
                buffetId,
                stock,
                healthClassifications,
                types,
                inventoryHandlingType,
                dailyCapacity
        );
    }

    private CategoriaProducto category(String categoryName) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion(categoryName);
        return category;
    }

    private record ProductUpdateRequestBuilder(
            UUID productId,
            UUID buffetId,
            Integer stock,
            Set<UUID> healthClassifications,
            Set<UUID> types,
            TipoManejoInventario inventoryHandlingType,
            Integer dailyCapacity
    ) {
        private UpdateProductRequest withCategory(UUID categoryId, String newCategoryName) {
            return request(categoryId, newCategoryName);
        }

        private UpdateProductRequest request() {
            return request(null, null);
        }

        private UpdateProductRequest request(UUID categoryId, String newCategoryName) {
            return new UpdateProductRequest(
                    productId,
                    "Nombre Act",
                    "Desc Act",
                    BigDecimal.TEN,
                    new BigDecimal("0.5"),
                    false,
                    categoryId,
                    newCategoryName,
                    buffetId,
                    stock,
                    healthClassifications,
                    types,
                    inventoryHandlingType,
                    dailyCapacity,
                    null
            );
        }
    }
}
