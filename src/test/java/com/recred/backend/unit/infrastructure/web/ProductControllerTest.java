package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.BulkProductData;
import com.recred.backend.application.BulkStockParseResponse;
import com.recred.backend.application.DeleteProduct;
import com.recred.backend.application.DeleteProductRequest;
import com.recred.backend.application.GetAllProducts;
import com.recred.backend.application.GetAllProductsRequest;
import com.recred.backend.application.GetAllProductsResponse;
import com.recred.backend.application.GetProductById;
import com.recred.backend.application.GetProductByIdRequest;
import com.recred.backend.application.ParseBulkStock;
import com.recred.backend.application.ProductResponse;
import com.recred.backend.application.SaveBulkProducts;
import com.recred.backend.application.SaveBulkProductsRequest;
import com.recred.backend.application.SaveBulkProductsResponse;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SaveProductRequest;
import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductRequest;
import com.recred.backend.application.UpdateProductPrice;
import com.recred.backend.application.UpdateProductPriceRequest;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.web.ProductController;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.ProductRequestDTO;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BUFFET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CATEGORY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private GetAllProducts getAllProducts;
    @Mock
    private GetProductById getProductById;
    @Mock
    private SaveProduct saveProduct;
    @Mock
    private UpdateProduct updateProduct;
    @Mock
    private DeleteProduct deleteProduct;
    @Mock
    private UpdateProductPrice updateProductPrice;
    @Mock
    private ParseBulkStock parseBulkStock;
    @Mock
    private SaveBulkProducts saveBulkProducts;
    @Mock
    private PlanAccessGuard planAccessGuard;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(
                getAllProducts,
                getProductById,
                saveProduct,
                updateProduct,
                deleteProduct,
                updateProductPrice,
                parseBulkStock,
                saveBulkProducts,
                planAccessGuard
        );
        mockMvc = standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void updatePriceAcceptsQueryParams() throws Exception {
        ResultActions result = whenUpdatingPriceWithQueryParams(PRODUCT_ID, BUFFET_ID);

        thenResultIsOk(result);
        thenUpdatePriceIsDelegated(PRODUCT_ID, BUFFET_ID);
    }

    @Test
    void updatePriceAcceptsJsonBody() throws Exception {
        ResultActions result = whenUpdatingPriceWithJsonBody(PRODUCT_ID, BUFFET_ID);

        thenResultIsOk(result);
        thenUpdatePriceIsDelegated(PRODUCT_ID, BUFFET_ID);
    }

    @Test
    void updatePriceAcceptsOnlyQueryPriceAndNullBuffet() {
        ResponseEntity<Void> response = whenUpdatingPriceWithOnlyQueryPrice(PRODUCT_ID);

        thenResponseIsOk(response);
        thenUpdatePriceIsDelegated(PRODUCT_ID, null);
    }

    @Test
    void updatePriceWithoutPriceThrowsIllegalArgumentException() {
        Executable execution = () -> controller.updatePrice(PRODUCT_ID, null, null, null);
        
        thenThrowsIllegalArgumentException(execution);
    }

    @Test
    void uploadFileShouldParseBulkStockFile() {
        byte[] content = "nombre,precio\nYogur,120".getBytes();
        MockMultipartFile file = givenMultipartFile(content);
        givenParseBulkStockResponse();

        ResponseEntity<BulkStockParseResponse> response = controller.uploadFile(null, file);

        thenFileIsParsedProperly(response, content);
    }

    @Test
    void uploadFileShouldWrapIoException() throws Exception {
        MultipartFile file = givenFailingMultipartFile();

        Executable execution = () -> controller.uploadFile(null, file);

        thenThrowsWrappedIoException(execution);
    }

    @Test
    void createBulkShouldMapRequestsAndResponses() {
        ProductRequestDTO request = productRequest(BUFFET_ID);
        givenBulkSaveResponse(PRODUCT_ID);

        ResponseEntity<List<ProductResponseDTO>> response = controller.createBulk(
                null,
                List.of(request)
        );

        thenBulkProductsAreSavedAndReturned(response, PRODUCT_ID, BUFFET_ID);
    }

    @Test
    void findAllReturnsMappedProducts() {
        givenProductsForBuffet(BUFFET_ID, ProductResponse.fromDomain(product(PRODUCT_ID)));

        ResponseEntity<List<ProductResponseDTO>> response = whenFindingAllProducts(BUFFET_ID);

        thenProductsAreReturned(response, PRODUCT_ID, BUFFET_ID);
    }

    @Test
    void findByIdReturnsMappedProduct() {
        givenProductExists(PRODUCT_ID, BUFFET_ID, ProductResponse.fromDomain(product(PRODUCT_ID)));

        ResponseEntity<ProductResponseDTO> response = whenFindingProductById(PRODUCT_ID, BUFFET_ID);

        thenProductIsReturned(response, PRODUCT_ID, BUFFET_ID);
    }

    @Test
    void createDelegatesSaveCommandAndReturnsCreatedProduct() {
        ProductRequestDTO request = productRequest(BUFFET_ID);
        givenProductIsSaved(PRODUCT_ID);

        ResponseEntity<ProductResponseDTO> response = whenCreatingProduct(request);

        thenProductIsCreated(response, PRODUCT_ID, BUFFET_ID);
    }

    @Test
    void updateDelegatesUpdateCommandAndReturnsUpdatedProduct() {
        ProductRequestDTO request = productRequest(BUFFET_ID);
        givenProductIsUpdated(PRODUCT_ID);

        ResponseEntity<ProductResponseDTO> response = whenUpdatingProduct(PRODUCT_ID, request);

        thenProductIsUpdated(response, PRODUCT_ID, BUFFET_ID);
    }

    @Test
    void deleteDelegatesAndReturnsNoContent() {
        ResponseEntity<Void> response = whenDeletingProduct(PRODUCT_ID);

        thenProductIsDeleted(response, PRODUCT_ID);
    }

    private MockMultipartFile givenMultipartFile(byte[] content) {
        return new MockMultipartFile(
                "file",
                "stock.csv",
                "text/csv",
                content
        );
    }
    
    private void givenParseBulkStockResponse() {
        when(parseBulkStock.execute(any()))
                .thenReturn(new BulkStockParseResponse(List.of(bulkProductData())));
    }
    
    private MultipartFile givenFailingMultipartFile() throws IOException {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("read error"));
        return file;
    }

    private void givenBulkSaveResponse(UUID productId) {
        when(saveBulkProducts.execute(any(SaveBulkProductsRequest.class)))
                .thenReturn(new SaveBulkProductsResponse(List.of(
                        ProductResponse.fromDomain(product(productId))
                )));
    }

    private void givenProductsForBuffet(UUID buffetId, ProductResponse... products) {
        when(getAllProducts.execute(new GetAllProductsRequest(buffetId)))
                .thenReturn(new GetAllProductsResponse(List.of(products)));
    }

    private void givenProductExists(
            UUID productId,
            UUID buffetId,
            ProductResponse product
    ) {
        when(getProductById.execute(new GetProductByIdRequest(productId, buffetId)))
                .thenReturn(product);
    }

    private void givenProductIsSaved(UUID productId) {
        when(saveProduct.execute(any(SaveProductRequest.class)))
                .thenReturn(ProductResponse.fromDomain(product(productId)));
    }

    private void givenProductIsUpdated(UUID productId) {
        when(updateProduct.execute(any(UpdateProductRequest.class)))
                .thenReturn(ProductResponse.fromDomain(product(productId)));
    }

    private ResultActions whenUpdatingPriceWithQueryParams(
            UUID productId,
            UUID buffetId) throws Exception {
        return mockMvc.perform(patch("/api/v1/products/{productId}/precio", productId)
                .param("nuevoPrecio", "120.00")
                .param("buffetId", buffetId.toString()));
    }

    private ResultActions whenUpdatingPriceWithJsonBody(
            UUID productId,
            UUID buffetId) throws Exception {
        return mockMvc.perform(patch("/api/v1/products/{productId}/precio", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "nuevoPrecio", "120.00",
                        "buffetId", buffetId.toString()
                ))));
    }

    private ResponseEntity<Void> whenUpdatingPriceWithOnlyQueryPrice(UUID productId) {
        return controller.updatePrice(
                productId,
                new BigDecimal("120.00"),
                null,
                null
        );
    }

    private ResponseEntity<List<ProductResponseDTO>> whenFindingAllProducts(UUID buffetId) {
        return controller.findAll(buffetId);
    }

    private ResponseEntity<ProductResponseDTO> whenFindingProductById(
            UUID productId,
            UUID buffetId) {
        return controller.findById(productId, buffetId);
    }

    private ResponseEntity<ProductResponseDTO> whenCreatingProduct(ProductRequestDTO request) {
        return controller.create(request);
    }

    private ResponseEntity<ProductResponseDTO> whenUpdatingProduct(
            UUID productId,
            ProductRequestDTO request) {
        return controller.update(productId, request);
    }

    private ResponseEntity<Void> whenDeletingProduct(UUID productId) {
        return controller.delete(productId);
    }

    private void thenResultIsOk(ResultActions result) throws Exception {
        result.andExpect(status().isOk());
    }

    private void thenResponseIsOk(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void thenUpdatePriceIsDelegated(UUID productId, UUID buffetId) {
        verify(updateProductPrice).execute(
                new UpdateProductPriceRequest(productId, new BigDecimal("120.00"), buffetId));
    }
    
    private void thenThrowsIllegalArgumentException(Executable execution) {
        assertThrows(IllegalArgumentException.class, execution);
    }
    
    private void thenFileIsParsedProperly(ResponseEntity<BulkStockParseResponse> response, byte[] content) {
        ArgumentCaptor<com.recred.backend.application.ParseBulkStockRequest> captor =
                ArgumentCaptor.forClass(
                        com.recred.backend.application.ParseBulkStockRequest.class);
        verify(parseBulkStock).execute(captor.capture());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Yogur", response.getBody().products().get(0).nombre());
        assertArrayEquals(content, captor.getValue().fileContent());
        assertEquals("text/csv", captor.getValue().contentType());
        verify(planAccessGuard).requireVendorAdvanced(null);
    }
    
    private void thenThrowsWrappedIoException(Executable execution) {
        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Error reading uploaded file", exception.getMessage());
    }
    
    private void thenBulkProductsAreSavedAndReturned(
            ResponseEntity<List<ProductResponseDTO>> response, 
            UUID productId, 
            UUID buffetId
    ) {
        ArgumentCaptor<SaveBulkProductsRequest> captor =
                ArgumentCaptor.forClass(SaveBulkProductsRequest.class);
        verify(saveBulkProducts).execute(captor.capture());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, captor.getValue().products().size());
        assertEquals(buffetId, captor.getValue().products().get(0).buffetId());
        assertEquals(productId, response.getBody().get(0).id());
        verify(planAccessGuard).requireVendorAdvanced(null);
    }
    
    private void thenProductsAreReturned(
            ResponseEntity<List<ProductResponseDTO>> response, 
            UUID productId, 
            UUID buffetId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productId, response.getBody().get(0).id());
        assertEquals("Yogur", response.getBody().get(0).nombre());
        verify(getAllProducts).execute(new GetAllProductsRequest(buffetId));
    }
    
    private void thenProductIsReturned(
            ResponseEntity<ProductResponseDTO> response, 
            UUID productId, 
            UUID buffetId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productId, response.getBody().id());
        verify(getProductById).execute(new GetProductByIdRequest(productId, buffetId));
    }
    
    private void thenProductIsCreated(
            ResponseEntity<ProductResponseDTO> response, 
            UUID productId, 
            UUID buffetId
    ) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(productId, response.getBody().id());

        ArgumentCaptor<SaveProductRequest> commandCaptor =
                ArgumentCaptor.forClass(SaveProductRequest.class);
        verify(saveProduct).execute(commandCaptor.capture());
        assertEquals("Yogur", commandCaptor.getValue().nombre());
        assertEquals(buffetId, commandCaptor.getValue().buffetId());
    }
    
    private void thenProductIsUpdated(
            ResponseEntity<ProductResponseDTO> response, 
            UUID productId, 
            UUID buffetId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productId, response.getBody().id());

        ArgumentCaptor<UpdateProductRequest> commandCaptor =
                ArgumentCaptor.forClass(UpdateProductRequest.class);
        verify(updateProduct).execute(commandCaptor.capture());
        assertEquals(productId, commandCaptor.getValue().id());
        assertEquals(buffetId, commandCaptor.getValue().buffetId());
    }
    
    private void thenProductIsDeleted(ResponseEntity<Void> response, UUID productId) {
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteProduct).execute(new DeleteProductRequest(productId));
    }

    private ProductRequestDTO productRequest(UUID buffetId) {
        return new ProductRequestDTO(
                "Yogur",
                "Yogur bebible",
                new BigDecimal("120.00"),
                new BigDecimal("200.00"),
                false,
                CATEGORY_ID,
                null,
                buffetId,
                5,
                null,
                null,
                Set.of(),
                Set.of(),
                null
        );
    }

    private BulkProductData bulkProductData() {
        return new BulkProductData(
                "Yogur",
                "Yogur bebible",
                new BigDecimal("120.00"),
                new BigDecimal("200.00"),
                false,
                CATEGORY_ID,
                null,
                5,
                Set.of(),
                Set.of()
        );
    }

    private Producto product(UUID productId) {
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Yogur");
        product.setDescripcion("Yogur bebible");
        product.setPrecio(new BigDecimal("120.00"));
        product.setPeso(new BigDecimal("200.00"));
        product.setRequierePreparacion(false);
        product.setActivo(true);
        product.setStockActual(5);
        product.setStockReservado(1);
        return product;
    }
}
