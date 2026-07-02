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
        request.validate();

        Tutor tutor = tutorRepository.findByUsuarioIdCognito(request.cognitoSubject())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor no encontrado para el usuario autenticado"));

        Set<Alumno> hijos = tutor.getHijos();

        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalDebt = BigDecimal.ZERO;
        List<ChildDashboardResponse> childrenDashboards = new ArrayList<>();

        for (Alumno alumno : hijos) {
            BigDecimal balance = getBalance(alumno);
            BigDecimal debt = getDebt(alumno);
            totalBalance = totalBalance.add(balance);
            totalDebt = totalDebt.add(debt);

            List<GetHistoryByStudentResponse> historial = getHistoryByStudent.execute(
                    new GetHistoryByStudentRequest(alumno.getId())
            );

            childrenDashboards.add(buildChildDashboard(alumno, balance, debt, historial));
        }

        String dashboardConfig = tutor.getDashboardConfig();
        String planStr = tutor.getUsuario() != null 
                ? tutor.getUsuario().getPlan().name() : "GRATUITO";

        return new GetTutorGlobalDashboardResponse("ME", totalBalance, totalDebt,
                dashboardConfig, childrenDashboards, planStr);
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
            // Fallback to ZERO
        }
        return spent;
    }

    private HealthSummaryResponse buildHealthSummary(Alumno alumno) {
        StudentRewardResponse reward = getStudentRewardStatus.execute(alumno.getId());
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
        List<ScheduledPickupResponse> pickups = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();

        for (GetHistoryByStudentResponse p : historial) {
            boolean isToday = p.pickupDate() != null && p.pickupDate().equals(today);
            boolean isActive = "PROGRAMADO".equals(p.withdrawalStatus())
                    || "EN_PREPARACION".equals(p.withdrawalStatus())
                    || "LISTO".equals(p.withdrawalStatus());

            if ("ANTICIPADA".equals(p.tipo()) && isToday && isActive) {
                String desc = p.pickupSlotDescription() != null ? p.pickupSlotDescription() : "";
                pickups.add(new ScheduledPickupResponse(
                        p.id().toString(), getPurchaseName(p), desc));
            }
        }
        return pickups;
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
}
