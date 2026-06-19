package com.recred.backend.unit.application;

import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetChildrenByParent;
import com.recred.backend.application.GetStudentHistory;
import com.recred.backend.application.GetStudentRewardStatus;
import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.StudentRewardResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.models.Usuario;
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
    private GetChildrenByParent getChildrenByParent;

    @Mock
    private GetActiveBudgetByStudent getActiveBudgetByStudent;

    @Mock
    private GetStudentRewardStatus getStudentRewardStatus;

    @Mock
    private PredictStudentSpending predictStudentSpending;

    @Mock
    private GetStudentHistory getStudentHistory;

    private GetTutorGlobalDashboard getTutorGlobalDashboard;

    @BeforeEach
    void setUp() {
        getTutorGlobalDashboard = new GetTutorGlobalDashboard(
                getChildrenByParent,
                getActiveBudgetByStudent,
                getStudentRewardStatus,
                predictStudentSpending,
                getStudentHistory
        );
    }

    @Test
    void executeByCognitoSubjectShouldBuildDashboardForChildren() {
        String cognitoSubject = "parent-subject";
        Alumno firstChild = child("Mateo", "100.00");
        Alumno secondChild = childWithoutUser();
        Alumno thirdChild = child("Ana", "50.00");
        Set<Alumno> children = new LinkedHashSet<>(List.of(firstChild, secondChild, thirdChild));
        Producto alfajor = product("Alfajor", "40.00");
        Producto jugo = product("Jugo", "30.00");
        Purchase multipleProductsPurchase = purchase(
                items(alfajor, jugo),
                "70.00"
        );
        Purchase emptyPurchase = purchase(Map.of(), "0.00");

        when(getChildrenByParent.executeByCognitoSubject(cognitoSubject)).thenReturn(children);
        when(getActiveBudgetByStudent.execute(firstChild.getId())).thenReturn(Optional.of(
                budget(firstChild.getId())
        ));
        when(getActiveBudgetByStudent.execute(secondChild.getId())).thenReturn(Optional.empty());
        when(getActiveBudgetByStudent.execute(thirdChild.getId())).thenReturn(Optional.empty());
        when(getStudentRewardStatus.execute(firstChild.getId())).thenReturn(reward("SILVER", 120));
        when(getStudentRewardStatus.execute(secondChild.getId())).thenReturn(reward("BRONZE", 20));
        when(getStudentRewardStatus.execute(thirdChild.getId())).thenReturn(reward("GOLD", 300));
        when(predictStudentSpending.execute(firstChild.getId(), null))
                .thenReturn(predictionWithAnalysis(firstChild.getId()));
        when(predictStudentSpending.execute(secondChild.getId(), null))
                .thenThrow(new RuntimeException("sin datos"));
        when(predictStudentSpending.execute(thirdChild.getId(), null))
                .thenReturn(predictionWithoutAnalysis(thirdChild.getId()));
        when(getStudentHistory.execute(firstChild.getId()))
                .thenReturn(List.of(multipleProductsPurchase));
        when(getStudentHistory.execute(secondChild.getId())).thenReturn(List.of());
        when(getStudentHistory.execute(thirdChild.getId())).thenReturn(List.of(emptyPurchase));

        TutorGlobalDashboardDTO result = getTutorGlobalDashboard
                .executeByCognitoSubject(cognitoSubject);

        assertEquals("ME", result.tutorId());
        assertEquals(new BigDecimal("150.00"), result.totalBalance());
        assertEquals(3, result.children().size());
        assertFirstChild(result.children().get(0));
        assertSecondChild(result.children().get(1));
        assertThirdChild(result.children().get(2));
    }

    private void assertFirstChild(TutorGlobalDashboardDTO.ChildDashboardDTO child) {
        assertEquals("Mateo", child.studentName());
        assertEquals(new BigDecimal("100.00"), child.balance());
        assertEquals("Puede gastar menos esta semana.", child.spendingPredictionMessage());
        assertNotNull(child.budget());
        assertEquals(PeriodoPresupuesto.MENSUAL.name(), child.budget().period());
        assertEquals(BigDecimal.ZERO, child.budget().spent());
        assertEquals(new BigDecimal("500.00"), child.budget().limit());
        assertEquals("SILVER", child.health().rewardLevel());
        assertEquals(120, child.health().rewardPoints());
        assertEquals(30, child.health().pointsToNextLevel());
        assertEquals("Buen progreso", child.health().weeklyHealthSummary());
        assertEquals(1, child.recentTransactions().size());
        assertEquals("Alfajor y 1 m\u00e1s", child.recentTransactions().get(0).name());
        assertNotNull(child.smartAction());
        assertEquals("BLOCK_CATEGORY", child.smartAction().actionType());
    }

    private void assertSecondChild(TutorGlobalDashboardDTO.ChildDashboardDTO child) {
        assertEquals("Alumno", child.studentName());
        assertEquals(BigDecimal.ZERO, child.balance());
        assertEquals("Datos insuficientes para predecir", child.spendingPredictionMessage());
        assertNull(child.budget());
        assertEquals(0, child.recentTransactions().size());
        assertNull(child.smartAction());
    }

    private void assertThirdChild(TutorGlobalDashboardDTO.ChildDashboardDTO child) {
        assertEquals("Ana", child.studentName());
        assertEquals(new BigDecimal("50.00"), child.balance());
        assertEquals(
                "Gasto estimado al fin de periodo: $123.45",
                child.spendingPredictionMessage()
        );
        assertEquals(1, child.recentTransactions().size());
        assertEquals("Compra", child.recentTransactions().get(0).name());
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

    private StudentRewardResponse reward(String level, int points) {
        return new StudentRewardResponse(points, level, "Buen progreso", 30, "GOLD");
    }

    private SpendingPredictionResult predictionWithAnalysis(UUID studentId) {
        return new SpendingPredictionResult(
                prediction(studentId, new BigDecimal("80.00")),
                Optional.of(new SpendingPredictionAnalysis(
                        "Puede gastar menos esta semana.",
                        List.of(),
                        List.of(),
                        "test-model"
                ))
        );
    }

    private SpendingPredictionResult predictionWithoutAnalysis(UUID studentId) {
        return new SpendingPredictionResult(
                prediction(studentId, new BigDecimal("123.45")),
                Optional.empty()
        );
    }

    private SpendingPrediction prediction(UUID studentId, BigDecimal predictedSpending) {
        return new SpendingPrediction(
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
                List.of()
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
        purchase.setDate(LocalDateTime.of(2026, 6, 10, 10, 0));
        purchase.setTotalAmount(new BigDecimal(amount));
        purchase.setItems(items);
        return purchase;
    }
}
