package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetActiveSchoolsRequest;
import com.recred.backend.application.GetActiveSchoolsResponse;
import com.recred.backend.application.GetGradesBySchool;
import com.recred.backend.application.GetGradesBySchoolRequest;
import com.recred.backend.application.GetGradesBySchoolResponse;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.infrastructure.web.GradeResponseDTO;
import com.recred.backend.infrastructure.web.SchoolController;
import com.recred.backend.infrastructure.web.SchoolResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolControllerTest {

    @Mock
    private GetActiveSchools getActiveSchools;
    @Mock
    private GetGradesBySchool getGradesBySchool;

    private SchoolController schoolController;

    @BeforeEach
    void setUp() {
        schoolController = new SchoolController(getActiveSchools, getGradesBySchool);
    }

    @Test
    void getSchools_ReturnsActiveSchools() {
        UUID schoolId = randomUUID();
        givenActiveSchools(school(schoolId));

        ResponseEntity<List<SchoolResponseDTO>> response = whenGettingSchools();

        SchoolResponseDTO school = firstOkBody(response);
        assertEquals(schoolId, school.id());
        assertEquals("North School", school.nombre());
        verify(getActiveSchools).execute(new GetActiveSchoolsRequest());
    }

    @Test
    void getGrades_ReturnsActiveGradesForSchool() {
        UUID schoolId = randomUUID();
        UUID gradeId = randomUUID();

        givenActiveGradesForSchool(schoolId, grade(gradeId));

        ResponseEntity<List<GradeResponseDTO>> response = whenGettingGrades(schoolId);

        GradeResponseDTO grade = firstOkBody(response);
        assertEquals(gradeId, grade.id());
        assertEquals("1 A", grade.nombre());
        verify(getGradesBySchool).execute(new GetGradesBySchoolRequest(schoolId));
    }

    @Test
    void handleSchoolNotFound_ReturnsErrorMessage() {
        UUID schoolId = randomUUID();
        SchoolNotFoundException exception = new SchoolNotFoundException(schoolId);

        Map<String, String> response = whenHandlingSchoolNotFound(exception);

        assertEquals("School not found: " + schoolId, response.get("error"));
    }

    private void givenActiveSchools(Colegio... schools) {
        when(getActiveSchools.execute(new GetActiveSchoolsRequest()))
                .thenReturn(GetActiveSchoolsResponse.fromDomain(List.of(schools)));
    }

    private void givenActiveGradesForSchool(UUID schoolId, Grado... grades) {
        when(getGradesBySchool.execute(new GetGradesBySchoolRequest(schoolId)))
                .thenReturn(GetGradesBySchoolResponse.fromDomain(List.of(grades)));
    }

    private ResponseEntity<List<SchoolResponseDTO>> whenGettingSchools() {
        return schoolController.getSchools();
    }

    private ResponseEntity<List<GradeResponseDTO>> whenGettingGrades(UUID schoolId) {
        return schoolController.getGrades(schoolId);
    }

    private Map<String, String> whenHandlingSchoolNotFound(
            SchoolNotFoundException exception) {
        return schoolController.handleSchoolNotFound(exception);
    }

    private Colegio school(UUID schoolId) {
        Nivel level = new Nivel();
        level.setDescripcion("Primary");

        Colegio school = new Colegio();
        school.setId(schoolId);
        school.setNombre("North School");
        school.setCue("123");
        school.setNivel(level);
        return school;
    }

    private Grado grade(UUID gradeId) {
        Grado grade = new Grado();
        grade.setId(gradeId);
        grade.setAnio("1");
        grade.setDivision("A");
        return grade;
    }

    private <T> T firstOkBody(ResponseEntity<List<T>> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        return response.getBody().get(0);
    }
}
