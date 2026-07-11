package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreateBudgetResponse;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableBudgetRequest;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetActiveBudgetByStudentRequest;
import com.recred.backend.application.GetActiveBudgetByStudentResponse;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRequest;
import com.recred.backend.application.UpdateBudgetResponse;
import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdateBudgetRulesRequest;
import com.recred.backend.application.UpdateBudgetRulesResponse;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.infrastructure.web.BudgetController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recred.backend.infrastructure.web.TutorAccessGuard;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    @Mock private CreateBudget createBudget;
    @Mock private UpdateBudget updateBudget;
    @Mock private DisableBudget disableBudget;
    @Mock private GetActiveBudgetByStudent getActiveBudgetByStudent;
    @Mock private UpdateBudgetRules updateBudgetRules;
    @Mock private TutorAccessGuard tutorAccessGuard;

    @InjectMocks
    private BudgetController budgetController;

    private MockMvc mockMvc;

    private final UUID BUDGET_ID = UUID.randomUUID();
    private final UUID STUDENT_ID = UUID.randomUUID();
    private final BigDecimal LIMIT_AMOUNT = new BigDecimal("5000.00");
    private final BigDecimal UPDATE_AMOUNT = new BigDecimal("10000.00");
    private final String CREATE_JSON = "{\"limitAmount\": 5000, \"period\": \"SEMANAL\"}";
    private final String UPDATE_JSON = "{\"limitAmount\": 10000}";
    private final String RULES_JSON = """
            {
              "rules": [
                {
                  "categoryId": "123e4567-e89b-12d3-a456-426614174000",
                  "porcentajeLimite": 50.00
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(budgetController).build();
    }

    @Test
    void createShouldReturn201Created() throws Exception {
        givenCreateBudgetReturnsResponse();
        
        ResultActions response = whenCreatingBudget();
        
        thenResponseIsCreated(response);
        thenDelegatesToCreateBudget();
    }

    @Test
    void getActiveByStudentShouldReturn200OkWhenExists() throws Exception {
        givenActiveBudgetExists();
        
        ResultActions response = whenGettingActiveBudget();
        
        thenResponseIsOkWithBudgetId(response);
        thenDelegatesToGetActiveBudget();
    }

    @Test
    void getActiveByStudentShouldReturn404NotFoundWhenNotExists() throws Exception {
        givenActiveBudgetDoesNotExist();
        
        ResultActions response = whenGettingActiveBudget();
        
        thenResponseIsNotFound(response);
        thenDelegatesToGetActiveBudget();
    }

    @Test
    void updateShouldReturn200Ok() throws Exception {
        givenUpdateBudgetReturnsResponse();
        
        ResultActions response = whenUpdatingBudget();
        
        thenUpdateResponseIsOk(response);
        thenDelegatesToUpdateBudget();
    }

    @Test
    void disableShouldReturn204NoContent() throws Exception {
        givenDisableBudgetSucceeds();
        
        ResultActions response = whenDisablingBudget();
        
        thenResponseIsNoContent(response);
        thenDelegatesToDisableBudget();
    }

    @Test
    void updateCategoryRulesShouldReturn200Ok() throws Exception {
        givenUpdateBudgetRulesReturnsResponse();
        
        ResultActions response = whenUpdatingBudgetRules();
        
        thenRulesUpdateResponseIsOk(response);
        thenDelegatesToUpdateBudgetRules();
    }

    private void givenCreateBudgetReturnsResponse() {
        CreateBudgetResponse responseMock = new CreateBudgetResponse(
                BUDGET_ID, STUDENT_ID, UUID.randomUUID(), LIMIT_AMOUNT,
                PeriodoPresupuesto.SEMANAL, LocalDate.now(), true, List.of(), LIMIT_AMOUNT
        );
        when(createBudget.execute(any())).thenReturn(responseMock);
    }

    private void givenActiveBudgetExists() {
        GetActiveBudgetByStudentResponse responseMock = new GetActiveBudgetByStudentResponse(
                BUDGET_ID, STUDENT_ID, UUID.randomUUID(), LIMIT_AMOUNT,
                PeriodoPresupuesto.SEMANAL, LocalDate.now(), true, List.of(), LIMIT_AMOUNT
        );
        when(getActiveBudgetByStudent.execute(any(GetActiveBudgetByStudentRequest.class)))
                .thenReturn(Optional.of(responseMock));
    }

    private void givenActiveBudgetDoesNotExist() {
        when(getActiveBudgetByStudent.execute(any(GetActiveBudgetByStudentRequest.class)))
                .thenReturn(Optional.empty());
    }

    private void givenUpdateBudgetReturnsResponse() {
        UpdateBudgetResponse responseMock = new UpdateBudgetResponse(
                BUDGET_ID, STUDENT_ID, UUID.randomUUID(), UPDATE_AMOUNT,
                PeriodoPresupuesto.SEMANAL, LocalDate.now(), true, List.of(), UPDATE_AMOUNT
        );
        when(updateBudget.execute(any(UpdateBudgetRequest.class))).thenReturn(responseMock);
    }

    private void givenDisableBudgetSucceeds() {
        when(disableBudget.execute(new DisableBudgetRequest(BUDGET_ID)))
                .thenReturn(new com.recred.backend.application.DisableBudgetResponse());
    }

    private void givenUpdateBudgetRulesReturnsResponse() {
        UpdateBudgetRulesResponse responseMock = new UpdateBudgetRulesResponse(
                BUDGET_ID, STUDENT_ID, UUID.randomUUID(), LIMIT_AMOUNT,
                PeriodoPresupuesto.SEMANAL, LocalDate.now(), true, List.of(), LIMIT_AMOUNT
        );
        when(updateBudgetRules.execute(any(UpdateBudgetRulesRequest.class))).thenReturn(responseMock);
    }

    private ResultActions whenCreatingBudget() throws Exception {
        return mockMvc.perform(post("/api/v1/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_JSON));
    }

    private ResultActions whenGettingActiveBudget() throws Exception {
        return mockMvc.perform(get("/api/v1/budgets/student/{studentId}/active", STUDENT_ID)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions whenUpdatingBudget() throws Exception {
        return mockMvc.perform(put("/api/v1/budgets/{budgetId}", BUDGET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(UPDATE_JSON));
    }

    private ResultActions whenDisablingBudget() throws Exception {
        return mockMvc.perform(patch("/api/v1/budgets/{budgetId}/disable", BUDGET_ID)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions whenUpdatingBudgetRules() throws Exception {
        return mockMvc.perform(put("/api/v1/budgets/{budgetId}/rules", BUDGET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(RULES_JSON));
    }

    private void thenResponseIsCreated(ResultActions response) throws Exception {
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(BUDGET_ID.toString()))
                .andExpect(jsonPath("$.activo").value(true));
    }

    private void thenResponseIsOkWithBudgetId(ResultActions response) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BUDGET_ID.toString()));
    }

    private void thenResponseIsNotFound(ResultActions response) throws Exception {
        response.andExpect(status().isNotFound());
    }

    private void thenUpdateResponseIsOk(ResultActions response) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BUDGET_ID.toString()))
                .andExpect(jsonPath("$.montoLimiteGeneral").value(10000.00));
    }

    private void thenResponseIsNoContent(ResultActions response) throws Exception {
        response.andExpect(status().isNoContent());
    }

    private void thenRulesUpdateResponseIsOk(ResultActions response) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BUDGET_ID.toString()));
    }

    private void thenDelegatesToCreateBudget() {
        verify(createBudget).execute(any());
    }

    private void thenDelegatesToGetActiveBudget() {
        verify(getActiveBudgetByStudent).execute(new GetActiveBudgetByStudentRequest(STUDENT_ID));
    }

    private void thenDelegatesToUpdateBudget() {
        verify(updateBudget).execute(any(UpdateBudgetRequest.class));
    }

    private void thenDelegatesToDisableBudget() {
        verify(disableBudget).execute(new DisableBudgetRequest(BUDGET_ID));
    }

    private void thenDelegatesToUpdateBudgetRules() {
        verify(updateBudgetRules).execute(any(UpdateBudgetRulesRequest.class));
    }
}
