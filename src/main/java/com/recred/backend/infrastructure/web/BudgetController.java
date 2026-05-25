package com.recred.backend.infrastructure.web;

import com.recred.backend.application.BudgetCommand;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.UpdateBudget;
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

    public BudgetController(CreateBudget createBudget,
                            UpdateBudget updateBudget,
                            DisableBudget disableBudget,
                            GetActiveBudgetByStudent getActiveBudgetByStudent) {
        this.createBudget = createBudget;
        this.updateBudget = updateBudget;
        this.disableBudget = disableBudget;
        this.getActiveBudgetByStudent = getActiveBudgetByStudent;
    }

    @PostMapping
    public ResponseEntity<PresupuestoAlumno> create(@RequestBody BudgetCommand command) {
        PresupuestoAlumno presupuesto = createBudget.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(presupuesto);
    }

    @GetMapping("/student/{studentId}/active")
    public ResponseEntity<PresupuestoAlumno> getActiveByStudent(@PathVariable UUID studentId) {
        return getActiveBudgetByStudent.execute(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
}