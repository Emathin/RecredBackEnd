package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetGradesBySchool;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public SchoolController(GetActiveSchools getActiveSchools,
                            GetGradesBySchool getGradesBySchool) {
        this.getActiveSchools = getActiveSchools;
        this.getGradesBySchool = getGradesBySchool;
    }

    @GetMapping
    public ResponseEntity<List<SchoolResponse>> getSchools() {
        List<SchoolResponse> schools = getActiveSchools.execute()
                .stream()
                .map(SchoolResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(schools);
    }

    @GetMapping("/{schoolId}/grados")
    public ResponseEntity<List<GradeResponse>> getGrades(@PathVariable UUID schoolId) {
        List<GradeResponse> grades = getGradesBySchool.execute(schoolId)
                .stream()
                .map(GradeResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(grades);
    }

    @ExceptionHandler(SchoolNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleSchoolNotFound(SchoolNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }
}
