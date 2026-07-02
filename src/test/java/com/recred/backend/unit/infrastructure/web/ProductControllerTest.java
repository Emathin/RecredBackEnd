package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SaveProductRequest;
import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductRequest;
import com.recred.backend.application.UpdateProductPrice;
import com.recred.backend.application.UpdateProductPriceRequest;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.web.ProductController;
import com.recred.backend.infrastructure.web.ProductRequestDTO;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
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
                saveBulkProducts
        );
        mockMvc = standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void updatePriceAcceptsQueryParams() throws Exception {
        UUID productId = randomUUID();
        UUID buffetId = randomUUID();

        ResultActions result = whenUpdatingPriceWithQueryParams(productId, buffetId);

        result.andExpect(status().isOk());
        verify(updateProductPrice).execute(
                new UpdateProductPriceRequest(productId, new BigDecimal("120.00"), buffetId));
    }

    @Test
    void updatePriceAcceptsJsonBody() throws Exception {
        UUID productId = randomUUID();
        UUID buffetId = randomUUID();

        ResultActions result = whenUpdatingPriceWithJsonBody(productId, buffetId);

        result.andExpect(status().isOk());
        verify(updateProductPrice).execute(
                new UpdateProductPriceRequest(productId, new BigDecimal("120.00"), buffetId));
    }

    @Test
    void updatePriceAcceptsOnlyQueryPriceAndNullBuffet() {
        UUID productId = randomUUID();

        ResponseEntity<Void> response = whenUpdatingPriceWithOnlyQueryPrice(productId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(updateProductPrice).execute(
                new UpdateProductPriceRequest(productId, new BigDecimal("120.00"), null));
    }

    @Test
    void updatePriceWithoutPriceThrowsIllegalArgumentException() {
        UUID productId = randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> controller.updatePrice(productId, null, null, null));
    }

    @Test
    void findAllReturnsMappedProducts() {
        UUID buffetId = randomUUID();
        UUID productId = randomUUID();
        givenProductsForBuffet(buffetId, ProductResponse.fromDomain(product(productId)));

        ResponseEntity<List<ProductResponseDTO>> response = whenFindingAllProducts(buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productId, response.getBody().get(0).id());
        assertEquals("Yogur", response.getBody().get(0).nombre());
        verify(getAllProducts).execute(new GetAllProductsRequest(buffetId));
    }

    @Test
    void findByIdReturnsMappedProduct() {
        UUID productId = randomUUID();
        UUID buffetId = randomUUID();
        givenProductExists(productId, buffetId, ProductResponse.fromDomain(product(productId)));

        ResponseEntity<ProductResponseDTO> response = whenFindingProductById(productId, buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productId, response.getBody().id());
        verify(getProductById).execute(new GetProductByIdRequest(productId, buffetId));
    }

    @Test
    void createDelegatesSaveCommandAndReturnsCreatedProduct() {
        UUID productId = randomUUID();
        UUID buffetId = randomUUID();
        ProductRequestDTO request = productRequest(buffetId);
        givenProductIsSaved(productId);

        ResponseEntity<ProductResponseDTO> response = whenCreatingProduct(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(productId, response.getBody().id());

        ArgumentCaptor<SaveProductRequest> commandCaptor =
                ArgumentCaptor.forClass(SaveProductRequest.class);
        verify(saveProduct).execute(commandCaptor.capture());
        assertEquals("Yogur", commandCaptor.getValue().nombre());
        assertEquals(buffetId, commandCaptor.getValue().buffetId());
    }

    @Test
    void updateDelegatesUpdateCommandAndReturnsUpdatedProduct() {
        UUID productId = randomUUID();
        UUID buffetId = randomUUID();
        ProductRequestDTO request = productRequest(buffetId);
        givenProductIsUpdated(productId);

        ResponseEntity<ProductResponseDTO> response = whenUpdatingProduct(productId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productId, response.getBody().id());

        ArgumentCaptor<UpdateProductRequest> commandCaptor =
                ArgumentCaptor.forClass(UpdateProductRequest.class);
        verify(updateProduct).execute(commandCaptor.capture());
        assertEquals(productId, commandCaptor.getValue().id());
        assertEquals(buffetId, commandCaptor.getValue().buffetId());
    }

    @Test
    void deleteDelegatesAndReturnsNoContent() {
        UUID productId = randomUUID();

        ResponseEntity<Void> response = whenDeletingProduct(productId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteProduct).execute(new DeleteProductRequest(productId));
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

    private ProductRequestDTO productRequest(UUID buffetId) {
        return new ProductRequestDTO(
                "Yogur",
                "Yogur bebible",
                new BigDecimal("120.00"),
                new BigDecimal("200.00"),
                false,
                randomUUID(),
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
