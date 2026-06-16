package com.recred.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.DeleteProduct;
import com.recred.backend.application.GetAllProducts;
import com.recred.backend.application.GetProductById;
import com.recred.backend.application.SaveProductCommand;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductCommand;
import com.recred.backend.application.UpdateProductPrice;
import com.recred.backend.infrastructure.web.ProductController;
import com.recred.backend.infrastructure.web.ProductRequestDTO;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import com.recred.backend.domain.models.Producto;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
                updateProductPrice
        );
        mockMvc = standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void updatePriceAcceptsQueryParams() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/products/{productId}/precio", productId)
                        .param("nuevoPrecio", "120.00")
                        .param("buffetId", buffetId.toString()))
                .andExpect(status().isOk());

        verify(updateProductPrice).execute(productId, new BigDecimal("120.00"), buffetId);
    }

    @Test
    void updatePriceAcceptsJsonBody() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/products/{productId}/precio", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nuevoPrecio", "120.00",
                                "buffetId", buffetId.toString()
                        ))))
                .andExpect(status().isOk());

        verify(updateProductPrice).execute(productId, new BigDecimal("120.00"), buffetId);
    }

    @Test
    void updatePriceAcceptsOnlyQueryPriceAndNullBuffet() {
        UUID productId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.updatePrice(
                productId,
                new BigDecimal("120.00"),
                null,
                null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(updateProductPrice).execute(productId, new BigDecimal("120.00"), null);
    }

    @Test
    void updatePriceWithoutPriceThrowsIllegalArgumentException() {
        UUID productId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> controller.updatePrice(productId, null, null, null));
    }

    @Test
    void findAllReturnsMappedProducts() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(getAllProducts.execute(buffetId)).thenReturn(List.of(product(productId)));

        ResponseEntity<List<ProductResponseDTO>> response = controller.findAll(buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productId, response.getBody().get(0).id());
        assertEquals("Yogur", response.getBody().get(0).nombre());
        verify(getAllProducts).execute(buffetId);
    }

    @Test
    void findByIdReturnsMappedProduct() {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(getProductById.execute(productId, buffetId)).thenReturn(product(productId));

        ResponseEntity<ProductResponseDTO> response =
                controller.findById(productId, buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productId, response.getBody().id());
        verify(getProductById).execute(productId, buffetId);
    }

    @Test
    void createDelegatesSaveCommandAndReturnsCreatedProduct() {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        ProductRequestDTO request = productRequest(buffetId);
        when(saveProduct.execute(any(SaveProductCommand.class)))
                .thenReturn(product(productId));

        ResponseEntity<ProductResponseDTO> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(productId, response.getBody().id());
        ArgumentCaptor<SaveProductCommand> commandCaptor =
                ArgumentCaptor.forClass(SaveProductCommand.class);
        verify(saveProduct).execute(commandCaptor.capture());
        assertEquals("Yogur", commandCaptor.getValue().nombre());
        assertEquals(buffetId, commandCaptor.getValue().buffetId());
    }

    @Test
    void updateDelegatesUpdateCommandAndReturnsUpdatedProduct() {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        ProductRequestDTO request = productRequest(buffetId);
        when(updateProduct.execute(any(UpdateProductCommand.class)))
                .thenReturn(product(productId));

        ResponseEntity<ProductResponseDTO> response = controller.update(productId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productId, response.getBody().id());
        ArgumentCaptor<UpdateProductCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateProductCommand.class);
        verify(updateProduct).execute(commandCaptor.capture());
        assertEquals(productId, commandCaptor.getValue().id());
        assertEquals(buffetId, commandCaptor.getValue().buffetId());
    }

    @Test
    void deleteDelegatesAndReturnsNoContent() {
        UUID productId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.delete(productId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteProduct).execute(productId);
    }

    private ProductRequestDTO productRequest(UUID buffetId) {
        return new ProductRequestDTO(
                "Yogur",
                "Yogur bebible",
                new BigDecimal("120.00"),
                new BigDecimal("200.00"),
                false,
                UUID.randomUUID(),
                null,
                buffetId,
                5,
                null,
                null,
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
