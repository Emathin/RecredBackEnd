package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.RemoveHealthRestrictions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/control-parental")
public class ParentalControlController {

    private final ManageRestrictions useCase;
    private final GetHealthRestrictions getRestrictionsUseCase;
    private final AddHealthRestriction addHealthRestriction;
    private final RemoveHealthRestrictions removeHealthRestrictions;
    private final DetectStudentPreferences detectPreferencesUseCase;

    public ParentalControlController(ManageRestrictions useCase,
                                     GetHealthRestrictions getRestrictionsUseCase,
                                     AddHealthRestriction addHealthRestriction,
                                     RemoveHealthRestrictions removeHealthRestrictions,
                                     DetectStudentPreferences detectPreferencesUseCase) {
        this.useCase = useCase;
        this.getRestrictionsUseCase = getRestrictionsUseCase;
        this.addHealthRestriction = addHealthRestriction;
        this.removeHealthRestrictions = removeHealthRestrictions;
        this.detectPreferencesUseCase = detectPreferencesUseCase;
    }

    @PutMapping("/alumnos/{studentID}/actualizar-restricciones-salud")
    public ResponseEntity<Void> updateHealthRestrictions(
            @PathVariable UUID studentID,
            @RequestBody UpdateRestrictionsDTO dto) {

        useCase.execute(studentID, dto.getClasificationIDs());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alumnos/{studentID}/obtener-restricciones-salud")
    public ResponseEntity<List<HealthClassificationDTO>> getHealthRestrictions(
            @PathVariable UUID studentID) {

        var domainRestrictions = getRestrictionsUseCase.ejecutar(studentID);
        List<HealthClassificationDTO> response = domainRestrictions.stream()
                .map(HealthClassificationDTO::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/alumnos/{studentID}/restricciones-salud/{classificationID}")
    public ResponseEntity<Void> addHealthRestriction(
            @PathVariable UUID studentID,
            @PathVariable UUID classificationID) {

        addHealthRestriction.execute(studentID, classificationID);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{studentID}/restricciones-salud/{classificationID}")
    public ResponseEntity<Void> removeHealthRestriction(
            @PathVariable UUID studentID,
            @PathVariable UUID classificationID) {
        removeHealthRestrictions.ejecutar(studentID, classificationID);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alumnos/{studentID}/analizar-preferencias")
    public ResponseEntity<Void> analyzePreferences(
            @PathVariable UUID studentID,
            @RequestParam UUID buffetId) {
        detectPreferencesUseCase.execute(studentID, buffetId);
        return ResponseEntity.accepted().build();
    }
}
