package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateAdvancePurchase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;



import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doThrow;

@WebMvcTest(
        controllers = PurchaseController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class // Esto evita que busque el filtro JWT
        }
)
@AutoConfigureMockMvc(addFilters = false)

class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtDecoder jwtDecoder;
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
    @MockitoBean
    private CreateAdvancePurchase createAdvancePurchase;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;


    private UUID studentId;
    private UUID productId;
    private UUID purchaseId;
    private ProcessPurchaseRequestDTO requestDTO;
    private Purchase purchase;
    private AdvancePurchaseRequest advancePurchaseRequest;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        productId = UUID.randomUUID();
        purchaseId = UUID.randomUUID();

        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Producto Test");
        product.setPrecio(new BigDecimal("500.00"));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        PurchaseItemDTO itemDTO = new PurchaseItemDTO(product.getId(), product.getNombre(),
                1, product.getPrecio());
        requestDTO = new ProcessPurchaseRequestDTO(studentId, Collections.singletonList(itemDTO));

        Alumno student = new Alumno();
        student.setId(studentId);

        purchase = new Purchase(student, Collections.emptyMap(), null, studentId, BuyerType.STUDENT);
        purchase.setId(purchaseId);
        purchase.setStatus(EstadoCompra.ENTREGADO);
        purchase.setDate(LocalDateTime.now());

        when(processPresentialPurchase.execute(eq(studentId), anyMap())).thenReturn(purchase);

        AdvancePurchaseRequest.ItemRequest itemRequest = new AdvancePurchaseRequest.ItemRequest(productId, 1);
        AdvancePurchaseRequest.OrderRequest orderRequest = new AdvancePurchaseRequest.OrderRequest(
                studentId,
                UUID.randomUUID(),
                BuyerType.TUTOR,
                "2024-07-30",
                RecessTime.FIRST_RECESS,
                List.of(itemRequest)
        );
        advancePurchaseRequest = new AdvancePurchaseRequest(List.of(orderRequest));
    }

    @Test
    void processPurchase_WithValidRequest_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/purchases/presential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchaseId.toString()))
                .andExpect(jsonPath("$.status").value(EstadoCompra.ENTREGADO.name()));

        verify(processPresentialPurchase).execute(eq(studentId), anyMap());
    }

    @Test
    void createAdvancePurchase_WithValidRequest_ShouldReturnOk() throws Exception {
        AdvancePurchaseResponse advancePurchaseResponse = new AdvancePurchaseResponse(
                purchaseId.toString(),
                new PurchaseStatus(purchaseId, studentId, EstadoCompra.PENDIENTE, "A preparar", LocalDateTime.now()),
                500.00,
                Collections.singletonMap(studentId, "ABC-123")
        );

        when(createAdvancePurchase.execute(any(AdvancePurchaseRequest.class), eq("test-user"))).thenReturn(advancePurchaseResponse);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("test-user", "");

        mockMvc.perform(post("/api/v1/purchases/advance")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(advancePurchaseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(purchaseId.toString()));

        verify(createAdvancePurchase).execute(any(AdvancePurchaseRequest.class), eq("test-user"));
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
                productRepository,
                createAdvancePurchase
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
                new TestingAuthenticationToken("cognito-subject", "");
        PurchaseStatus purchaseStatus = new PurchaseStatus(
                purchaseId,
                studentId,
                EstadoCompra.PENDIENTE,
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
                .andExpect(jsonPath("$.status").value(EstadoCompra.PENDIENTE.name()))
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
                productRepository,
                createAdvancePurchase
        );

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.getPurchaseStatusById(purchaseId, null));
    }

    @Test
    void updatePurchaseStatus_WithValidRequest_ShouldReturnUpdatedStatus() throws Exception {
        PurchaseStatus purchaseStatus = new PurchaseStatus(
                purchaseId,
                studentId,
                EstadoCompra.EN_PREPARACION,
                "En preparacion",
                LocalDateTime.now()
        );
        when(updatePurchaseStatus.execute(purchaseId, EstadoCompra.EN_PREPARACION))
                .thenReturn(purchaseStatus);

        mockMvc.perform(patch("/api/v1/purchases/{id}/status", purchaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePurchaseStatusRequestDTO(
                                        EstadoCompra.EN_PREPARACION
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EstadoCompra.EN_PREPARACION.name()))
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