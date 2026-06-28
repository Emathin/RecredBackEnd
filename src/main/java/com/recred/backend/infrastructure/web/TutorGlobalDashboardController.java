package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.UpdateTutorDashboardConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/tutores")
public class TutorGlobalDashboardController {

    private final GetTutorGlobalDashboard getTutorGlobalDashboard;
    private final UpdateTutorDashboardConfig updateTutorDashboardConfig;

    public TutorGlobalDashboardController(GetTutorGlobalDashboard getTutorGlobalDashboard,
                                          UpdateTutorDashboardConfig updateTutorDashboardConfig) {
        this.getTutorGlobalDashboard = getTutorGlobalDashboard;
        this.updateTutorDashboardConfig = updateTutorDashboardConfig;
    }

    @GetMapping("/me/dashboard-global")
    public ResponseEntity<TutorGlobalDashboardDTO> getGlobalDashboard(
            Authentication authentication) {
        var request = new com.recred.backend.application.GetTutorGlobalDashboardRequest(
                cognitoSubject(authentication));
        var response = getTutorGlobalDashboard.execute(request);
        
        List<TutorGlobalDashboardDTO.ChildDashboardDTO> children = response.childrenDashboards()
                .stream().map(c -> {
                    
            TutorGlobalDashboardDTO.BudgetSummaryDTO budget = c.budget() != null 
                    ? new TutorGlobalDashboardDTO.BudgetSummaryDTO(
                            c.budget().periodName(), 
                            c.budget().spent(), 
                            c.budget().limit()) 
                    : null;
            
            TutorGlobalDashboardDTO.HealthSummaryDTO health = c.health() != null 
                    ? new TutorGlobalDashboardDTO.HealthSummaryDTO(
                            c.health().rewardLevel(), 
                            c.health().rewardPoints(), 
                            c.health().pointsToNextLevel(), 
                            c.health().weeklyHealthSummary()) 
                    : null;
            
            List<TutorGlobalDashboardDTO.ScheduledPickupDTO> pickups = c.todayPickups()
                    .stream()
                    .map(p -> new TutorGlobalDashboardDTO.ScheduledPickupDTO(
                            p.purchaseId(), 
                            p.title(), 
                            p.slotDescription()))
                    .toList();
            
            List<TutorGlobalDashboardDTO.TransactionSummaryDTO> trans = c.recentTransactions()
                    .stream()
                    .map(t -> new TutorGlobalDashboardDTO.TransactionSummaryDTO(
                            t.id(), 
                            t.title(), 
                            t.date(), 
                            t.amount(), 
                            t.icon()))
                    .toList();
            
            TutorGlobalDashboardDTO.SmartActionDTO smartAction = c.smartAction() != null 
                    ? new TutorGlobalDashboardDTO.SmartActionDTO(
                            c.smartAction().title(), 
                            c.smartAction().description(), 
                            c.smartAction().actionType(), 
                            c.smartAction().actionValue()) 
                    : null;
            
            return new TutorGlobalDashboardDTO.ChildDashboardDTO(
                    c.id(), 
                    c.nombre(), 
                    c.saldoCreditos(), 
                    c.saldoDeuda(), 
                    c.urlFotoPerfil(),
                    c.predictionMessage(), 
                    budget, 
                    health, 
                    pickups, 
                    trans, 
                    smartAction);
        }).toList();

        TutorGlobalDashboardDTO dashboard = new TutorGlobalDashboardDTO(
                response.tutorId(), 
                response.totalBalance(), 
                response.totalDebt(), 
                response.dashboardConfig(), 
                children, 
                response.planStr()
        );
        return ResponseEntity.ok(dashboard);
    }

    @PutMapping("/me/dashboard-config")
    public ResponseEntity<Void> updateDashboardConfig(
            Authentication authentication,
            @RequestBody String config) {
        var updateRequest = new com.recred.backend.application.UpdateTutorDashboardConfigRequest(
                cognitoSubject(authentication), config);
        updateTutorDashboardConfig.execute(updateRequest);
        return ResponseEntity.ok().build();
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
