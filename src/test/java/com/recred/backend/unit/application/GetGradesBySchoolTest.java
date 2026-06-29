package com.recred.backend.unit.application;

import com.recred.backend.application.GetGradesBySchool;
import com.recred.backend.application.GetGradesBySchoolRequest;
import com.recred.backend.application.GetGradesBySchoolResponse;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetGradesBySchoolTest {

    @Mock
    private ColegioRepository schoolRepository;

    @Mock
    private GradoRepository gradeRepository;

    @InjectMocks
    private GetGradesBySchool getGradesBySchool;

    @Test
    void execute_WhenSchoolExists_ReturnsActiveGrades() {
        UUID schoolId = randomUUID();
        Grado firstGrade = grade("1", "A");
        Grado secondGrade = grade("2", "B");

        when(schoolRepository.existsActiveById(schoolId)).thenReturn(true);
        when(gradeRepository.findAllActiveByColegioId(schoolId))
                .thenReturn(List.of(firstGrade, secondGrade));

        GetGradesBySchoolResponse result = getGradesBySchool.execute(request(schoolId));

        assertGrades(result, firstGrade, secondGrade);
        verify(schoolRepository).existsActiveById(schoolId);
        verify(gradeRepository).findAllActiveByColegioId(schoolId);
    }

    private void assertGrades(
            GetGradesBySchoolResponse result,
            Grado firstGrade,
            Grado secondGrade) {
        assertEquals(2, result.grades().size());
        assertEquals(firstGrade.getId(), result.grades().get(0).id());
        assertEquals("1", result.grades().get(0).anio());
        assertEquals("A", result.grades().get(0).division());
        assertEquals(secondGrade.getId(), result.grades().get(1).id());
        assertEquals("2", result.grades().get(1).anio());
        assertEquals("B", result.grades().get(1).division());
    }

    @Test
    void execute_WhenSchoolDoesNotExist_ThrowsException() {
        UUID schoolId = randomUUID();

        when(schoolRepository.existsActiveById(schoolId)).thenReturn(false);

        assertThrows(
                SchoolNotFoundException.class,
                () -> getGradesBySchool.execute(request(schoolId))
        );
        verify(schoolRepository).existsActiveById(schoolId);
        verifyNoInteractions(gradeRepository);
    }

    private GetGradesBySchoolRequest request(UUID schoolId) {
        return new GetGradesBySchoolRequest(schoolId);
    }

    private Grado grade(String year, String division) {
        Grado grade = new Grado();
        grade.setId(randomUUID());
        grade.setAnio(year);
        grade.setDivision(division);
        return grade;
    }
}
