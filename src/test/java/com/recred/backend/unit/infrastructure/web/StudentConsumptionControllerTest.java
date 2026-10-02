package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetLoggedUserConsumptionDetail;
import com.recred.backend.application.GetLoggedUserConsumptionDetailRequest;
import com.recred.backend.application.GetLoggedUserConsumptionDetailResponse;
import com.recred.backend.application.GetStudentRewardStatus;
import com.recred.backend.application.GetStudentRewardStatusRequest;
import com.recred.backend.application.StudentRewardResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.recred.backend.infrastructure.web.StudentConsumptionController;
import com.recred.backend.infrastructure.web.StudentConsumptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentConsumptionControllerTest {

    @Mock private GetLoggedUserConsumptionDetail getLoggedUserConsumptionDetail;
    @Mock private GetStudentRewardStatus getStudentRewardStatus;
    
    private final UUID studentId = UUID.randomUUID();
    private final UUID purchaseId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID buffetId = UUID.randomUUID();
    
    private StudentConsumptionController controller;

    @BeforeEach
    void setUp() {
        controller = new StudentConsumptionController(
                getLoggedUserConsumptionDetail, getStudentRewardStatus);
    }

    @Test
    void givenValidStudentWhenGettingRewardStatusThenReturnsStatus() {
        StudentRewardResponse response = new StudentRewardResponse(
                50, "BRONZE", "Mensaje", 50, "SILVER"
        );
        givenRewardStatus(studentId, response);

        StudentRewardResponse result = whenGettingRewardStatus(studentId);

        thenRewardStatusIsReturned(result, 50, "BRONZE", 50, "SILVER");
        thenGetRewardStatusWasRequested(studentId);
    }

    @Test
    void givenAuthenticatedUserWhenGettingConsumptionThenReturnsDetail() {
        givenLoggedUserConsumptionDetailReturns(givenConsumptionDetail());

        StudentConsumptionResponse response = 
                whenGettingConsumption("2026-05-01", "2026-05-20", studentId, authentication());

        thenConsumptionIsReturned(response);
        thenConsumptionDetailWasRequested("cognito-sub", studentId, "2026-05-01", "2026-05-20");
    }

    @Test
    void givenMissingAuthenticationWhenGettingConsumptionThenThrowsException() {
        Executable execution = () -> whenGettingConsumption("2026-05-01", "2026-05-20", null, null);

        thenThrowsAuthenticationException(execution);
        thenGetLoggedUserConsumptionWasNotCalled();
    }

    private void givenRewardStatus(UUID alumnoId, StudentRewardResponse response) {
        when(getStudentRewardStatus.execute(new GetStudentRewardStatusRequest(alumnoId)))
                .thenReturn(response);
    }

    private GetLoggedUserConsumptionDetailResponse givenConsumptionDetail() {
        return new GetLoggedUserConsumptionDetailResponse(
                studentId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20),
                new BigDecimal("1500.00"),
                1,
                List.of(new GetLoggedUserConsumptionDetailResponse.PurchaseResponse(
                        purchaseId,
                        LocalDateTime.of(2026, 5, 20, 10, 30),
                        "ENTREGADO",
                        "CREDITOS",
                        new BigDecimal("1500.00"),
                        buffetId,
                        "Buffet Primario",
                        List.of(new GetLoggedUserConsumptionDetailResponse.ProductResponse(
                                productId,
                                "Sandwich",
                                "Comida",
                                1,
                                new BigDecimal("1500.00"),
                                new BigDecimal("1500.00"),
                                List.of()
                        ))
                ))
        );
    }

    private void givenLoggedUserConsumptionDetailReturns(GetLoggedUserConsumptionDetailResponse detail) {
        when(getLoggedUserConsumptionDetail.execute(any())).thenReturn(detail);
    }

    private StudentRewardResponse whenGettingRewardStatus(UUID alumnoId) {
        return controller.getRewardStatus(alumnoId);
    }

    private StudentConsumptionResponse whenGettingConsumption(
            String from, String to, UUID studentId, JwtAuthenticationToken auth) {
        return controller.getConsumption(auth, studentId, LocalDate.parse(from), LocalDate.parse(to));
    }

    private void thenRewardStatusIsReturned(
            StudentRewardResponse result, int totalPoints, String currentLevel, int pointsToNext, String nextLevel) {
        assertEquals(totalPoints, result.totalPoints());
        assertEquals(currentLevel, result.currentLevel());
        assertEquals(pointsToNext, result.pointsToNextLevel());
        assertEquals(nextLevel, result.nextLevelName());
    }

    private void thenGetRewardStatusWasRequested(UUID alumnoId) {
        verify(getStudentRewardStatus).execute(new GetStudentRewardStatusRequest(alumnoId));
    }

    private void thenConsumptionIsReturned(StudentConsumptionResponse response) {
        assertEquals(studentId, response.studentId());
        assertEquals(LocalDate.of(2026, 5, 1), response.from());
        assertEquals(LocalDate.of(2026, 5, 20), response.to());
        assertEquals(new BigDecimal("1500.00"), response.totalAmount());
        assertEquals(1, response.purchaseCount());
        assertEquals(purchaseId, response.purchases().get(0).purchaseId());
        assertEquals("Buffet Primario", response.purchases().get(0).buffetName());
        assertEquals(productId, response.purchases().get(0).products().get(0).productId());
        assertEquals("Sandwich", response.purchases().get(0).products().get(0).productName());
    }

    private void thenConsumptionDetailWasRequested(String sub, UUID studentId, String from, String to) {
        ArgumentCaptor<GetLoggedUserConsumptionDetailRequest> requestCaptor = ArgumentCaptor
                .forClass(GetLoggedUserConsumptionDetailRequest.class);
        verify(getLoggedUserConsumptionDetail).execute(requestCaptor.capture());
        GetLoggedUserConsumptionDetailRequest request = requestCaptor.getValue();
        assertEquals(sub, request.cognitoSubject());
        assertEquals(studentId, request.studentId());
        assertEquals(LocalDate.parse(from), request.from());
        assertEquals(LocalDate.parse(to), request.to());
    }

    private void thenThrowsAuthenticationException(Executable execution) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }

    private void thenGetLoggedUserConsumptionWasNotCalled() {
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
