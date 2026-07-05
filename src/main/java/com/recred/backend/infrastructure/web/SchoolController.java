package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateGrade;
import com.recred.backend.application.CreateGradeRequest;
import com.recred.backend.application.DeactivateGrade;
import com.recred.backend.application.DeactivateGradeRequest;
import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetActiveSchoolsRequest;
import com.recred.backend.application.GetGrade;
import com.recred.backend.application.GetGradeRequest;
import com.recred.backend.application.GetGradesBySchool;
import com.recred.backend.application.GetGradesBySchoolRequest;
import com.recred.backend.application.ListSchoolGrades;
import com.recred.backend.application.ListSchoolGradesRequest;
import com.recred.backend.application.ReactivateGrade;
import com.recred.backend.application.ReactivateGradeRequest;
import com.recred.backend.application.UpdateGrade;
import com.recred.backend.application.UpdateGradeRequest;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/colegios")
public class SchoolController {

    private final GetActiveSchools getActiveSchools;
    private final GetGradesBySchool getGradesBySchool;
    private final ListSchoolGrades listSchoolGrades;
    private final GetGrade getGrade;
    private final CreateGrade createGrade;
    private final UpdateGrade updateGrade;
    private final DeactivateGrade deactivateGrade;
    private final ReactivateGrade reactivateGrade;
    private final SchoolAccessGuard schoolAccessGuard;

    public SchoolController(GetActiveSchools getActiveSchools,
                            GetGradesBySchool getGradesBySchool,
                            ListSchoolGrades listSchoolGrades,
                            GetGrade getGrade,
                            CreateGrade createGrade,
                            UpdateGrade updateGrade,
                            DeactivateGrade deactivateGrade,
                            ReactivateGrade reactivateGrade,
                            SchoolAccessGuard schoolAccessGuard) {
        this.getActiveSchools = getActiveSchools;
        this.getGradesBySchool = getGradesBySchool;
        this.listSchoolGrades = listSchoolGrades;
        this.getGrade = getGrade;
        this.createGrade = createGrade;
        this.updateGrade = updateGrade;
        this.deactivateGrade = deactivateGrade;
        this.reactivateGrade = reactivateGrade;
        this.schoolAccessGuard = schoolAccessGuard;
    }

    @GetMapping
    public ResponseEntity<List<SchoolResponseDTO>> getSchools() {
        List<SchoolResponseDTO> schools = getActiveSchools
                .execute(new GetActiveSchoolsRequest())
                .schools()
                .stream()
                .map(SchoolResponseDTO::fromApplication)
                .toList();

        return ResponseEntity.ok(schools);
    }

    @GetMapping("/{schoolId}/grados")
    public ResponseEntity<List<GradeResponseDTO>> getGrades(
            @PathVariable UUID schoolId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Authentication authentication
    ) {
        if (includeInactive) {
            schoolAccessGuard.requireSchoolManagementAccess(authentication, schoolId);
        }

        return ResponseEntity.ok(grades(schoolId, includeInactive));
    }

    @GetMapping("/{schoolId}/grados/{gradeId}")
    public ResponseEntity<GradeResponseDTO> getGrade(
            @PathVariable UUID schoolId,
            @PathVariable UUID gradeId,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolManagementAccess(authentication, schoolId);
        return ResponseEntity.ok(GradeResponseDTO.fromApplication(
                getGrade.execute(new GetGradeRequest(schoolId, gradeId))));
    }

    @PostMapping("/{schoolId}/grados")
    public ResponseEntity<GradeResponseDTO> createGrade(
            @PathVariable UUID schoolId,
            @RequestBody GradeRequestDTO request,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolManagementAccess(authentication, schoolId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GradeResponseDTO.fromApplication(createGrade.execute(
                        new CreateGradeRequest(
                                schoolId,
                                request.nivelId(),
                                request.anio(),
                                request.division()
                        ))));
    }

    @PutMapping("/{schoolId}/grados/{gradeId}")
    public ResponseEntity<GradeResponseDTO> updateGrade(
            @PathVariable UUID schoolId,
            @PathVariable UUID gradeId,
            @RequestBody GradeRequestDTO request,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolManagementAccess(authentication, schoolId);
        return ResponseEntity.ok(GradeResponseDTO.fromApplication(updateGrade.execute(
                new UpdateGradeRequest(
                        schoolId,
                        gradeId,
                        request.nivelId(),
                        request.anio(),
                        request.division()
                ))));
    }

    @DeleteMapping("/{schoolId}/grados/{gradeId}")
    public ResponseEntity<Void> deactivateGrade(
            @PathVariable UUID schoolId,
            @PathVariable UUID gradeId,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolManagementAccess(authentication, schoolId);
        deactivateGrade.execute(new DeactivateGradeRequest(schoolId, gradeId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{schoolId}/grados/{gradeId}/reactivar")
    public ResponseEntity<GradeResponseDTO> reactivateGrade(
            @PathVariable UUID schoolId,
            @PathVariable UUID gradeId,
            Authentication authentication
    ) {
        schoolAccessGuard.requireSchoolManagementAccess(authentication, schoolId);
        return ResponseEntity.ok(GradeResponseDTO.fromApplication(
                reactivateGrade.execute(new ReactivateGradeRequest(schoolId, gradeId))));
    }

    private List<GradeResponseDTO> grades(UUID schoolId, boolean includeInactive) {
        if (includeInactive) {
            return listSchoolGrades
                    .execute(new ListSchoolGradesRequest(schoolId, true))
                    .stream()
                    .map(GradeResponseDTO::fromApplication)
                    .toList();
        }

        return getGradesBySchool
                .execute(new GetGradesBySchoolRequest(schoolId))
                .grades()
                .stream()
                .map(GradeResponseDTO::fromApplication)
                .toList();
    }

    @ExceptionHandler(SchoolNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleSchoolNotFound(SchoolNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }
}
