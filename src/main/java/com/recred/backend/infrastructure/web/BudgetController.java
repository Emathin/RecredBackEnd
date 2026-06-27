package com.recred.backend.infrastructure.web;

import com.recred.backend.application.BudgetCommand;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreateBudgetRequest;
import com.recred.backend.application.CreateBudgetResponse;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdateBudgetRulesCommand;
import com.recred.backend.application.CheckBudgetDates;
import com.recred.backend.application.CheckBudgetDatesRequest;
import com.recred.backend.application.CheckBudgetDatesResponse;
import com.recred.backend.domain.models.PresupuestoAlumno;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final CreateBudget createBudget;
    private final UpdateBudget updateBudget;
    private final DisableBudget disableBudget;
    private final GetActiveBudgetByStudent getActiveBudgetByStudent;
    private final UpdateBudgetRules updateBudgetRules;
    private final CheckBudgetDates checkBudgetDates;

    public BudgetController(CreateBudget createBudget,
                            UpdateBudget updateBudget,
                            DisableBudget disableBudget,
                            GetActiveBudgetByStudent getActiveBudgetByStudent,
                            UpdateBudgetRules updateBudgetRules,
                            CheckBudgetDates checkBudgetDates) {
        this.createBudget = createBudget;
        this.updateBudget = updateBudget;
        this.disableBudget = disableBudget;
        this.getActiveBudgetByStudent = getActiveBudgetByStudent;
        this.updateBudgetRules = updateBudgetRules;
        this.checkBudgetDates = checkBudgetDates;
    }

    @PostMapping
    public ResponseEntity<CreateBudgetResponse> create(@RequestBody CreateBudgetRequest request) {
        CreateBudgetResponse response = createBudget.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/student/{studentId}/active")
    public ResponseEntity<PresupuestoAlumno> getActiveByStudent(@PathVariable UUID studentId) {
        return getActiveBudgetByStudent.execute(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/student/{studentId}/check-dates")
    public ResponseEntity<CheckBudgetDatesResponse> checkDates(
            @PathVariable UUID studentId,
            @RequestBody CheckBudgetDatesRequest request) {
        CheckBudgetDatesResponse response = checkBudgetDates.execute(studentId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<PresupuestoAlumno> update(@PathVariable UUID budgetId,
                                                    @RequestBody BudgetCommand command) {
        PresupuestoAlumno presupuesto = updateBudget.execute(budgetId, command);
        return ResponseEntity.ok(presupuesto);
    }

    @PatchMapping("/{budgetId}/disable")
    public ResponseEntity<Void> disable(@PathVariable UUID budgetId) {
        disableBudget.execute(budgetId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{budgetId}/rules")
    public ResponseEntity<PresupuestoAlumno> updateCategoryRules(
            @PathVariable UUID budgetId,
            @RequestBody UpdateBudgetRulesCommand command) {

        PresupuestoAlumno actualizado = updateBudgetRules.execute(budgetId, command);
        return ResponseEntity.ok(actualizado);
    }
}