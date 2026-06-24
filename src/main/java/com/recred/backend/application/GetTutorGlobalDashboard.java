package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.BudgetSummaryDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.ChildDashboardDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.HealthSummaryDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.ScheduledPickupDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.SmartActionDTO;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO.TransactionSummaryDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GetTutorGlobalDashboard {

    private final ObtenerHijosPorPadre obtenerHijosPorPadre;
    private final GetActiveBudgetByStudent getActiveBudgetByStudent;
    private final GetStudentRewardStatus getStudentRewardStatus;
    private final PredictStudentSpending predictStudentSpending;
    private final ObtenerHistorialAlumno obtenerHistorialAlumno;
    private final TutorRepository tutorRepository;

    public GetTutorGlobalDashboard(ObtenerHijosPorPadre obtenerHijosPorPadre,
                                   GetActiveBudgetByStudent getActiveBudgetByStudent,
                                   GetStudentRewardStatus getStudentRewardStatus,
                                   PredictStudentSpending predictStudentSpending,
                                   ObtenerHistorialAlumno obtenerHistorialAlumno,
                                   TutorRepository tutorRepository) {
        this.obtenerHijosPorPadre = obtenerHijosPorPadre;
        this.getActiveBudgetByStudent = getActiveBudgetByStudent;
        this.getStudentRewardStatus = getStudentRewardStatus;
        this.predictStudentSpending = predictStudentSpending;
        this.obtenerHistorialAlumno = obtenerHistorialAlumno;
        this.tutorRepository = tutorRepository;
    }

    public TutorGlobalDashboardDTO executeByCognitoSubject(String cognitoSubject) {
        Set<Alumno> hijos = obtenerHijosPorPadre.executeByCognitoSubject(cognitoSubject);
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

            Optional<PresupuestoAlumno> presupuestoOpt = getActiveBudgetByStudent
                    .execute(alumno.getId());
            BudgetSummaryDTO budgetDto = null;
            if (presupuestoOpt.isPresent()) {
                PresupuestoAlumno p = presupuestoOpt.get();
                budgetDto = new BudgetSummaryDTO(
                        p.getPeriodo().name(),
                        BigDecimal.ZERO,
                        p.getMontoLimiteGeneral()
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

            List<Purchase> historial = obtenerHistorialAlumno.execute(alumno.getId());
            List<TransactionSummaryDTO> transactions = historial.stream()
                    .limit(4)
                    .map(p -> new TransactionSummaryDTO(
                            p.getId().toString(),
                            getPurchaseName(p),
                            p.getDate(),
                            p.getTotalAmount(),
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
            
            List<ScheduledPickupDTO> todayPickups = new ArrayList<>();
            java.time.LocalDate today = java.time.LocalDate.now();
            for (Purchase p : historial) {
                var tipo = p.getTipo();
                var ws = p.getWithdrawalStatus();
                boolean isToday = p.getPickupDate() != null 
                        && p.getPickupDate().equals(today);
                boolean isActive = ws == com.recred.backend.domain.models.EstadoRetiro.PROGRAMADO
                        || ws == com.recred.backend.domain.models.EstadoRetiro.EN_PREPARACION
                        || ws == com.recred.backend.domain.models.EstadoRetiro.LISTO;

                if (tipo == com.recred.backend.domain.models.TipoCompra.ANTICIPADA 
                        && isToday && isActive) {
                    String desc = p.getPickupSlotDescription() != null 
                            ? p.getPickupSlotDescription() : "";
                    todayPickups.add(new ScheduledPickupDTO(
                            p.getId().toString(),
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

    private String getPurchaseName(Purchase p) {
        if (p.getItems().isEmpty()) {
            return "Compra";
        }
        com.recred.backend.domain.models.Producto first = p.getItems().keySet().iterator().next();
        String name = first != null 
                ? first.getNombre() : "Producto";
        if (p.getItems().size() > 1) {
            name += " y " + (p.getItems().size() - 1) + " más";
        }
        return name;
    }
}
