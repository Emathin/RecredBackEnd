package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreateBudgetRequest;
import com.recred.backend.application.CreateBudgetResponse;
import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableBudgetRequest;
import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetActiveBudgetByStudentRequest;
import com.recred.backend.application.GetActiveBudgetByStudentResponse;
import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRequest;
import com.recred.backend.application.UpdateBudgetResponse;
import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdateBudgetRulesRequest;
import com.recred.backend.application.UpdateBudgetRulesResponse;
import com.recred.backend.application.CheckBudgetDates;
import com.recred.backend.application.CheckBudgetDatesRequest;
import com.recred.backend.application.CheckBudgetDatesResponse;
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
    public ResponseEntity<GetActiveBudgetByStudentResponse> getActiveByStudent(
            @PathVariable UUID studentId) {
        return getActiveBudgetByStudent.execute(new GetActiveBudgetByStudentRequest(studentId))
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
    public ResponseEntity<UpdateBudgetResponse> update(@PathVariable UUID budgetId,
                                                    @RequestBody UpdateBudgetRequest requestBody) {
        UpdateBudgetRequest request = new UpdateBudgetRequest(
                budgetId,
                requestBody.limitAmount(),
                requestBody.period(),
                requestBody.startDate(),
                requestBody.categoryRules()
        );
        UpdateBudgetResponse response = updateBudget.execute(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{budgetId}/disable")
    public ResponseEntity<Void> disable(@PathVariable UUID budgetId) {
        disableBudget.execute(new DisableBudgetRequest(budgetId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{budgetId}/rules")
    public ResponseEntity<UpdateBudgetRulesResponse> updateCategoryRules(
            @PathVariable UUID budgetId,
            @RequestBody UpdateBudgetRulesRequest requestBody) {

        UpdateBudgetRulesRequest request = new UpdateBudgetRulesRequest(
                budgetId,
                requestBody.rules()
        );
        UpdateBudgetRulesResponse actualizado = updateBudgetRules.execute(request);
        return ResponseEntity.ok(actualizado);
    }
}