package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.application.GetTutorGlobalDashboardResponse.BudgetSummaryResponse;
import com.recred.backend.application.GetTutorGlobalDashboardResponse.ChildDashboardResponse;
import com.recred.backend.application.GetTutorGlobalDashboardResponse.HealthSummaryResponse;
import com.recred.backend.application.GetTutorGlobalDashboardResponse.ScheduledPickupResponse;
import com.recred.backend.application.GetTutorGlobalDashboardResponse.SmartActionResponse;
import com.recred.backend.application.GetTutorGlobalDashboardResponse.TransactionSummaryResponse;

import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.domain.models.RangoFechas;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GetTutorGlobalDashboard {

    private final GetActiveBudgetByStudent getActiveBudgetByStudent;
    private final GetStudentRewardStatus getStudentRewardStatus;
    private final PredictStudentSpending predictStudentSpending;
    private final GetHistoryByStudent getHistoryByStudent;
    private final TutorRepository tutorRepository;

    public GetTutorGlobalDashboard(GetActiveBudgetByStudent getActiveBudgetByStudent,
                                   GetStudentRewardStatus getStudentRewardStatus,
                                   PredictStudentSpending predictStudentSpending,
                                   GetHistoryByStudent getHistoryByStudent,
                                   TutorRepository tutorRepository) {
        this.getActiveBudgetByStudent = getActiveBudgetByStudent;
        this.getStudentRewardStatus = getStudentRewardStatus;
        this.predictStudentSpending = predictStudentSpending;
        this.getHistoryByStudent = getHistoryByStudent;
        this.tutorRepository = tutorRepository;
    }

    public GetTutorGlobalDashboardResponse execute(GetTutorGlobalDashboardRequest request) {
        Tutor tutor = findTutor(request.cognitoSubject());
        DashboardTotals dashboardTotals = dashboardTotals(tutor.getHijos());
        return response(tutor, dashboardTotals);
    }

    private Tutor findTutor(String cognitoSubject) {
        return tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor no encontrado para el usuario autenticado"));
    }

    private DashboardTotals dashboardTotals(Set<Alumno> hijos) {
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalDebt = BigDecimal.ZERO;
        List<ChildDashboardResponse> dashboards = new ArrayList<>();

        for (Alumno alumno : hijos) {
            ChildAmounts amounts = childAmounts(alumno);
            totalBalance = totalBalance.add(amounts.balance());
            totalDebt = totalDebt.add(amounts.debt());
            dashboards.add(childDashboard(alumno, amounts));
        }

        return new DashboardTotals(totalBalance, totalDebt, dashboards);
    }

    private ChildAmounts childAmounts(Alumno alumno) {
        return new ChildAmounts(getBalance(alumno), getDebt(alumno));
    }

    private ChildDashboardResponse childDashboard(Alumno alumno, ChildAmounts amounts) {
        List<GetHistoryByStudentResponse> historial = getHistoryByStudent.execute(
                new GetHistoryByStudentRequest(alumno.getId())
        );
        return buildChildDashboard(alumno, amounts.balance(), amounts.debt(), historial);
    }

    private GetTutorGlobalDashboardResponse response(
            Tutor tutor,
            DashboardTotals dashboardTotals
    ) {
        String planStr = tutor.getUsuario() != null
                ? tutor.getUsuario().getPlan().name() : "GRATUITO";
        return new GetTutorGlobalDashboardResponse(
                "ME",
                dashboardTotals.totalBalance(),
                dashboardTotals.totalDebt(),
                tutor.getDashboardConfig(),
                dashboardTotals.childrenDashboards(),
                planStr);
    }

    private BigDecimal getBalance(Alumno alumno) {
        return alumno.getBilletera() != null 
                ? alumno.getBilletera().getSaldoCreditos() : BigDecimal.ZERO;
    }

    private BigDecimal getDebt(Alumno alumno) {
        BigDecimal debt = alumno.getBilletera() != null 
                ? alumno.getBilletera().getSaldoDeudaInterno() : BigDecimal.ZERO;
        return debt == null ? BigDecimal.ZERO : debt;
    }

    private ChildDashboardResponse buildChildDashboard(Alumno alumno, BigDecimal balance, 
            BigDecimal debt, List<GetHistoryByStudentResponse> historial) {
        
        BudgetSummaryResponse budgetDto = buildBudgetSummary(alumno, historial);
        HealthSummaryResponse healthDto = buildHealthSummary(alumno);
        String predictionMsg = buildPredictionMessage(alumno);
        List<TransactionSummaryResponse> transactions = buildRecentTransactions(historial);
        SmartActionResponse smartAction = buildSmartAction(transactions);
        List<ScheduledPickupResponse> todayPickups = buildTodayPickups(historial);

        String stuName = alumno.getUsuario() != null 
                ? alumno.getUsuario().getNombre() : "Alumno";
        String urlFotoPerfil = alumno.getUsuario() != null 
                ? alumno.getUsuario().getUrlFotoPerfil() : null;

        return new ChildDashboardResponse(
                alumno.getId(), stuName, balance, debt, urlFotoPerfil,
                predictionMsg, budgetDto, healthDto, todayPickups, transactions, smartAction
        );
    }

    private BudgetSummaryResponse buildBudgetSummary(Alumno alumno, 
            List<GetHistoryByStudentResponse> historial) {
        Optional<GetActiveBudgetByStudentResponse> presupuestoOpt = getActiveBudgetByStudent
                .execute(new GetActiveBudgetByStudentRequest(alumno.getId()));

        if (presupuestoOpt.isEmpty()) {
            return null;
        }

        GetActiveBudgetByStudentResponse p = presupuestoOpt.get();
        BigDecimal spent = calculateSpent(p, historial);

        return new BudgetSummaryResponse(p.periodo().name(), spent, p.montoLimiteGeneral());
    }

    private BigDecimal calculateSpent(GetActiveBudgetByStudentResponse p, 
            List<GetHistoryByStudentResponse> historial) {
        BigDecimal spent = BigDecimal.ZERO;
        try {
            CalculadorPeriodoPresupuesto calculador = new CalculadorPeriodoPresupuesto();
            LocalDateTime now = LocalDateTime.now();
            RangoFechas rango = calculador.calcularRango(p.periodo(), now);
            for (GetHistoryByStudentResponse purchase : historial) {
                LocalDateTime purchaseDate = purchase.date();
                if (purchaseDate != null && !purchaseDate.isBefore(rango.inicio()) 
                        && !purchaseDate.isAfter(rango.fin())) {
                    spent = spent.add(purchase.totalAmount());
                }
            }
        } catch (Exception e) {
        }
        return spent;
    }

    private HealthSummaryResponse buildHealthSummary(Alumno alumno) {
        StudentRewardResponse reward = getStudentRewardStatus.execute(
                new GetStudentRewardStatusRequest(alumno.getId())
        );
        return new HealthSummaryResponse(
                reward.currentLevel(),
                reward.totalPoints(),
                reward.pointsToNextLevel(),
                reward.levelMessage()
        );
    }

    private String buildPredictionMessage(Alumno alumno) {
        try {
            var predictionRequest = new PredictStudentSpendingRequest(alumno.getId(), null);
            var prediction = predictStudentSpending.execute(predictionRequest);
            if (prediction.aiAnalysis() != null) {
                return prediction.aiAnalysis().summary();
            } else {
                return "Gasto estimado al fin de periodo: $" + prediction.predictedSpending();
            }
        } catch (Exception e) {
            return "Datos insuficientes para predecir";
        }
    }

    private List<TransactionSummaryResponse> buildRecentTransactions(
            List<GetHistoryByStudentResponse> historial) {
        return historial.stream()
                .limit(4)
                .map(p -> new TransactionSummaryResponse(
                        p.id().toString(),
                        getPurchaseName(p),
                        p.date(),
                        p.totalAmount(),
                        "🛒"
                )).collect(Collectors.toList());
    }

    private SmartActionResponse buildSmartAction(List<TransactionSummaryResponse> transactions) {
        if (!transactions.isEmpty()) {
            return new SmartActionResponse(
                    "Oportunidad Saludable",
                    "Cambia las compras frecuentes de snacks por alternativas más sanas.",
                    "BLOCK_CATEGORY",
                    "SNACKS"
            );
        }
        return null;
    }

    private List<ScheduledPickupResponse> buildTodayPickups(
            List<GetHistoryByStudentResponse> historial) {
        java.time.LocalDate today = java.time.LocalDate.now();

        return historial.stream()
                .filter(p -> p.pickupDate() != null && p.pickupDate().equals(today))
                .filter(p -> "ANTICIPADA".equals(p.tipo()))
                .filter(p -> "PROGRAMADO".equals(p.withdrawalStatus())
                        || "EN_PREPARACION".equals(p.withdrawalStatus())
                        || "LISTO".equals(p.withdrawalStatus()))
                .sorted(java.util.Comparator.comparing(p ->
                        p.pickupSlotStartTime() == null 
                        ? java.time.LocalTime.MAX 
                        : p.pickupSlotStartTime()))
                .map(p -> {
                    String desc = p.pickupSlotDescription() != null 
                            ? p.pickupSlotDescription() : "";
                    return new ScheduledPickupResponse(
                            p.id().toString(), getPurchaseName(p), desc);
                })
                .toList();
    }

    private String getPurchaseName(GetHistoryByStudentResponse p) {
        if (p.items() == null || p.items().isEmpty()) {
            return "Compra";
        }
        GetHistoryByStudentResponse.Item first = p.items().iterator().next();
        String name = first != null ? first.nombre() : "Producto";
        if (p.items().size() > 1) {
            name += " y " + (p.items().size() - 1) + " más";
        }
        return name;
    }

    private record DashboardTotals(
            BigDecimal totalBalance,
            BigDecimal totalDebt,
            List<ChildDashboardResponse> childrenDashboards
    ) {
    }

    private record ChildAmounts(BigDecimal balance, BigDecimal debt) {
    }
}
