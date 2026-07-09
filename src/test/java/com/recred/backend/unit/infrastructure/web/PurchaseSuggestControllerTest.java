package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateAdvancePurchaseRequest;
import com.recred.backend.application.CreateAdvancePurchaseResponse;
import com.recred.backend.application.NotifyPurchaseSuggestions;
import com.recred.backend.application.NotifyPurchaseSuggestionsRequest;
import com.recred.backend.application.NotifyPurchaseSuggestionsResponse;
import com.recred.backend.application.PrizeHealthyPurchase;
import com.recred.backend.application.PushNotificationResponse;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.RespondToAISuggestionRequest;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.recred.backend.domain.models.RolUsuario.ALUMNO;
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

@ExtendWith(MockitoExtension.class)
class PurchaseSuggestControllerTest {

    private static final String TEST_COGNITO_ID = "user-123";
    private static final String TEST_NOTIFICATION_URL =
            "/api/v1/sugerencias-consumo/test-notificacion";
    private static final String BUY_SUGGESTION_URL =
            "/api/v1/sugerencias-consumo/comprar";
    private static final UUID SUGERENCIA_ID = randomUUID();
    private static final UUID ALUMNO_ID = randomUUID();
    private static final UUID PRODUCTO_ID = randomUUID();
    private static final UUID USUARIO_ID = randomUUID();
    private static final UUID BUFFET_ID = randomUUID();

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private PurchaseSuggestController controller;
    private TestingAuthenticationToken auth;

    @Mock private NotifyPurchaseSuggestions notifyPurchaseSuggestions;
    @Mock private RespondToAISuggestion respondToAISuggestion;
    @Mock private CreateAdvancePurchase createAdvancePurchase;
    @Mock private PrizeHealthyPurchase prizeHealthyPurchase;
    @Mock private SugerenciaIARepository sugerenciaIARepository;
    @Mock private UsuarioService usuarioService;
    @Mock private TutorRepository tutorRepository;
    @Mock private PlanAccessGuard planAccessGuard;

    @BeforeEach
    void setUp() {
        controller = new PurchaseSuggestController(
                notifyPurchaseSuggestions,
                respondToAISuggestion,
                createAdvancePurchase,
                prizeHealthyPurchase,
                sugerenciaIARepository,
                usuarioService,
                tutorRepository,
                planAccessGuard
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        auth = new TestingAuthenticationToken(TEST_COGNITO_ID, null);
    }

    @Test
    void testNotification_WhenExists_ShouldReturn200AndNotification() throws Exception {
        givenPurchaseSuggestionNotification();

        ResultActions result = whenPostingTestNotification();

        thenNotificationIsReturned(result);
        thenTutorOrStudentAdvancedIsRequired();
    }

    @Test
    void buySuggestion_ShouldProcessAdvancePurchase() throws Exception {
        givenSuggestionCanBePurchasedBy(RolUsuario.PADRE);

        ResultActions result = whenPostingBuySuggestion();

        thenAdvancePurchaseIsProcessed(result);
        thenTutorOrStudentAdvancedIsRequired();
        thenAISuggestionIsResponded();
    }

    @Test
    void testNotification_WhenNoNotificationAndAuthenticated_ShouldReturn204() {
        givenNoPurchaseSuggestionNotification();

        ResponseEntity<PushNotificationResponse> response =
                whenTestingNotificationWithAuthentication();

        thenNoContentIsReturned(response);
    }

    @Test
    void testNotification_WithoutAuthenticatedUser_ShouldFail() {
        Executable execution = this::whenTestingNotificationWithoutAuthentication;

        thenThrowsAuthenticationCredentialsNotFoundException(execution);
    }

    @Test
    void buySuggestion_WhenSuggestionDoesNotExist_ShouldFail() {
        givenSuggestionDoesNotExist();

        Executable execution = this::whenBuyingSuggestion;

        thenThrowsSuggestionNotFoundException(execution);
    }

    @Test
    void buySuggestion_WhenUserIsStudentAndTutorExists_ShouldCreatePurchase() {
        givenSuggestionCanBePurchasedBy(ALUMNO);
        givenTutorForStudent();

        ResponseEntity<AdvancePurchaseResponse> result = whenBuyingSuggestion();

        thenOkIsReturned(result);
        thenAdvancePurchaseIsCreated();
    }

    private void givenPurchaseSuggestionNotification() {
        PushNotificationResponse notification = new PushNotificationResponse(
                USUARIO_ID,
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
        when(sugerenciaIARepository.findById(SUGERENCIA_ID))
                .thenReturn(Optional.of(sugerenciaEntity()));
        when(usuarioService.obtenerUsuarioActual(anyString()))
                .thenReturn(usuario(rol));
        when(createAdvancePurchase.execute(any(CreateAdvancePurchaseRequest.class)))
                .thenReturn(advancePurchaseResponse());
    }

    private void givenSuggestionDoesNotExist() {
        when(sugerenciaIARepository.findById(SUGERENCIA_ID)).thenReturn(Optional.empty());
    }

    private void givenTutorForStudent() {
        Tutor tutor = new Tutor();
        tutor.setId(randomUUID());
        when(tutorRepository.findByAlumnoId(ALUMNO_ID)).thenReturn(List.of(tutor));
    }

    private ResultActions whenPostingTestNotification() throws Exception {
        return mockMvc.perform(post(TEST_NOTIFICATION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestSuggestionRequest(TEST_COGNITO_ID, null)))
                .principal(auth));
    }

    private ResultActions whenPostingBuySuggestion() throws Exception {
        return mockMvc.perform(post(BUY_SUGGESTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestSuggestionRequest(TEST_COGNITO_ID, SUGERENCIA_ID)))
                .principal(auth));
    }

    private ResponseEntity<PushNotificationResponse> whenTestingNotificationWithAuthentication() {
        return controller.testNotification(auth, new TestSuggestionRequest(null, null));
    }

    private void whenTestingNotificationWithoutAuthentication() {
        controller.testNotification(null, new TestSuggestionRequest(null, null));
    }

    private ResponseEntity<AdvancePurchaseResponse> whenBuyingSuggestion() {
        return controller.buySuggestion(auth, new TestSuggestionRequest(null, SUGERENCIA_ID));
    }

    private void thenNotificationIsReturned(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.data.type").value("PURCHASE_SUGGESTION"));
    }

    private void thenAdvancePurchaseIsProcessed(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists());
    }

    private void thenNoContentIsReturned(ResponseEntity<PushNotificationResponse> response) {
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    private void thenOkIsReturned(ResponseEntity<AdvancePurchaseResponse> result) {
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    private void thenThrowsAuthenticationCredentialsNotFoundException(Executable execution) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }

    private void thenThrowsSuggestionNotFoundException(Executable execution) {
        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Sugerencia no encontrada", exception.getMessage());
    }

    private void thenAdvancePurchaseIsCreated() {
        verify(createAdvancePurchase).execute(any(CreateAdvancePurchaseRequest.class));
    }

    private void thenTutorOrStudentAdvancedIsRequired() {
        verify(planAccessGuard).requireTutorOrStudentAdvanced(auth);
    }

    private void thenAISuggestionIsResponded() {
        verify(respondToAISuggestion).execute(
                new RespondToAISuggestionRequest(SUGERENCIA_ID, true));
    }

    private SugerenciaIAEntity sugerenciaEntity() {
        ProductoEntity producto = new ProductoEntity();
        producto.setId(PRODUCTO_ID);
        AlumnoEntity alumno = new AlumnoEntity();
        alumno.setId(ALUMNO_ID);
        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(BUFFET_ID);

        SugerenciaIAEntity sugerencia = new SugerenciaIAEntity();
        sugerencia.setId(SUGERENCIA_ID);
        sugerencia.setAlumnoEntity(alumno);
        sugerencia.setProductoEntity(producto);
        sugerencia.setBuffetEntity(buffet);
        return sugerencia;
    }

    private Usuario usuario(RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setRol(rol);
        return usuario;
    }

    private CreateAdvancePurchaseResponse advancePurchaseResponse() {
        return new CreateAdvancePurchaseResponse(
                randomUUID().toString(),
                new PurchaseStatus(
                        randomUUID(),
                        ALUMNO_ID,
                        EstadoCompra.PENDIENTE,
                        "Pendiente",
                        LocalDateTime.now()
                ),
                100.00,
                Collections.singletonMap(ALUMNO_ID, "CODE123")
        );
    }
}
