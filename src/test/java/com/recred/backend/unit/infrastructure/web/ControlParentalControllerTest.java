package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.AddHealthRestrictionRequest;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DetectStudentPreferencesRequest;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.GetHealthRestrictionsRequest;
import com.recred.backend.application.GetHealthRestrictionsResponse;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.ManageRestrictionsRequest;
import com.recred.backend.application.ManualProductLock;
import com.recred.backend.application.ManualProductLockRequest;
import com.recred.backend.application.ManualProductLockResponse;
import com.recred.backend.application.ManualProductUnlock;
import com.recred.backend.application.ManualProductUnlockRequest;
import com.recred.backend.application.ManualProductUnlockResponse;
import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.RemoveHealthRestrictionsRequest;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.infrastructure.web.ActualizarRestriccionesDTO;
import com.recred.backend.infrastructure.web.ControlParentalController;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.TutorAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ControlParentalController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
class ControlParentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManualProductLock manualProductLock;

    @MockitoBean
    private ManualProductUnlock manualProductUnlock;

    @MockitoBean
    private ManageRestrictions manageRestrictions;

    @MockitoBean
    private GetHealthRestrictions getHealthRestrictions;

    @MockitoBean
    private AddHealthRestriction addHealthRestriction;

    @MockitoBean
    private RemoveHealthRestrictions removeHealthRestrictions;

    @MockitoBean
    private DetectStudentPreferences detectStudentPreferences;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private TutorAccessGuard tutorAccessGuard;

    @MockitoBean
    private PlanAccessGuard planAccessGuard;

    private UUID alumnoId;
    private UUID clasificacionId;
    private UUID studentId;
    private UUID classificationId;
    private UUID buffetId;
    private UUID productId;
    private ActualizarRestriccionesDTO updateRestrictionsRequest;
    private GetHealthRestrictionsResponse healthRestrictionResponse;
    private ResultActions response;

    @BeforeEach
    void setUp() {
        studentId = randomUUID();
        classificationId = randomUUID();
    }

    @Test
    void givenHealthRestrictionsUpdate_whenUpdatingRestrictions_thenReturnsOk() throws Exception {
        givenHealthRestrictionsUpdateRequest();

        whenUpdateHealthRestrictions();

        thenResponseIsOk();
        thenManageRestrictionsWasCalled();
    }

    @Test
    void givenHealthRestrictions_whenGettingRestrictions_thenReturnsRestrictions()
            throws Exception {
        givenStudentHasHealthRestrictions();

        whenGetHealthRestrictions();

        thenHealthRestrictionsAreReturned();
        thenGetHealthRestrictionsWasCalled();
    }

    @Test
    void givenHealthRestrictionRequest_whenAddingRestriction_thenReturnsOk() throws Exception {
        whenAddHealthRestriction();

        thenResponseIsOk();
        thenAddHealthRestrictionWasCalled();
    }

    @Test
    void givenHealthRestrictionRequest_whenRemovingRestriction_thenReturnsNoContent()
            throws Exception {
        whenRemoveHealthRestriction();

        thenResponseIsNoContent();
        thenRemoveHealthRestrictionWasCalled();
    }

    @Test
    void givenPreferenceAnalysisRequest_whenAnalyzingPreferences_thenReturnsAccepted()
            throws Exception {
        givenPreferenceAnalysisRequest();

        whenAnalyzePreferences();

        thenResponseIsAccepted();
        thenDetectStudentPreferencesWasCalled();
    }

    @Test
    void givenPreferenceAnalysisFailure_whenAnalyzingPreferences_thenReturnsInternalServerError()
            throws Exception {
        givenPreferenceAnalysisRequest();
        givenPreferenceAnalysisFails();

        whenAnalyzePreferences();

        thenResponseIsInternalServerError();
    }

    @Test
    void givenManualProductLockRequest_whenBlockingProduct_thenReturnsOk() throws Exception {
        givenManualProductLockRequest();
        givenManualProductLockSucceeds();

        whenBlockProduct();

        thenResponseIsOk();
        thenManualProductLockWasCalled();
        verify(detectStudentPreferences).execute(
                new DetectStudentPreferencesRequest(alumnoId, buffetId)
        );
        verify(planAccessGuard).requireTutorIntermediate(null);
    }

    @Test
    void givenManualProductUnlockRequest_whenUnlockingProduct_thenReturnsOk() throws Exception {
        givenManualProductUnlockRequest();
        givenManualProductUnlockSucceeds();

        whenUnlockProduct();

        thenResponseIsOk();
        thenManualProductUnlockWasCalled();
    }

    private void givenHealthRestrictionsUpdateRequest() {
        updateRestrictionsRequest = new ActualizarRestriccionesDTO();
        updateRestrictionsRequest.setClasificacionesIds(List.of(classificationId));
    }

    private void givenStudentHasHealthRestrictions() {
        healthRestrictionResponse = new GetHealthRestrictionsResponse(
                classificationId,
                "Celiaco",
                true
        );
        when(getHealthRestrictions.execute(new GetHealthRestrictionsRequest(studentId)))
                .thenReturn(Set.of(healthRestrictionResponse));
    }

    private void givenPreferenceAnalysisRequest() {
        buffetId = randomUUID();
    }

    private void givenPreferenceAnalysisFails() {
        doThrow(new RuntimeException("Error IA")).when(detectStudentPreferences)
                .execute(new DetectStudentPreferencesRequest(studentId, buffetId));
    }

    private void givenManualProductLockRequest() {
        productId = randomUUID();
    }

    private void givenManualProductUnlockRequest() {
        productId = randomUUID();
    }

    private void givenManualProductLockSucceeds() {
        when(manualProductLock.execute(new ManualProductLockRequest(studentId, productId)))
                .thenReturn(new ManualProductLockResponse());
    }

    private void givenManualProductUnlockSucceeds() {
        when(manualProductUnlock.execute(new ManualProductUnlockRequest(studentId, productId)))
                .thenReturn(new ManualProductUnlockResponse());
    }

    private void whenUpdateHealthRestrictions() throws Exception {
        response = mockMvc.perform(put(
                "/api/v1/control-parental/alumnos/{alumnoId}/actualizar-restricciones-salud",
                studentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRestrictionsRequest)));
    }

    private void whenGetHealthRestrictions() throws Exception {
        response = mockMvc.perform(get(
                "/api/v1/control-parental/alumnos/{alumnoId}/obtener-restricciones-salud",
                studentId)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private void whenAddHealthRestriction() throws Exception {
        response = mockMvc.perform(post(
                "/api/v1/control-parental/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}",
                studentId,
                classificationId)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private void whenRemoveHealthRestriction() throws Exception {
        response = mockMvc.perform(delete(
                "/api/v1/control-parental/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}",
                studentId,
                classificationId)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private void whenAnalyzePreferences() throws Exception {
        response = mockMvc.perform(post(
                "/api/v1/control-parental/alumnos/{alumnoId}/analizar-preferencias",
                studentId)
                .param("buffetId", buffetId.toString())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private void whenBlockProduct() throws Exception {
        response = mockMvc.perform(post(
                "/api/v1/control-parental/alumnos/{alumnoId}/productos-bloqueados/{productoId}",
                studentId,
                productId));
    }

    private void whenUnlockProduct() throws Exception {
        response = mockMvc.perform(delete(
                "/api/v1/control-parental/alumnos/{alumnoId}/productos-bloqueados/{productoId}",
                studentId,
                productId));
    }

    private void thenResponseIsOk() throws Exception {
        response.andExpect(status().isOk());
    }

    private void thenResponseIsNoContent() throws Exception {
        response.andExpect(status().isNoContent());
    }

    private void thenResponseIsAccepted() throws Exception {
        response.andExpect(status().isAccepted());
    }

    private void thenResponseIsInternalServerError() throws Exception {
        response.andExpect(status().isInternalServerError());
    }

    private void thenHealthRestrictionsAreReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(classificationId.toString()))
                .andExpect(jsonPath("$[0].descripcion").value("Celiaco"));
    }

    private void thenManageRestrictionsWasCalled() {
        verify(manageRestrictions).execute(new ManageRestrictionsRequest(
                studentId,
                updateRestrictionsRequest.getClasificacionesIds()));
    }

    private void thenGetHealthRestrictionsWasCalled() {
        verify(getHealthRestrictions).execute(new GetHealthRestrictionsRequest(studentId));
    }

    private void thenAddHealthRestrictionWasCalled() {
        verify(addHealthRestriction).execute(
                new AddHealthRestrictionRequest(studentId, classificationId));
    }

    private void thenRemoveHealthRestrictionWasCalled() {
        verify(removeHealthRestrictions).execute(
                new RemoveHealthRestrictionsRequest(studentId, classificationId));
    }

    private void thenDetectStudentPreferencesWasCalled() {
        verify(detectStudentPreferences).execute(
                new DetectStudentPreferencesRequest(studentId, buffetId));
    }

    private void thenManualProductLockWasCalled() {
        verify(manualProductLock).execute(new ManualProductLockRequest(studentId, productId));
    }

    private void thenManualProductUnlockWasCalled() {
        verify(manualProductUnlock).execute(
                new ManualProductUnlockRequest(studentId, productId));
    }
}
