package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.BudgetSummaryDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.ChildDashboardDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.HealthSummaryDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.ScheduledPickupDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.SmartActionDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.TransactionSummaryDTO;

import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.domain.models.RangoFechas;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public TutorGlobalDashboardDTO executeByCognitoSubject(String cognitoSubject) {
        Set<Alumno> hijos = tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new RuntimeException(
                        "Tutor no encontrado para el usuario autenticado"))
                .getHijos();
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalDebt = BigDecimal.ZERO;
        List<ChildDashboardDTO> childrenDashboards = new ArrayList<>();

        for (Alumno alumno : hijos) {
            BigDecimal balance = alumno.getBilletera() != null 
                    ? alumno.getBilletera().getSaldoCreditos() : BigDecimal.ZERO;
            totalBalance = totalBalance.add(balance);

            BigDecimal debt = alumno.getBilletera() != null 
                    ? alumno.getBilletera().getSaldoDeudaInterno() : BigDecimal.ZERO;
            if (debt == null) {
                debt = BigDecimal.ZERO;
            }
            totalDebt = totalDebt.add(debt);

            List<GetHistoryByStudentResponse> historial = getHistoryByStudent.execute(
                    new GetHistoryByStudentRequest(alumno.getId())
            );

            Optional<GetActiveBudgetByStudentResponse> presupuestoOpt = getActiveBudgetByStudent
                    .execute(new GetActiveBudgetByStudentRequest(alumno.getId()));
            BudgetSummaryDTO budgetDto = null;
            if (presupuestoOpt.isPresent()) {
                GetActiveBudgetByStudentResponse p = presupuestoOpt.get();
                BigDecimal spent = BigDecimal.ZERO;
                try {
                    CalculadorPeriodoPresupuesto calculador = new CalculadorPeriodoPresupuesto();
                    LocalDateTime now = LocalDateTime.now();
                    RangoFechas rango = calculador.calcularRango(p.periodo(), now);
                    for (GetHistoryByStudentResponse purchase : historial) {
                        LocalDateTime purchaseDate = purchase.date();
                        if (purchaseDate != null 
                                && !purchaseDate.isBefore(rango.inicio()) 
                                && !purchaseDate.isAfter(rango.fin())) {
                            spent = spent.add(purchase.totalAmount());
                        }
                    }
                } catch (Exception e) {
                    // Fallback to ZERO in case of any calculation issues
                }
                budgetDto = new BudgetSummaryDTO(
                        p.periodo().name(),
                        spent,
                        p.montoLimiteGeneral()
                );
            }

            StudentRewardResponse reward = getStudentRewardStatus.execute(alumno.getId());
            HealthSummaryDTO healthDto = new HealthSummaryDTO(
                    reward.currentLevel(),
                    reward.totalPoints(),
                    reward.pointsToNextLevel(),
                    reward.levelMessage()
            );

            String predictionMsg = "Datos insuficientes para predecir";
            try {
                var prediction = predictStudentSpending.execute(alumno.getId(), null);
                if (prediction.aiAnalysis().isPresent()) {
                    predictionMsg = prediction.aiAnalysis().get().summary();
                } else if (prediction.spendingPrediction() != null) {
                    predictionMsg = "Gasto estimado al fin de periodo: $" 
                            + prediction.spendingPrediction().predictedSpending();
                }
            } catch (Exception e) {
                
            }
            List<TransactionSummaryDTO> transactions = historial.stream()
                    .limit(4)
                    .map(p -> new TransactionSummaryDTO(
                            p.id().toString(),
                            getPurchaseName(p),
                            p.date(),
                            p.totalAmount(),
                            "🛒"
                    )).toList();

            SmartActionDTO smartAction = null;
            if (!transactions.isEmpty()) {
                smartAction = new SmartActionDTO(
                        "Oportunidad Saludable",
                        "Cambia las compras frecuentes de snacks por alternativas más sanas.",
                        "BLOCK_CATEGORY",
                        "SNACKS"
                );
            }

            String stuName = alumno.getUsuario() != null 
                    ? alumno.getUsuario().getNombre() : "Alumno";
            String urlFotoPerfil = alumno.getUsuario() != null
                    ? alumno.getUsuario().getUrlFotoPerfil() : null;
            
            List<ScheduledPickupDTO> todayPickups = new ArrayList<>();
            java.time.LocalDate today = java.time.LocalDate.now();
            for (GetHistoryByStudentResponse p : historial) {
                var tipo = p.tipo();
                var ws = p.withdrawalStatus();
                boolean isToday = p.pickupDate() != null 
                        && p.pickupDate().equals(today);
                boolean isActive = "PROGRAMADO".equals(ws)
                        || "EN_PREPARACION".equals(ws)
                        || "LISTO".equals(ws);

                if ("ANTICIPADA".equals(tipo) 
                        && isToday && isActive) {
                    String desc = p.pickupSlotDescription() != null 
                            ? p.pickupSlotDescription() : "";
                    todayPickups.add(new ScheduledPickupDTO(
                            p.id().toString(),
                            getPurchaseName(p),
                            desc
                    ));
                }
            }

            childrenDashboards.add(new ChildDashboardDTO(
                    alumno.getId(),
                    stuName,
                    balance,
                    debt,
                    urlFotoPerfil,
                    predictionMsg,
                    budgetDto,
                    healthDto,
                    todayPickups,
                    transactions,
                    smartAction
            ));
        }

        Tutor tutor = tutorRepository.findByUsuarioIdCognito(cognitoSubject).orElse(null);
        String dashboardConfig = tutor != null ? tutor.getDashboardConfig() : null;
        String planStr = tutor != null && tutor.getUsuario() != null
                ? tutor.getUsuario().getPlan().name() : "GRATUITO";

        return new TutorGlobalDashboardDTO("ME", totalBalance, totalDebt, 
                dashboardConfig, childrenDashboards, planStr);
    }

    private String getPurchaseName(GetHistoryByStudentResponse p) {
        if (p.items() == null || p.items().isEmpty()) {
            return "Compra";
        }
        GetHistoryByStudentResponse.Item first = p.items().iterator().next();
        String name = first != null 
                ? first.nombre() : "Producto";
        if (p.items().size() > 1) {
            name += " y " + (p.items().size() - 1) + " más";
        }
        return name;
    }
}
