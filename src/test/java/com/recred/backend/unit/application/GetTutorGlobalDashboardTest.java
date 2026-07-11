package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTutorGlobalDashboardTest {

    @Mock
    private GetActiveBudgetByStudent getActiveBudgetByStudent;

    @Mock
    private GetStudentRewardStatus getStudentRewardStatus;

    @Mock
    private PredictStudentSpending predictStudentSpending;

    @Mock
    private GetHistoryByStudent getHistoryByStudent;

    @Mock
    private TutorRepository tutorRepository;

    private GetTutorGlobalDashboard getTutorGlobalDashboard;

    @BeforeEach
    void setUp() {
        getTutorGlobalDashboard = new GetTutorGlobalDashboard(
                getActiveBudgetByStudent,
                getStudentRewardStatus,
                predictStudentSpending,
                getHistoryByStudent,
                tutorRepository
        );
    }

    @Test
    void executeByCognitoSubjectShouldBuildDashboardForChildren() {
        String cognitoSubject = "parent-subject";
        Alumno firstChild = child("Mateo", "100.00");
        Alumno secondChild = childWithoutUser();
        Alumno thirdChild = child("Ana", "50.00");
        Producto alfajor = product("Alfajor", "40.00");
        Producto jugo = product("Jugo", "30.00");
        Purchase multipleProductsPurchase = purchase(
                items(alfajor, jugo),
                "70.00"
        );
        Purchase emptyPurchase = purchase(Map.of(), "0.00");

        givenTutorWithChildren(cognitoSubject, firstChild, secondChild, thirdChild);
        givenBudgetFor(firstChild);
        givenNoBudgetFor(secondChild);
        givenNoBudgetFor(thirdChild);
        givenRewardsFor(firstChild, secondChild, thirdChild);
        givenPredictionWithAnalysis(firstChild);
        givenPredictionFailure(secondChild);
        givenPredictionWithoutAnalysis(thirdChild);
        givenHistory(firstChild, multipleProductsPurchase);
        givenEmptyHistory(secondChild);
        givenHistory(thirdChild, emptyPurchase);

        GetTutorGlobalDashboardResponse result = whenDashboardIsRequested(cognitoSubject);

        thenDashboardSummaryMatches(result);
        assertFirstChild(result.childrenDashboards().get(0));
        assertSecondChild(result.childrenDashboards().get(1));
        assertThirdChild(result.childrenDashboards().get(2));
    }

    private void givenTutorWithChildren(
            String cognitoSubject,
            Alumno firstChild,
            Alumno secondChild,
            Alumno thirdChild
    ) {
        Set<Alumno> children = new LinkedHashSet<>(List.of(
                firstChild,
                secondChild,
                thirdChild
        ));
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setHijos(children);
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));
    }

    private void givenBudgetFor(Alumno child) {
        when(getActiveBudgetByStudent.execute(new GetActiveBudgetByStudentRequest(child.getId())))
                .thenReturn(Optional.of(budgetResponse(child.getId())));
    }

    private void givenNoBudgetFor(Alumno child) {
        when(getActiveBudgetByStudent.execute(new GetActiveBudgetByStudentRequest(child.getId())))
                .thenReturn(Optional.empty());
    }

    private void givenRewardsFor(
            Alumno firstChild,
            Alumno secondChild,
            Alumno thirdChild
    ) {
        when(getStudentRewardStatus.execute(new GetStudentRewardStatusRequest(
                firstChild.getId()))).thenReturn(reward(
                "SILVER",
                120
        ));
        when(getStudentRewardStatus.execute(new GetStudentRewardStatusRequest(
                secondChild.getId()))).thenReturn(reward(
                "BRONZE",
                20
        ));
        when(getStudentRewardStatus.execute(new GetStudentRewardStatusRequest(
                thirdChild.getId()))).thenReturn(reward(
                "GOLD",
                300
        ));
    }

    private void givenPredictionWithAnalysis(Alumno child) {
        when(predictStudentSpending.execute(predictionRequest(child.getId())))
                .thenReturn(predictionWithAnalysis(child.getId()));
    }

    private void givenPredictionFailure(Alumno child) {
        when(predictStudentSpending.execute(predictionRequest(child.getId())))
                .thenThrow(new RuntimeException("sin datos"));
    }

    private void givenPredictionWithoutAnalysis(Alumno child) {
        when(predictStudentSpending.execute(predictionRequest(child.getId())))
                .thenReturn(predictionWithoutAnalysis(child.getId()));
    }

    private void givenHistory(Alumno child, Purchase purchase) {
        when(getHistoryByStudent.execute(new GetHistoryByStudentRequest(child.getId())))
                .thenReturn(List.of(toResponse(purchase, child.getId())));
    }

    private void givenEmptyHistory(Alumno child) {
        when(getHistoryByStudent.execute(new GetHistoryByStudentRequest(child.getId())))
                .thenReturn(List.of());
    }

    private GetTutorGlobalDashboardResponse whenDashboardIsRequested(String cognitoSubject) {
        return getTutorGlobalDashboard.execute(new GetTutorGlobalDashboardRequest(cognitoSubject));
    }

    private void thenDashboardSummaryMatches(GetTutorGlobalDashboardResponse result) {
        assertEquals("ME", result.tutorId());
        assertEquals(new BigDecimal("150.00"), result.totalBalance());
        assertEquals(3, result.childrenDashboards().size());
    }

    private void assertFirstChild(GetTutorGlobalDashboardResponse.ChildDashboardResponse child) {
        assertEquals("Mateo", child.nombre());
        assertEquals(new BigDecimal("100.00"), child.saldoCreditos());
        assertEquals("Puede gastar menos esta semana.", child.predictionMessage());
        assertNotNull(child.budget());
        assertEquals(PeriodoPresupuesto.MENSUAL.name(), child.budget().periodName());
        assertEquals(BigDecimal.ZERO, child.budget().spent());
        assertEquals(new BigDecimal("500.00"), child.budget().limit());
        assertEquals("SILVER", child.health().rewardLevel());
        assertEquals(120, child.health().rewardPoints());
        assertEquals(30, child.health().pointsToNextLevel());
        assertEquals("Buen progreso", child.health().weeklyHealthSummary());
        assertEquals(1, child.recentTransactions().size());
        assertEquals("Alfajor y 1 m\u00e1s", child.recentTransactions().get(0).title());
        assertNotNull(child.smartAction());
        assertEquals("BLOCK_CATEGORY", child.smartAction().actionType());
    }

    private void assertSecondChild(GetTutorGlobalDashboardResponse.ChildDashboardResponse child) {
        assertEquals("Alumno", child.nombre());
        assertEquals(BigDecimal.ZERO, child.saldoCreditos());
        assertEquals("Datos insuficientes para predecir", child.predictionMessage());
        assertNull(child.budget());
        assertEquals(0, child.recentTransactions().size());
        assertNull(child.smartAction());
    }

    private void assertThirdChild(GetTutorGlobalDashboardResponse.ChildDashboardResponse child) {
        assertEquals("Ana", child.nombre());
        assertEquals(new BigDecimal("50.00"), child.saldoCreditos());
        assertEquals(
                "Gasto estimado al fin de periodo: $123.45",
                child.predictionMessage()
        );
        assertEquals(1, child.recentTransactions().size());
        assertEquals("Compra", child.recentTransactions().get(0).title());
        assertNotNull(child.smartAction());
    }

    private Alumno child(String name, String balance) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNombre(name);
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(usuario);
        Billetera billetera = new Billetera();
        billetera.setSaldoCreditos(new BigDecimal(balance));
        alumno.setBilletera(billetera);
        return alumno;
    }

    private Alumno childWithoutUser() {
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        return alumno;
    }

    private PresupuestoAlumno budget(UUID alumnoId) {
        return new PresupuestoAlumno(
                UUID.randomUUID(),
                alumnoId,
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 6, 1),
                true,
                List.of()
        );
    }

    private GetActiveBudgetByStudentResponse budgetResponse(UUID studentId) {
        return new GetActiveBudgetByStudentResponse(
                UUID.randomUUID(),
                studentId,
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 6, 1),
                true,
                List.of(),
                new BigDecimal("500.00")
        );
    }

    private StudentRewardResponse reward(String level, int points) {
        return new StudentRewardResponse(points, level, "Buen progreso", 30, "GOLD");
    }

    private PredictStudentSpendingRequest predictionRequest(UUID studentId) {
        return new PredictStudentSpendingRequest(studentId, null);
    }

    private PredictStudentSpendingResponse predictionWithAnalysis(UUID studentId) {
        return prediction(
                studentId,
                new BigDecimal("80.00"),
                new PredictStudentSpendingResponse.SpendingPredictionAnalysisResponse(
                        "Puede gastar menos esta semana.",
                        List.of(),
                        List.of(),
                        "test-model"
                )
        );
    }

    private PredictStudentSpendingResponse predictionWithoutAnalysis(UUID studentId) {
        return prediction(studentId, new BigDecimal("123.45"), null);
    }

    private PredictStudentSpendingResponse prediction(
            UUID studentId,
            BigDecimal predictedSpending,
            PredictStudentSpendingResponse.SpendingPredictionAnalysisResponse analysis
    ) {
        return new PredictStudentSpendingResponse(
                studentId,
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                BigDecimal.ZERO,
                predictedSpending,
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                BigDecimal.ONE,
                30,
                20,
                List.of(),
                analysis
        );
    }

    private Producto product(String name, String price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(new BigDecimal(price));
        return product;
    }

    private Map<Producto, Integer> items(Producto first, Producto second) {
        Map<Producto, Integer> items = new LinkedHashMap<>();
        items.put(first, 1);
        items.put(second, 1);
        return items;
    }

    private Purchase purchase(Map<Producto, Integer> items, String amount) {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setDate(LocalDateTime.of(2020, 6, 10, 10, 0));
        purchase.setTotalAmount(new BigDecimal(amount));
        purchase.setItems(items);
        return purchase;
    }

    private GetHistoryByStudentResponse toResponse(Purchase p, UUID studentId) {
        List<GetHistoryByStudentResponse.Item> items = p.getItems().entrySet().stream()
                .map(entry -> new GetHistoryByStudentResponse.Item(
                        entry.getKey().getId(),
                        entry.getKey().getNombre(),
                        entry.getValue(),
                        entry.getKey().getPrecio(),
                        entry.getKey().getRequierePreparacion(),
                        entry.getKey().getCupoMaximoDiario(),
                        entry.getKey().getPromocionId(),
                        entry.getKey().getPromocionNombre()
                ))
                .toList();

        return new GetHistoryByStudentResponse(
                p.getId(),
                studentId,
                p.getTotalAmount(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getStatus() != null ? p.getStatus().getLabel() : null,
                p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null,
                p.getDate(),
                items,
                p.getTipo() != null ? p.getTipo().name() : null,
                p.getWithdrawalCode(),
                p.getPickupSlotDescription(),
                p.getPickupDate(),
                p.getPickupSlotStartTime(),
                p.getWithdrawalStatus() != null ? p.getWithdrawalStatus().name() : null,
                null
        );
    }
}
