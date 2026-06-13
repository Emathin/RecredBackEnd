package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetGradesBySchool;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Nivel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolControllerTest {

    private GetActiveSchools getActiveSchools;
    private GetGradesBySchool getGradesBySchool;
    private SchoolController schoolController;

    @BeforeEach
    void setUp() {
        getActiveSchools = mock(GetActiveSchools.class);
        getGradesBySchool = mock(GetGradesBySchool.class);
        schoolController = new SchoolController(getActiveSchools, getGradesBySchool);
    }

    @Test
    void getSchools_ReturnsActiveSchools() {
        UUID schoolId = UUID.randomUUID();
        Nivel level = new Nivel();
        level.setDescripcion("Primary");

        Colegio school = new Colegio();
        school.setId(schoolId);
        school.setNombre("North School");
        school.setCue("123");
        school.setNivel(level);

        when(getActiveSchools.execute()).thenReturn(List.of(school));

        ResponseEntity<List<SchoolResponse>> response = schoolController.getSchools();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(schoolId, response.getBody().get(0).id());
        assertEquals("North School", response.getBody().get(0).name());
        assertEquals("Primary", response.getBody().get(0).level());
        verify(getActiveSchools).execute();
    }

    @Test
    void getGrades_ReturnsActiveGradesForSchool() {
        UUID schoolId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();

        Grado grade = new Grado();
        grade.setId(gradeId);
        grade.setAnio("1");
        grade.setDivision("A");

        when(getGradesBySchool.execute(schoolId)).thenReturn(List.of(grade));

        ResponseEntity<List<GradeResponse>> response = schoolController.getGrades(schoolId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(gradeId, response.getBody().get(0).id());
        assertEquals("1", response.getBody().get(0).year());
        assertEquals("A", response.getBody().get(0).division());
        assertEquals("1 A", response.getBody().get(0).description());
        verify(getGradesBySchool).execute(schoolId);
    }

    @Test
    void handleSchoolNotFound_ReturnsErrorMessage() {
        UUID schoolId = UUID.randomUUID();
        SchoolNotFoundException exception = new SchoolNotFoundException(schoolId);

        Map<String, String> response = schoolController.handleSchoolNotFound(exception);

        assertEquals("School not found: " + schoolId, response.get("error"));
    }
}
