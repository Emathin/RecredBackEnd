package com.recred.backend.unit.application;

import com.recred.backend.application.GetGradesBySchool;
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
        UUID schoolId = UUID.randomUUID();
        List<Grado> expectedGrades = List.of(new Grado(), new Grado());

        when(schoolRepository.existsActiveById(schoolId)).thenReturn(true);
        when(gradeRepository.findAllActiveByColegioId(schoolId)).thenReturn(expectedGrades);

        List<Grado> result = getGradesBySchool.execute(schoolId);

        assertEquals(expectedGrades, result);
        verify(schoolRepository).existsActiveById(schoolId);
        verify(gradeRepository).findAllActiveByColegioId(schoolId);
    }

    @Test
    void execute_WhenSchoolDoesNotExist_ThrowsException() {
        UUID schoolId = UUID.randomUUID();

        when(schoolRepository.existsActiveById(schoolId)).thenReturn(false);

        assertThrows(SchoolNotFoundException.class, () -> getGradesBySchool.execute(schoolId));
        verify(schoolRepository).existsActiveById(schoolId);
        verifyNoInteractions(gradeRepository);
    }
}
