package com.recred.backend.unit.application;

import com.recred.backend.application.GetGradesBySchool;
import com.recred.backend.application.GetGradesBySchoolRequest;
import com.recred.backend.application.GetGradesBySchoolResponse;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetGradesBySchoolTest {

    private final ColegioRepository schoolRepository = mock(ColegioRepository.class);
    private final GradoRepository gradeRepository = mock(GradoRepository.class);

    private GetGradesBySchool useCase;
    private UUID schoolId;
    private UUID firstGradeId;
    private UUID secondGradeId;
    private UUID levelId;

    @BeforeEach
    void setUp() {
        useCase = new GetGradesBySchool(schoolRepository, gradeRepository);
    }

    @Test
    void givenSchoolExists_whenGettingGrades_thenReturnsActiveGrades() {
        givenSchoolExists();
        givenSchoolHasActiveGrades();

        GetGradesBySchoolResponse response = whenGetGradesBySchool();

        thenActiveGradesWereReturned(response);
        thenSchoolWasValidated();
        thenActiveGradesWereRequested();
    }

    @Test
    void givenSchoolDoesNotExist_whenGettingGrades_thenThrowsException() {
        givenSchoolDoesNotExist();

        SchoolNotFoundException exception = whenGetGradesBySchoolFails();

        thenSchoolNotFoundWasThrown(exception);
        thenSchoolWasValidated();
        thenGradesWereNotRequested();
    }

    private void givenSchoolExists() {
        schoolId = randomUUID();
        when(schoolRepository.existsActiveById(schoolId)).thenReturn(true);
    }

    private void givenSchoolDoesNotExist() {
        schoolId = randomUUID();
        when(schoolRepository.existsActiveById(schoolId)).thenReturn(false);
    }

    private void givenSchoolHasActiveGrades() {
        firstGradeId = randomUUID();
        secondGradeId = randomUUID();
        levelId = randomUUID();

        when(gradeRepository.findAllActiveByColegioId(schoolId)).thenReturn(List.of(
                grade(firstGradeId, "1", "A"),
                grade(secondGradeId, "2", "B")
        ));
    }

    private GetGradesBySchoolResponse whenGetGradesBySchool() {
        return useCase.execute(new GetGradesBySchoolRequest(schoolId));
    }

    private SchoolNotFoundException whenGetGradesBySchoolFails() {
        Executable execution = () -> useCase.execute(new GetGradesBySchoolRequest(schoolId));
        return assertThrows(SchoolNotFoundException.class, execution);
    }

    private void thenActiveGradesWereReturned(GetGradesBySchoolResponse response) {
        assertEquals(2, response.grades().size());
        assertEquals(firstGradeId, response.grades().get(0).id());
        assertEquals(levelId, response.grades().get(0).nivelId());
        assertEquals("Primario", response.grades().get(0).nivelDescripcion());
        assertEquals("1", response.grades().get(0).anio());
        assertEquals("A", response.grades().get(0).division());
        assertEquals(secondGradeId, response.grades().get(1).id());
        assertEquals("2", response.grades().get(1).anio());
        assertEquals("B", response.grades().get(1).division());
    }

    private void thenSchoolNotFoundWasThrown(SchoolNotFoundException exception) {
        assertEquals("School not found: " + schoolId, exception.getMessage());
    }

    private void thenSchoolWasValidated() {
        verify(schoolRepository).existsActiveById(schoolId);
    }

    private void thenActiveGradesWereRequested() {
        verify(gradeRepository).findAllActiveByColegioId(schoolId);
    }

    private void thenGradesWereNotRequested() {
        verifyNoInteractions(gradeRepository);
    }

    private Grado grade(UUID gradeId, String year, String division) {
        Grado grade = new Grado();
        grade.setId(gradeId);
        grade.setNivel(level(levelId));
        grade.setAnio(year);
        grade.setDivision(division);
        return grade;
    }

    private Nivel level(UUID requestedLevelId) {
        Nivel level = new Nivel();
        level.setId(requestedLevelId);
        level.setDescripcion("Primario");
        level.setActivo(true);
        return level;
    }
}
