package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PurchaseController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessPresentialPurchase processPresentialPurchase;

    @MockBean
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
}
