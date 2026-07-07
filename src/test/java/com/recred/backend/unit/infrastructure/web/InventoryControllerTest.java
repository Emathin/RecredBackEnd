package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.AdjustInventoryStock;
import com.recred.backend.application.AdjustInventoryStockRequest;
import com.recred.backend.application.AdjustInventoryStockResponse;
import com.recred.backend.application.ApplyInventoryQuickAction;
import com.recred.backend.application.ApplyInventoryQuickActionRequest;
import com.recred.backend.application.GetStockMovements;
import com.recred.backend.application.GetStockMovementsRequest;
import com.recred.backend.application.GetStockMovementsResponse;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListInventoryProductsRequest;
import com.recred.backend.application.UploadProductImage;
import com.recred.backend.application.UploadProductImageRequest;
import com.recred.backend.application.UploadProductImageResponse;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.web.InventoryController;
import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.recred.backend.application.ListInventoryProductsResponse.fromDomain;
import static com.recred.backend.domain.models.EstadoInventario.BAJO_STOCK;
import static com.recred.backend.domain.models.EstadoInventario.DESACTIVADO;
import static com.recred.backend.domain.models.EstadoInventario.DISPONIBLE;
import static com.recred.backend.domain.models.TipoManejoInventario.CUPO_DIARIO;
import static com.recred.backend.domain.models.TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE;
import static com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private AdjustInventoryStock adjustInventoryStock;
    @Mock
    private ApplyInventoryQuickAction applyInventoryQuickAction;
    @Mock
    private ListInventoryProducts listInventoryProducts;
    @Mock
    private GetStockMovements getStockMovements;
    @Mock
    private KiosqueroAccessGuard kiosqueroAccessGuard;
    @Mock
    private UploadProductImage uploadProductImage;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID buffetId;
    private UUID productId;
    private UUID userId;
    private UUID inventoryId;
    private Producto product;
    private Producto secondProduct;
    private Producto thirdProduct;
    private Producto fourthProduct;
    private Inventario inventory;
    private AdjustInventoryStockResponse stockResponse;
    private GetStockMovementsResponse.StockMovementResponse movement;
    private String expectedImageUrl;
    private ResultActions response;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new InventoryController(
                adjustInventoryStock,
                applyInventoryQuickAction,
                listInventoryProducts,
                getStockMovements,
                kiosqueroAccessGuard,
                uploadProductImage
        )).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void givenInventoryProducts_whenListingInventory_thenReturnsProductsWithAvailability()
            throws Exception {
        givenInventoryProduct();
        givenInventoryProducts(product);

        whenListInventory();

        thenInventoryProductsAreReturned();
        thenListInventoryProductsWasCalled();
    }

    @Test
    void givenStockAdjustmentRequest_whenAdjustingStock_thenReturnsUpdatedInventory()
            throws Exception {
        givenStockAdjustmentRequest();
        givenStockAdjustmentReturnsUpdatedInventory();

        whenAdjustStock();

        thenAdjustedStockIsReturned();
        thenAdjustStockWasCalled();
    }

    @Test
    void givenInventoryProduct_whenGettingOverview_thenReturnsSellerState() throws Exception {
        givenDailyCapacityProduct();
        givenInventoryProducts(product);

        whenGetInventoryOverview();

        thenInventoryOverviewIsReturned();
    }

    @Test
    void givenQuickActionRequest_whenApplyingQuickAction_thenMapsCommand() throws Exception {
        givenQuickActionRequest();
        givenQuickActionReturnsUpdatedInventory();

        whenApplyQuickAction();

        thenQuickActionResponseIsReturned();
        thenQuickActionWasCalled();
    }

    @Test
    void givenStockMovements_whenGettingMovements_thenReturnsStockMovements() throws Exception {
        givenStockMovement();
        givenStockMovements();

        whenGetMovements();

        thenStockMovementsAreReturned();
        thenGetStockMovementsWasCalled();
    }

    @Test
    void givenAlternativeInventoryModes_whenGettingOverview_thenMapsStates() throws Exception {
        givenAlternativeInventoryModeProducts();
        givenInventoryProducts(product, secondProduct, thirdProduct, fourthProduct);

        whenGetInventoryOverview();

        thenAlternativeInventoryModesAreReturned();
    }

    @Test
    void givenStockAdjustmentWithNullState_whenAdjustingStock_thenReturnsNullFields()
            throws Exception {
        givenStockAdjustmentRequest();
        givenStockAdjustmentReturnsInventoryWithoutState();

        whenAdjustStockWithoutStateFields();

        thenInventoryStateFieldsAreNull();
    }

    @Test
    void givenProductImageUpload_whenUploadingImage_thenReturnsImageUrl() throws Exception {
        givenProductImageUpload();
        givenUploadProductImageReturnsUrl();

        whenUploadProductImage();

        thenImageUrlIsReturned();
        thenUploadProductImageWasCalled();
    }

    @Test
    void givenMovementWithoutType_whenGettingMovements_thenReturnsNullType() throws Exception {
        givenMovementWithoutType();
        givenStockMovements();

        whenGetMovements();

        thenMovementTypeIsNull();
    }

    private void givenInventoryProduct() {
        buffetId = randomUUID();
        product = product();
    }

    private void givenStockAdjustmentRequest() {
        buffetId = randomUUID();
        productId = randomUUID();
        userId = randomUUID();
        inventory = inventory(buffetId, productId);
    }

    private void givenDailyCapacityProduct() {
        buffetId = randomUUID();
        product = product();
        product.setTipoManejoInventario(CUPO_DIARIO);
        product.setStockActual(20);
        product.setStockReservado(5);
        product.setStockDisponible(15);
        product.setCupoMaximoDiario(20);
        product.setUrlImagen("http://example.com/image.jpg");
    }

    private void givenQuickActionRequest() {
        buffetId = randomUUID();
        productId = randomUUID();
        userId = randomUUID();
        inventory = inventory(buffetId, productId);
    }

    private void givenStockMovement() {
        buffetId = randomUUID();
        productId = randomUUID();
        inventoryId = randomUUID();
        userId = randomUUID();
        movement = movement("AJUSTE", inventoryId, userId);
    }

    private void givenAlternativeInventoryModeProducts() {
        buffetId = randomUUID();
        product = product();
        product.setTipoManejoInventario(DISPONIBLE_NO_DISPONIBLE);
        product.setEstadoInventario(BAJO_STOCK);
        product.setStockDisponible(1);

        secondProduct = product();
        secondProduct.setTipoManejoInventario(null);
        secondProduct.setEstadoInventario(null);
        secondProduct.setStockDisponible(null);

        thirdProduct = product();
        thirdProduct.setTipoManejoInventario(CUPO_DIARIO);
        thirdProduct.setCupoMaximoDiario(null);
        thirdProduct.setStockActual(7);
        thirdProduct.setStockDisponible(0);

        fourthProduct = product();
        fourthProduct.setEstadoInventario(DESACTIVADO);
    }

    private void givenProductImageUpload() {
        buffetId = randomUUID();
        productId = randomUUID();
        expectedImageUrl = "http://cloudinary.com/image.jpg";
    }

    private void givenMovementWithoutType() {
        buffetId = randomUUID();
        productId = randomUUID();
        movement = movement(null, randomUUID(), randomUUID());
    }

    private void givenInventoryProducts(Producto... products) {
        when(listInventoryProducts.execute(new ListInventoryProductsRequest(buffetId)))
                .thenReturn(fromDomain(List.of(products)));
    }

    private void givenStockAdjustmentReturnsUpdatedInventory() {
        when(adjustInventoryStock.execute(any(AdjustInventoryStockRequest.class)))
                .thenReturn(AdjustInventoryStockResponse.fromDomain(inventory));
    }

    private void givenQuickActionReturnsUpdatedInventory() {
        when(applyInventoryQuickAction.execute(any(ApplyInventoryQuickActionRequest.class)))
                .thenReturn(AdjustInventoryStockResponse.fromDomain(inventory));
    }

    private void givenStockMovements() {
        when(getStockMovements.execute(new GetStockMovementsRequest(buffetId, productId)))
                .thenReturn(new GetStockMovementsResponse(List.of(movement)));
    }

    private void givenStockAdjustmentReturnsInventoryWithoutState() {
        stockResponse = new AdjustInventoryStockResponse(
                randomUUID(),
                buffetId,
                productId,
                12,
                2,
                1,
                10,
                null,
                null,
                null,
                null
        );
        when(adjustInventoryStock.execute(any(AdjustInventoryStockRequest.class)))
                .thenReturn(stockResponse);
    }

    private void givenUploadProductImageReturnsUrl() {
        when(uploadProductImage.execute(any(UploadProductImageRequest.class)))
                .thenReturn(new UploadProductImageResponse(expectedImageUrl));
    }

    private void whenListInventory() throws Exception {
        response = mockMvc.perform(get("/api/v1/inventory/{buffetId}", buffetId));
    }

    private void whenAdjustStock() throws Exception {
        response = mockMvc.perform(patch(
                "/api/v1/inventory/{buffetId}/products/{productId}/stock",
                buffetId,
                productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "stockActual", 12,
                        "motivo", "Correccion manual",
                        "usuarioId", userId.toString()
                ))));
    }

    private void whenGetInventoryOverview() throws Exception {
        response = mockMvc.perform(get("/api/v1/inventory/{buffetId}/overview", buffetId));
    }

    private void whenApplyQuickAction() throws Exception {
        response = mockMvc.perform(patch(
                "/api/v1/inventory/{buffetId}/products/{productId}/quick-action",
                buffetId,
                productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "action", "ADD_STOCK",
                        "quantity", 5,
                        "stockMinimo", 2,
                        "usuarioId", userId.toString()
                ))));
    }

    private void whenGetMovements() throws Exception {
        response = mockMvc.perform(get(
                "/api/v1/inventory/{buffetId}/products/{productId}/movements",
                buffetId,
                productId));
    }

    private void whenAdjustStockWithoutStateFields() throws Exception {
        response = mockMvc.perform(patch(
                "/api/v1/inventory/{buffetId}/products/{productId}/stock",
                buffetId,
                productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "stockActual", 12,
                        "motivo", "Correccion manual"
                ))));
    }

    private void whenUploadProductImage() throws Exception {
        response = mockMvc.perform(multipart(
                "/api/v1/inventory/{buffetId}/products/{productId}/imagen",
                buffetId,
                productId)
                .file("imagen", "fake-image-content".getBytes()));
    }

    private void thenInventoryProductsAreReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId().toString()))
                .andExpect(jsonPath("$[0].stockActual").value(12))
                .andExpect(jsonPath("$[0].stockReservado").value(2))
                .andExpect(jsonPath("$[0].stockDisponible").value(10))
                .andExpect(jsonPath("$[0].urlImagen").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$[0].image").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$[0].estadoInventario").value("DISPONIBLE"))
                .andExpect(jsonPath("$[0].tipoManejoInventario").value("STOCK_EXACTO"));
    }

    private void thenListInventoryProductsWasCalled() {
        verify(listInventoryProducts).execute(new ListInventoryProductsRequest(buffetId));
    }

    private void thenAdjustedStockIsReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(12))
                .andExpect(jsonPath("$.urlImagen").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$.stockDisponible").value(10));
    }

    private void thenAdjustStockWasCalled() {
        ArgumentCaptor<AdjustInventoryStockRequest> captor =
                ArgumentCaptor.forClass(AdjustInventoryStockRequest.class);
        verify(adjustInventoryStock).execute(captor.capture());
        assertEquals(buffetId, captor.getValue().buffetId());
        assertEquals(productId, captor.getValue().productId());
        assertEquals(12, captor.getValue().stockActual());
        assertEquals("Correccion manual", captor.getValue().motivo());
        assertEquals(userId, captor.getValue().usuarioId());
    }

    private void thenInventoryOverviewIsReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(product.getId().toString()))
                .andExpect(jsonPath("$[0].tipoManejoInventario").value("CUPO_DIARIO"))
                .andExpect(jsonPath("$[0].estadoInventario").value("DISPONIBLE"))
                .andExpect(jsonPath("$[0].stockDisponible").value(15))
                .andExpect(jsonPath("$[0].cupoMaximoDiario").value(20))
                .andExpect(jsonPath("$[0].cupoDisponibleDia").value(15))
                .andExpect(jsonPath("$[0].urlImagen").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$[0].image").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$[0].disponible").value(true))
                .andExpect(jsonPath("$[0].agotado").value(false));
    }

    private void thenQuickActionResponseIsReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(12));
    }

    private void thenQuickActionWasCalled() {
        ArgumentCaptor<ApplyInventoryQuickActionRequest> captor =
                ArgumentCaptor.forClass(ApplyInventoryQuickActionRequest.class);
        verify(applyInventoryQuickAction).execute(captor.capture());
        assertEquals(buffetId, captor.getValue().buffetId());
        assertEquals(productId, captor.getValue().productId());
        assertEquals(InventoryQuickActionType.ADD_STOCK, captor.getValue().action());
        assertEquals(5, captor.getValue().quantity());
        assertEquals(2, captor.getValue().stockMinimo());
        assertEquals(userId, captor.getValue().usuarioId());
    }

    private void thenStockMovementsAreReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inventarioId").value(inventoryId.toString()))
                .andExpect(jsonPath("$[0].tipo").value("AJUSTE"))
                .andExpect(jsonPath("$[0].cantidad").value(5))
                .andExpect(jsonPath("$[0].motivo").value("Carga inicial"))
                .andExpect(jsonPath("$[0].usuarioId").value(userId.toString()));
    }

    private void thenGetStockMovementsWasCalled() {
        verify(getStockMovements).execute(new GetStockMovementsRequest(buffetId, productId));
    }

    private void thenAlternativeInventoryModesAreReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoManejoInventario")
                        .value("DISPONIBLE_NO_DISPONIBLE"))
                .andExpect(jsonPath("$[0].bajoStock").value(true))
                .andExpect(jsonPath("$[1].tipoManejoInventario").value("STOCK_EXACTO"))
                .andExpect(jsonPath("$[2].cupoMaximoDiario").value(7))
                .andExpect(jsonPath("$[2].agotado").value(true))
                .andExpect(jsonPath("$[2].disponible").value(false))
                .andExpect(jsonPath("$[3].agotado").value(true))
                .andExpect(jsonPath("$[3].disponible").value(false));
    }

    private void thenInventoryStateFieldsAreNull() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoInventario").doesNotExist())
                .andExpect(jsonPath("$.tipoManejoInventario").doesNotExist());
    }

    private void thenImageUrlIsReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlImagen").value(expectedImageUrl));
    }

    private void thenUploadProductImageWasCalled() {
        verify(uploadProductImage).execute(any(UploadProductImageRequest.class));
    }

    private void thenMovementTypeIsNull() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").doesNotExist());
    }

    private Inventario inventory(UUID buffetId, UUID productId) {
        Inventario inventory = new Inventario();
        inventory.setId(randomUUID());
        inventory.setBuffetId(buffetId);
        inventory.setProductoId(productId);
        inventory.setStockActual(12);
        inventory.setStockReservado(2);
        inventory.setStockMinimo(1);
        inventory.setEstado(DISPONIBLE);
        inventory.setTipoManejo(STOCK_EXACTO);
        inventory.setUrlImagen("http://example.com/image.jpg");
        return inventory;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(randomUUID());
        product.setNombre("Alfajor");
        product.setDescripcion("Alfajor simple");
        product.setPrecio(new BigDecimal("10.00"));
        product.setStockActual(12);
        product.setStockReservado(2);
        product.setStockDisponible(10);
        product.setEstadoInventario(DISPONIBLE);
        product.setTipoManejoInventario(STOCK_EXACTO);
        product.setUrlImagen("http://example.com/image.jpg");
        return product;
    }

    private GetStockMovementsResponse.StockMovementResponse movement(
            String type,
            UUID inventoryId,
            UUID userId) {
        return new GetStockMovementsResponse.StockMovementResponse(
                randomUUID(),
                inventoryId,
                type,
                5,
                10,
                15,
                "Carga inicial",
                userId,
                null,
                LocalDateTime.of(2026, 6, 8, 10, 0)
        );
    }
}
