package com.recred.backend.web;

import com.recred.backend.application.GetLoggedUserConsumptionDetail;
import com.recred.backend.application.GetStudentRewardStatus;
import com.recred.backend.application.StudentConsumptionCommand;
import com.recred.backend.application.StudentRewardResponse;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.StudentConsumptionDetail;
import com.recred.backend.domain.models.StudentConsumptionProduct;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.recred.backend.infrastructure.web.StudentConsumptionController;
import com.recred.backend.infrastructure.web.StudentConsumptionExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentConsumptionControllerTest {

    private GetLoggedUserConsumptionDetail getLoggedUserConsumptionDetail;
    private GetStudentRewardStatus getStudentRewardStatus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getLoggedUserConsumptionDetail = mock(GetLoggedUserConsumptionDetail.class);
        getStudentRewardStatus = mock(GetStudentRewardStatus.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StudentConsumptionController(
                        getLoggedUserConsumptionDetail, getStudentRewardStatus))
                .setControllerAdvice(new StudentConsumptionExceptionHandler())
                .build();
    }

    @Test
    void getRewardStatus_DebeRetornarEstadoExitosamente() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        StudentRewardResponse response = new StudentRewardResponse(
                50, "BRONZE", "Mensaje", 50, "SILVER"
        );
        when(getStudentRewardStatus.execute(alumnoId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/consumos/alumnos/{alumnoId}/recompensas-saludables", alumnoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(50))
                .andExpect(jsonPath("$.currentLevel").value("BRONZE"))
                .andExpect(jsonPath("$.pointsToNextLevel").value(50))
                .andExpect(jsonPath("$.nextLevelName").value("SILVER"));

        verify(getStudentRewardStatus).execute(alumnoId);
    }

    @Test
    void getConsumptionDelegatesAuthenticatedUserAndFilters() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        StudentConsumptionDetail detail = new StudentConsumptionDetail(
                studentId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20),
                new BigDecimal("1500.00"),
                1,
                List.of(new StudentConsumptionPurchase(
                        purchaseId,
                        LocalDateTime.of(2026, 5, 20, 10, 30),
                        EstadoCompra.ENTREGADO,
                        MedioPago.CREDITOS,
                        new BigDecimal("1500.00"),
                        buffetId,
                        "Buffet Primario",
                        List.of(new StudentConsumptionProduct(
                                productId,
                                "Sandwich",
                                "Comida",
                                1,
                                new BigDecimal("1500.00"),
                                new BigDecimal("1500.00")
                        ))
                ))
        );
        when(getLoggedUserConsumptionDetail.execute(eq("cognito-sub"), org.mockito
                .ArgumentMatchers.any())).thenReturn(detail);

        mockMvc.perform(get("/api/v1/consumos")
                        .principal(authentication())
                        .param("alumnoId", studentId.toString())
                        .param("desde", "2026-05-01")
                        .param("hasta", "2026-05-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoId").value(studentId.toString()))
                .andExpect(jsonPath("$.desde").value("2026-05-01"))
                .andExpect(jsonPath("$.hasta").value("2026-05-20"))
                .andExpect(jsonPath("$.montoTotal").value(1500.00))
                .andExpect(jsonPath("$.cantidadCompras").value(1))
                .andExpect(jsonPath("$.compras[0].compraId").value(purchaseId.toString()))
                .andExpect(jsonPath("$.compras[0].buffet").value("Buffet Primario"))
                .andExpect(jsonPath("$.compras[0].productos[0].productoId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.compras[0].productos[0].nombre").value("Sandwich"));

        ArgumentCaptor<StudentConsumptionCommand> commandCaptor = ArgumentCaptor
                .forClass(StudentConsumptionCommand.class);
        verify(getLoggedUserConsumptionDetail).execute(eq("cognito-sub"), commandCaptor.capture());
        StudentConsumptionCommand command = commandCaptor.getValue();
        assertEquals(Optional.of(studentId), command.studentId());
        assertEquals(Optional.of(LocalDate.of(2026, 5, 1)), command.from());
        assertEquals(Optional.of(LocalDate.of(2026, 5, 20)), command.to());
    }

    @Test
    void getConsumptionRejectsMissingAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/consumos")
                        .param("desde", "2026-05-01")
                        .param("hasta", "2026-05-20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Usuario no autenticado"));

        verifyNoInteractions(getLoggedUserConsumptionDetail);
    }

    private JwtAuthenticationToken authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("cognito-sub")
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
