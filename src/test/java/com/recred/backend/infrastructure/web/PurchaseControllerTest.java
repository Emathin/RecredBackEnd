package com.recred.backend.infrastructure.web;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PurchaseController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProcessPresentialPurchase processPresentialPurchase;
    @MockitoBean
    private GetPresentialPurchase getPresentialPurchase;
    @MockitoBean
    private CancelPresentialPurchase cancelPresentialPurchase;
    @MockitoBean
    private ProductRepository productRepository;

    private UUID studentId;
    private UUID productId;
    private ProcessPurchaseRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        productId = UUID.randomUUID();

        PurchaseItemDTO itemDTO = new PurchaseItemDTO(productId, 1);
        requestDTO = new ProcessPurchaseRequestDTO(studentId, Collections.singletonList(itemDTO));

        Producto product = new Producto();
        product.setId(productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    }

    @Test
    void processPurchase_WithValidRequest_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/purchases/presential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());

        verify(processPresentialPurchase).execute(eq(studentId), anyMap());
    }

    @Test
    void processPurchase_WhenStudentNotFound_ShouldReturnNotFound() throws Exception {
        doThrow(new AlumnoNoEncontradoException(studentId))
                .when(processPresentialPurchase).execute(eq(studentId), anyMap());

        mockMvc.perform(post("/api/v1/purchases/presential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void processPurchase_WhenInsufficientBalance_ShouldReturnBadRequest() throws Exception {
        doThrow(new InsufficientBalanceException("")).when(processPresentialPurchase).execute(eq(studentId), anyMap());

        mockMvc.perform(post("/api/v1/purchases/presential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPurchaseById_WhenPurchaseExists_ShouldReturnPurchase() throws Exception {
        UUID purchaseId = UUID.randomUUID();
        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        when(getPresentialPurchase.execute(purchaseId)).thenReturn(Optional.of(purchase));

        mockMvc.perform(get("/api/v1/purchases/{id}", purchaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchaseId.toString()));
    }

    @Test
    void getPurchaseById_WhenPurchaseDoesNotExist_ShouldReturnNotFound() throws Exception {
        UUID purchaseId = UUID.randomUUID();
        when(getPresentialPurchase.execute(purchaseId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/purchases/{id}", purchaseId))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelPurchase_WithValidId_ShouldReturnOk() throws Exception {
        UUID purchaseId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/purchases/{id}/cancel", purchaseId))
                .andExpect(status().isOk());

        verify(cancelPresentialPurchase).execute(purchaseId);
    }
}
