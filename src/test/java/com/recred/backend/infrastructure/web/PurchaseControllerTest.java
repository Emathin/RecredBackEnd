package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.domain.exceptions.StudentNotFoundException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.
        OAuth2ResourceServerAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PurchaseController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class}
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
    private GetPurchaseStatus getPurchaseStatus;
    @MockitoBean
    private UpdatePurchaseStatus updatePurchaseStatus;
    @MockitoBean
    private CancelPresentialPurchase cancelPresentialPurchase;
    @MockitoBean
    private ProductRepository productRepository;

    private UUID studentId;
    private UUID productId;
    private UUID purchaseId;
    private ProcessPurchaseRequestDTO requestDTO;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        productId = UUID.randomUUID();
        purchaseId = UUID.randomUUID();

        PurchaseItemDTO itemDTO = new PurchaseItemDTO(productId, 1);
        requestDTO = new ProcessPurchaseRequestDTO(studentId, Collections.singletonList(itemDTO));

        Product product = new Product();
        product.setId(productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Student student = new Student();
        student.setId(studentId);

        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStudent(student);
        purchase.setStatus(PurchaseState.ENTREGADO);
        purchase.setDate(LocalDateTime.now());

        when(processPresentialPurchase.execute(eq(studentId), anyMap())).thenReturn(purchase);
    }

    @Test
    void processPurchase_WithValidRequest_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/purchases/presential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchaseId.toString()))
                .andExpect(jsonPath("$.status").value(PurchaseState.ENTREGADO.name()));

        verify(processPresentialPurchase).execute(eq(studentId), anyMap());
    }

    @Test
    void processPurchase_WhenStudentNotFound_ShouldReturnNotFound() throws Exception {
        doThrow(new StudentNotFoundException(studentId))
                .when(processPresentialPurchase).execute(eq(studentId), anyMap());

        mockMvc.perform(post("/api/v1/purchases/presential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void processPurchase_WhenInsufficientBalance_ShouldReturnBadRequest() throws Exception {
        doThrow(new InsufficientBalanceException(""))
                .when(processPresentialPurchase)
                .execute(eq(studentId), anyMap());

        mockMvc.perform(post("/api/v1/purchases/presential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processPurchase_WhenProductDoesNotExist_ShouldThrowException() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        PurchaseController controller = new PurchaseController(
                processPresentialPurchase,
                getPresentialPurchase,
                getPurchaseStatus,
                updatePurchaseStatus,
                cancelPresentialPurchase,
                productRepository
        );

        assertThrows(RuntimeException.class,
                () -> controller.processPurchase(requestDTO));
    }

    @Test
    void getPurchaseById_WhenPurchaseExists_ShouldReturnPurchase() throws Exception {
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
    void getPurchaseStatusById_WhenPurchaseExists_ShouldReturnStatus() throws Exception {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("cognito-subject", null);
        PurchaseStatus purchaseStatus = new PurchaseStatus(
                purchaseId,
                studentId,
                PurchaseState.PENDIENTE,
                "A preparar",
                LocalDateTime.now()
        );
        when(getPurchaseStatus.execute("cognito-subject", purchaseId))
                .thenReturn(purchaseStatus);

        mockMvc.perform(get("/api/v1/purchases/{id}/status", purchaseId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseId").value(purchaseId.toString()))
                .andExpect(jsonPath("$.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.status").value(PurchaseState.PENDIENTE.name()))
                .andExpect(jsonPath("$.statusLabel").value("A preparar"));
    }

    @Test
    void getPurchaseStatusById_WhenAuthenticationIsMissing_ShouldThrowException() {
        PurchaseController controller = new PurchaseController(
                processPresentialPurchase,
                getPresentialPurchase,
                getPurchaseStatus,
                updatePurchaseStatus,
                cancelPresentialPurchase,
                productRepository
        );

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.getPurchaseStatusById(purchaseId, null));
    }

    @Test
    void updatePurchaseStatus_WithValidRequest_ShouldReturnUpdatedStatus() throws Exception {
        PurchaseStatus purchaseStatus = new PurchaseStatus(
                purchaseId,
                studentId,
                PurchaseState.EN_PREPARACION,
                "En preparacion",
                LocalDateTime.now()
        );
        when(updatePurchaseStatus.execute(purchaseId, PurchaseState.EN_PREPARACION))
                .thenReturn(purchaseStatus);

        mockMvc.perform(patch("/api/v1/purchases/{id}/status", purchaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePurchaseStatusRequestDTO(
                                        PurchaseState.EN_PREPARACION
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(PurchaseState.EN_PREPARACION.name()))
                .andExpect(jsonPath("$.statusLabel").value("En preparacion"));
    }

    @Test
    void cancelPurchase_WithValidId_ShouldReturnOk() throws Exception {
        UUID purchaseId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/purchases/{id}/cancel", purchaseId))
                .andExpect(status().isOk());

        verify(cancelPresentialPurchase).execute(purchaseId);
    }
}
