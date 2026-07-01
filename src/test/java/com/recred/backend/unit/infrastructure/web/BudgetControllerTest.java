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
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.infrastructure.web.BudgetController;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateBudget createBudget;

    @Mock
    private UpdateBudget updateBudget;

    @Mock
    private DisableBudget disableBudget;

    @Mock
    private GetActiveBudgetByStudent getActiveBudgetByStudent;

    @Mock
    private UpdateBudgetRules updateBudgetRules;

    @InjectMocks
    private BudgetController budgetController;

    private UUID budgetId;
    private UUID studentId;
    private PresupuestoAlumno presupuestoMock;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders.standaloneSetup(budgetController).build();

        budgetId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        presupuestoMock = new PresupuestoAlumno();
        presupuestoMock.setId(budgetId);
        presupuestoMock.setActivo(true);
    }

    @Test
    void create_Retorna201Created() throws Exception {
        CreateBudgetResponse responseMock = new CreateBudgetResponse(
                budgetId,
                studentId,
                UUID.randomUUID(),
                new BigDecimal("5000.00"),
                PeriodoPresupuesto.SEMANAL,
                LocalDate.now(),
                true,
                List.of(),
                new BigDecimal("5000.00")
        );
        when(createBudget.execute(any())).thenReturn(responseMock);

        String jsonBody = "{\"limitAmount\": 5000, \"period\": \"SEMANAL\"}";

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(budgetId.toString()))
                .andExpect(jsonPath("$.activo").value(true));

        verify(createBudget).execute(any());
    }

    @Test
    void getActiveByStudent_SiExiste_Retorna200Ok() throws Exception {
        GetActiveBudgetByStudentResponse responseMock = new GetActiveBudgetByStudentResponse(
                budgetId,
                studentId,
                UUID.randomUUID(),
                new BigDecimal("5000.00"),
                PeriodoPresupuesto.SEMANAL,
                LocalDate.now(),
                true,
                List.of(),
                new BigDecimal("5000.00")
        );
        when(getActiveBudgetByStudent.execute(new GetActiveBudgetByStudentRequest(studentId))).thenReturn(Optional.of(responseMock));

        mockMvc.perform(get("/api/v1/budgets/student/{studentId}/active", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId.toString()));

        verify(getActiveBudgetByStudent).execute(new GetActiveBudgetByStudentRequest(studentId));
    }

    @Test
    void getActiveByStudent_SiNoExiste_Retorna404NotFound() throws Exception {
        when(getActiveBudgetByStudent.execute(new GetActiveBudgetByStudentRequest(studentId))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/budgets/student/{studentId}/active", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(getActiveBudgetByStudent).execute(new GetActiveBudgetByStudentRequest(studentId));
    }

    @Test
    void update_Retorna200Ok() throws Exception {
        UpdateBudgetResponse responseMock = new UpdateBudgetResponse(
                budgetId,
                studentId,
                UUID.randomUUID(),
                new BigDecimal("10000.00"),
                PeriodoPresupuesto.SEMANAL,
                LocalDate.now(),
                true,
                List.of(),
                new BigDecimal("10000.00")
        );
        when(updateBudget.execute(any(UpdateBudgetRequest.class))).thenReturn(responseMock);

        String jsonBody = "{\"limitAmount\": 10000}";

        mockMvc.perform(put("/api/v1/budgets/{budgetId}", budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId.toString()))
                .andExpect(jsonPath("$.montoLimiteGeneral").value(10000.00));

        verify(updateBudget).execute(any(UpdateBudgetRequest.class));
    }

    @Test
    void disable_Retorna204NoContent() throws Exception {

        doNothing().when(disableBudget).execute(new DisableBudgetRequest(budgetId));

        mockMvc.perform(patch("/api/v1/budgets/{budgetId}/disable", budgetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(disableBudget).execute(new DisableBudgetRequest(budgetId));
    }

    @Test
    void updateCategoryRules_Retorna200Ok() throws Exception {
        UpdateBudgetRulesResponse responseMock = new UpdateBudgetRulesResponse(
                budgetId,
                studentId,
                UUID.randomUUID(),
                new BigDecimal("5000.00"),
                PeriodoPresupuesto.SEMANAL,
                LocalDate.now(),
                true,
                List.of(),
                new BigDecimal("5000.00")
        );
        when(updateBudgetRules.execute(any(UpdateBudgetRulesRequest.class))).thenReturn(responseMock);

        String jsonBody = """
                {
                  "rules": [
                    {
                      "categoryId": "123e4567-e89b-12d3-a456-426614174000",
                      "porcentajeLimite": 50.00
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/v1/budgets/{budgetId}/rules", budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId.toString()));

        verify(updateBudgetRules).execute(any(UpdateBudgetRulesRequest.class));
    }
}

