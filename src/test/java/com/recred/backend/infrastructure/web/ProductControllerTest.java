package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.UpdateProductPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private UpdateProductPrice updateProductPrice;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ProductController(updateProductPrice)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void updatePriceAcceptsQueryParams() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/productos/{productId}/precio", productId)
                        .param("nuevoPrecio", "120.00")
                        .param("buffetId", buffetId.toString()))
                .andExpect(status().isOk());

        verify(updateProductPrice).execute(productId, new BigDecimal("120.00"), buffetId);
    }

    @Test
    void updatePriceAcceptsJsonBody() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/productos/{productId}/precio", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nuevoPrecio", "120.00",
                                "buffetId", buffetId.toString()
                        ))))
                .andExpect(status().isOk());

        verify(updateProductPrice).execute(productId, new BigDecimal("120.00"), buffetId);
    }
}
