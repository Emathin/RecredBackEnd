package com.recred.backend.unit.application;

import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SaveProductRequest;
import com.recred.backend.application.ProductResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveProductTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID HEALTH_ID = UUID.randomUUID();
    private static final UUID TYPE_ID = UUID.randomUUID();
    
    private static final String NEW_CAT_NAME = "Nueva Cat";
    private static final String EXISTING_CAT_NAME = "Cat Existente";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SaveProduct saveProduct;

    @Test
    void execute_WithCompleteData_ShouldSaveProductAndInventory() {
        SaveProductRequest request = givenCompleteRequest();
        givenProductIsSaved(PRODUCT_ID);
        givenInventoryNotFound(BUFFET_ID, PRODUCT_ID);

        ProductResponse response = whenExecute(request);

        thenProductAndInventoryAreSavedWithStock(response, 50);
    }

    @Test
    void execute_WhenCategoryDoesNotExist_ShouldCreateNewCategory() {
        SaveProductRequest request = givenRequestWithNewCategoryName();
        givenCategoryNotFound(NEW_CAT_NAME);
        givenCategoryIsSaved(NEW_CAT_NAME);
        givenProductIsSavedAndReturned();

        ProductResponse response = whenExecute(request);

        thenNewCategoryIsCreatedAndAssigned(response, NEW_CAT_NAME);
    }

    @Test
    void execute_WhenCategoryExists_ShouldUseExistingCategory() {
        SaveProductRequest request = givenRequestWithExistingCategoryName();
        CategoriaProducto existingCat = givenCategoryFound(EXISTING_CAT_NAME);
        givenProductIsSavedAndReturned();

        ProductResponse response = whenExecute(request);

        thenExistingCategoryIsAssigned(response, existingCat);
    }

    @Test
    void execute_WhenInventoryAlreadyExists_ShouldUpdateExistingInventory() {
        SaveProductRequest request = givenRequestWithBuffetIdAndStock(100);
        givenProductIsSaved(PRODUCT_ID);
        Inventario existingInventory = givenInventoryFound(BUFFET_ID, PRODUCT_ID);

        whenExecute(request);

        thenInventoryIsUpdated(existingInventory, 100);
    }

    @Test
    void execute_WhenInventoryHasManagementTypeButNoStock_ShouldSaveInventoryWithoutStock() {
        SaveProductRequest request = givenRequestWithDailyLimitManagement(20);
        givenProductIsSaved(PRODUCT_ID);
        givenInventoryNotFound(BUFFET_ID, PRODUCT_ID);

        ProductResponse response = whenExecute(request);

        thenInventoryIsSavedWithDailyLimit(response, 20);
    }

    @Test
    void execute_WhenClassificationsAndTypesProvided_ShouldMapThem() {
        SaveProductRequest request = givenRequestWithClassificationsAndTypes();
        givenProductIsSavedAndReturned();

        ProductResponse response = whenExecute(request);

        thenClassificationsAndTypesAreMapped(response);
    }

    @Test
    void execute_WhenCategoryFieldsAreNull_ShouldNotAssignCategory() {
        SaveProductRequest request = givenRequestWithNullCategoryFields();
        givenProductIsSavedAndReturned();

        ProductResponse response = whenExecute(request);

        thenCategoryIsNull(response);
    }

    @Test
    void execute_WhenStockIsNull_ShouldNotSaveInventory() {
        SaveProductRequest request = givenRequestWithNullStockAndManagement();
        givenProductIsSavedAndReturned();

        whenExecute(request);

        thenNoInventoryIsSaved();
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldNotSaveInventory() {
        SaveProductRequest request = givenRequestWithNullBuffetId();
        givenProductIsSavedAndReturned();

        whenExecute(request);

        thenNoInventoryIsSaved();
    }

    private SaveProductRequest givenCompleteRequest() {
        return new SaveProductRequest(
                "Galletitas", "Deliciosas", new BigDecimal("150.00"), new BigDecimal("0.5"), false,
                CATEGORY_ID, null, BUFFET_ID, 50, Set.of(HEALTH_ID), Set.of(TYPE_ID)
        );
    }

    private SaveProductRequest givenRequestWithNewCategoryName() {
        return new SaveProductRequest(
                "Producto", "Desc", BigDecimal.TEN, BigDecimal.ONE, false,
                null, NEW_CAT_NAME, null, null, null, null
        );
    }

    private SaveProductRequest givenRequestWithExistingCategoryName() {
        return new SaveProductRequest(
                "Producto", "Desc", BigDecimal.TEN, BigDecimal.ONE, false,
                null, EXISTING_CAT_NAME, null, null, null, null
        );
    }

    private SaveProductRequest givenRequestWithBuffetIdAndStock(int stock) {
        return new SaveProductRequest(
                "Prod", "Desc", BigDecimal.TEN, BigDecimal.ONE, false,
                null, null, BUFFET_ID, stock, null, null
        );
    }

    private SaveProductRequest givenRequestWithDailyLimitManagement(int limit) {
        return new SaveProductRequest(
                "Prod", "Desc", BigDecimal.TEN, BigDecimal.ONE, false,
                null, null, BUFFET_ID, null, null, null, TipoManejoInventario.CUPO_DIARIO, limit, null
        );
    }

    private SaveProductRequest givenRequestWithClassificationsAndTypes() {
        return new SaveProductRequest(
                "Prod", null, null, null, true,
                null, null, null, null, Set.of(HEALTH_ID), Set.of(TYPE_ID)
        );
    }

    private SaveProductRequest givenRequestWithNullCategoryFields() {
        return new SaveProductRequest(
                "Prod", null, null, null, true,
                null, "", null, null, null, null
        );
    }

    private SaveProductRequest givenRequestWithNullStockAndManagement() {
        return new SaveProductRequest(
                "Prod", null, null, null, true,
                null, null, BUFFET_ID, null, null, null
        );
    }

    private SaveProductRequest givenRequestWithNullBuffetId() {
        return new SaveProductRequest(
                "Prod", null, null, null, true,
                null, null, null, 100, null, null
        );
    }

    private void givenProductIsSaved(UUID productId) {
        Producto savedProduct = new Producto();
        savedProduct.setId(productId);
        when(productRepository.save(any(Producto.class))).thenReturn(savedProduct);
    }

    private void givenProductIsSavedAndReturned() {
        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void givenInventoryNotFound(UUID buffetId, UUID productId) {
        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.empty());
    }

    private Inventario givenInventoryFound(UUID buffetId, UUID productId) {
        Inventario inventory = new Inventario();
        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        return inventory;
    }

    private void givenCategoryNotFound(String name) {
        when(categoryRepository.findByDescription(name)).thenReturn(Optional.empty());
    }

    private void givenCategoryIsSaved(String name) {
        CategoriaProducto newCat = new CategoriaProducto();
        newCat.setDescripcion(name);
        when(categoryRepository.save(any(CategoriaProducto.class))).thenReturn(newCat);
    }

    private CategoriaProducto givenCategoryFound(String name) {
        CategoriaProducto existingCat = new CategoriaProducto();
        existingCat.setId(CATEGORY_ID);
        existingCat.setDescripcion(name);
        when(categoryRepository.findByDescription(name)).thenReturn(Optional.of(existingCat));
        return existingCat;
    }

    private ProductResponse whenExecute(SaveProductRequest request) {
        return saveProduct.execute(request);
    }

    private void thenProductAndInventoryAreSavedWithStock(ProductResponse response, int expectedStock) {
        assertNotNull(response);
        assertEquals(expectedStock, response.stockActual());
        verify(productRepository).save(any(Producto.class));
        verify(inventoryRepository).save(any(Inventario.class));
    }

    private void thenNewCategoryIsCreatedAndAssigned(ProductResponse response, String expectedName) {
        assertNotNull(response.categoria());
        assertEquals(expectedName, response.categoria().descripcion());
        verify(categoryRepository).save(any(CategoriaProducto.class));
    }

    private void thenExistingCategoryIsAssigned(ProductResponse response, CategoriaProducto expectedCat) {
        assertEquals(expectedCat.getId(), response.categoria().id());
        assertEquals(expectedCat.getDescripcion(), response.categoria().descripcion());
    }

    private void thenInventoryIsUpdated(Inventario inventory, int expectedStock) {
        assertEquals(expectedStock, inventory.getStockActual());
        verify(inventoryRepository).save(inventory);
    }

    private void thenInventoryIsSavedWithDailyLimit(ProductResponse response, int expectedLimit) {
        assertNull(response.stockActual());
        assertNull(response.stockDisponible());
        assertEquals(TipoManejoInventario.CUPO_DIARIO.name(), response.tipoManejoInventario());
        assertEquals(expectedLimit, response.cupoMaximoDiario());
        verify(inventoryRepository).save(any(Inventario.class));
    }

    private void thenClassificationsAndTypesAreMapped(ProductResponse response) {
        assertEquals(1, response.clasificacionesSalud().size());
        assertEquals(HEALTH_ID, response.clasificacionesSalud().iterator().next().id());
        assertEquals(1, response.tipos().size());
        assertEquals(TYPE_ID, response.tipos().iterator().next().id());
    }

    private void thenCategoryIsNull(ProductResponse response) {
        assertNull(response.categoria());
    }

    private void thenNoInventoryIsSaved() {
        verify(inventoryRepository, never()).save(any());
    }
}
