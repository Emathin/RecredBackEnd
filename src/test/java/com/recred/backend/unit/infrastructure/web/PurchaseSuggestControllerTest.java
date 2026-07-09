package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateAdvancePurchaseRequest;
import com.recred.backend.application.CreateAdvancePurchaseResponse;
import com.recred.backend.application.NotifyPurchaseSuggestions;
import com.recred.backend.application.NotifyPurchaseSuggestionsRequest;
import com.recred.backend.application.NotifyPurchaseSuggestionsResponse;
import com.recred.backend.application.PushNotificationResponse;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.RespondToAISuggestionRequest;
import com.recred.backend.application.PrizeHealthyPurchase;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.PurchaseSuggestController;
import com.recred.backend.infrastructure.web.TestSuggestionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PurchaseSuggestController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseSuggestControllerTest {

    private static final String TEST_COGNITO_ID = "user-123";
    private static final String TEST_NOTIFICATION_URL =
            "/api/v1/sugerencias-consumo/test-notificacion";
    private static final String BUY_SUGGESTION_URL =
            "/api/v1/sugerencias-consumo/comprar";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseSuggestController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private NotifyPurchaseSuggestions notifyPurchaseSuggestions;

    @MockitoBean
    private RespondToAISuggestion respondToAISuggestion;

    @MockitoBean
    private CreateAdvancePurchase createAdvancePurchase;

    @MockitoBean
    private PrizeHealthyPurchase prizeHealthyPurchase;

    @MockitoBean
    private SugerenciaIARepository sugerenciaIARepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private TutorRepository tutorRepository;

    @MockitoBean
    private PlanAccessGuard planAccessGuard;
    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    private UUID sugerenciaId;
    private UUID alumnoId;
    private UUID productoId;
    private UUID usuarioId;
    private TestingAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        sugerenciaId = randomUUID();
        alumnoId = randomUUID();
        productoId = randomUUID();
        usuarioId = randomUUID();

        auth = new TestingAuthenticationToken(TEST_COGNITO_ID, null);
    }

    @Test
    void testNotification_WhenExists_ShouldReturn200AndNotification() throws Exception {
        givenPurchaseSuggestionNotification();

        ResultActions result = whenPostingTestNotification(requestWithoutSuggestion());

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(usuarioId.toString()))
                .andExpect(jsonPath("$.data.type").value("PURCHASE_SUGGESTION"));
        verify(planAccessGuard).requireTutorOrStudentAdvanced(auth);
    }

    @Test
    void buySuggestion_ShouldProcessAdvancePurchase() throws Exception {
        givenSuggestionCanBePurchasedBy(RolUsuario.PADRE);

        ResultActions result = whenPostingBuySuggestion(requestWithSuggestion());

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists());
        verify(planAccessGuard).requireTutorOrStudentAdvanced(auth);
        verify(respondToAISuggestion).execute(
                new RespondToAISuggestionRequest(sugerenciaId, true));
    }

    @Test
    void testNotification_WhenNoNotificationAndAuthenticated_ShouldReturn204() {
        givenNoPurchaseSuggestionNotification();

        ResponseEntity<PushNotificationResponse> response =
                whenTestingNotificationWithAuthentication();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testNotification_WithoutAuthenticatedUser_ShouldFail() {
        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> controller.testNotification(null, requestWithoutUser())
        );
    }

    @Test
    void buySuggestion_WhenSuggestionDoesNotExist_ShouldFail() {
        givenSuggestionDoesNotExist();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> whenBuyingSuggestion());

        assertEquals("Sugerencia no encontrada", exception.getMessage());
    }

    @Test
    void buySuggestion_WhenUserIsStudentAndTutorExists_ShouldCreatePurchase() {
        Tutor tutor = tutor();
        givenSuggestionCanBePurchasedBy(RolUsuario.ALUMNO);
        givenTutorForStudent(tutor);

        ResponseEntity<AdvancePurchaseResponse> result = whenBuyingSuggestion();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(createAdvancePurchase).execute(any(CreateAdvancePurchaseRequest.class));
    }

    private void givenPurchaseSuggestionNotification() {
        PushNotificationResponse notification = new PushNotificationResponse(
                usuarioId,
                "Titulo",
                "Mensaje",
                Map.of("type", "PURCHASE_SUGGESTION", "producto", "{}")
        );

        when(notifyPurchaseSuggestions.execute(any(NotifyPurchaseSuggestionsRequest.class)))
                .thenReturn(new NotifyPurchaseSuggestionsResponse(notification));
    }

    private void givenNoPurchaseSuggestionNotification() {
        when(notifyPurchaseSuggestions.execute(new NotifyPurchaseSuggestionsRequest(
                TEST_COGNITO_ID
        ))).thenReturn(new NotifyPurchaseSuggestionsResponse(null));
    }

    private void givenSuggestionCanBePurchasedBy(RolUsuario rol) {
        when(sugerenciaIARepository.findById(sugerenciaId))
                .thenReturn(Optional.of(sugerenciaEntity()));
        when(usuarioService.obtenerUsuarioActual(anyString()))
                .thenReturn(usuario(rol));
        when(createAdvancePurchase.execute(any(CreateAdvancePurchaseRequest.class)))
                .thenReturn(advancePurchaseResponse());
    }

    private void givenSuggestionDoesNotExist() {
        when(sugerenciaIARepository.findById(sugerenciaId)).thenReturn(Optional.empty());
    }

    private void givenTutorForStudent(Tutor tutor) {
        when(tutorRepository.findByAlumnoId(alumnoId)).thenReturn(List.of(tutor));
    }

    private ResultActions whenPostingTestNotification(TestSuggestionRequest request)
            throws Exception {
        return postTestNotification(request);
    }

    private ResultActions whenPostingBuySuggestion(TestSuggestionRequest request)
            throws Exception {
        return postBuySuggestion(request);
    }

    private ResponseEntity<PushNotificationResponse> whenTestingNotificationWithAuthentication() {
        return controller.testNotification(auth, requestWithoutUser());
    }

    private ResponseEntity<AdvancePurchaseResponse> whenBuyingSuggestion() {
        return controller.buySuggestion(auth, requestWithOnlySuggestion());
    }

    private ResultActions postTestNotification(TestSuggestionRequest request) throws Exception {
        return postJson(TEST_NOTIFICATION_URL, request);
    }

    private ResultActions postBuySuggestion(TestSuggestionRequest request) throws Exception {
        return postJson(BUY_SUGGESTION_URL, request);
    }

    private ResultActions postJson(String path, TestSuggestionRequest request) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(auth));
    }

    private TestSuggestionRequest requestWithoutSuggestion() {
        return new TestSuggestionRequest(TEST_COGNITO_ID, null);
    }

    private TestSuggestionRequest requestWithSuggestion() {
        return new TestSuggestionRequest(TEST_COGNITO_ID, sugerenciaId);
    }

    private TestSuggestionRequest requestWithoutUser() {
        return new TestSuggestionRequest(null, null);
    }

    private TestSuggestionRequest requestWithOnlySuggestion() {
        return new TestSuggestionRequest(null, sugerenciaId);
    }

    private SugerenciaIAEntity sugerenciaEntity() {
        ProductoEntity producto = new ProductoEntity();
        producto.setId(productoId);

        AlumnoEntity alumno = new AlumnoEntity();
        alumno.setId(alumnoId);

        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(randomUUID());

        SugerenciaIAEntity sugerencia = new SugerenciaIAEntity();
        sugerencia.setId(sugerenciaId);
        sugerencia.setAlumnoEntity(alumno);
        sugerencia.setProductoEntity(producto);
        sugerencia.setBuffetEntity(buffet);
        return sugerencia;
    }

    private Usuario usuario(RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setRol(rol);
        return usuario;
    }

    private Tutor tutor() {
        Tutor tutor = new Tutor();
        tutor.setId(randomUUID());
        return tutor;
    }

    private CreateAdvancePurchaseResponse advancePurchaseResponse() {
        return new CreateAdvancePurchaseResponse(
                randomUUID().toString(),
                new PurchaseStatus(
                        randomUUID(),
                        alumnoId,
                        EstadoCompra.PENDIENTE,
                        "Pendiente",
                        LocalDateTime.now()
                ),
                100.00,
                Collections.singletonMap(alumnoId, "CODE123")
        );
    }
}
