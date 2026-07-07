package com.recred.backend.unit.application;

import com.recred.backend.application.CreateGrade;
import com.recred.backend.application.CreateGradeRequest;
import com.recred.backend.application.DeactivateGrade;
import com.recred.backend.application.DeactivateGradeRequest;
import com.recred.backend.application.GradeDetailsResponse;
import com.recred.backend.application.ListSchoolGrades;
import com.recred.backend.application.ListSchoolGradesRequest;
import com.recred.backend.application.ReactivateGrade;
import com.recred.backend.application.ReactivateGradeRequest;
import com.recred.backend.application.UpdateGrade;
import com.recred.backend.application.UpdateGradeRequest;
import com.recred.backend.domain.exceptions.GradeAlreadyExistsException;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.domain.repositories.NivelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GradeManagementUseCasesTest {

    private final GradoRepository gradeRepository = mock(GradoRepository.class);
    private final ColegioRepository schoolRepository = mock(ColegioRepository.class);
    private final NivelRepository levelRepository = mock(NivelRepository.class);

    @Test
    void create_ShouldSaveActiveGradeForActiveSchool() {
        UUID schoolId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        CreateGradeRequest request = new CreateGradeRequest(schoolId, levelId, " 1 ", " A ");

        givenActiveSchool(schoolId);
        givenActiveLevel(levelId);
        givenNoActiveDuplicate(schoolId, levelId, null, "1", "A");
        givenSavedGradeWithId(gradeId);

        GradeDetailsResponse response = whenCreatingGrade(request);

        assertEquals(gradeId, response.id());
        assertEquals(levelId, response.nivelId());
        assertEquals("1", response.anio());
        assertEquals("A", response.division());
        assertTrue(response.activo());
    }

    @Test
    void create_WhenActiveGradeAlreadyExists_ShouldThrowException() {
        UUID schoolId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        CreateGradeRequest request = new CreateGradeRequest(schoolId, levelId, "1", "A");

        givenActiveSchool(schoolId);
        givenActiveLevel(levelId);
        givenActiveDuplicate(schoolId, levelId, null, "1", "A");

        Executable execution = () -> whenCreatingGrade(request);

        assertThrows(GradeAlreadyExistsException.class, execution);
        verify(gradeRepository, never()).save(any(Grado.class));
    }

    @Test
    void update_WhenGradeIsActive_ShouldValidateDuplicateAndSave() {
        UUID schoolId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        Grado existing = grade(gradeId, schoolId, levelId, "1", "A", true);
        UpdateGradeRequest request = new UpdateGradeRequest(
                schoolId,
                gradeId,
                levelId,
                "2",
                "B");

        givenActiveSchool(schoolId);
        givenActiveLevel(levelId);
        givenGradeExists(gradeId, schoolId, existing);
        givenNoActiveDuplicate(schoolId, levelId, gradeId, "2", "B");
        givenSavedGrade(existing);

        GradeDetailsResponse response = whenUpdatingGrade(request);

        assertEquals(levelId, response.nivelId());
        assertEquals("2", response.anio());
        assertEquals("B", response.division());
        verify(gradeRepository).save(existing);
    }

    @Test
    void deactivate_ShouldSaveInactiveGrade() {
        UUID schoolId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        Grado existing = grade(gradeId, schoolId, levelId, "1", "A", true);
        DeactivateGradeRequest request = new DeactivateGradeRequest(schoolId, gradeId);

        givenGradeExists(gradeId, schoolId, existing);
        givenSavedGrade(existing);

        GradeDetailsResponse response = whenDeactivatingGrade(request);

        assertFalse(response.activo());
        verify(gradeRepository).save(existing);
    }

    @Test
    void reactivate_WhenActiveDuplicateExists_ShouldThrowException() {
        UUID schoolId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        Grado existing = grade(gradeId, schoolId, levelId, "1", "A", false);
        ReactivateGradeRequest request = new ReactivateGradeRequest(schoolId, gradeId);

        givenGradeExists(gradeId, schoolId, existing);
        givenActiveDuplicate(schoolId, levelId, gradeId, "1", "A");

        Executable execution = () -> whenReactivatingGrade(request);

        assertThrows(GradeAlreadyExistsException.class, execution);
        verify(gradeRepository, never()).save(any(Grado.class));
    }

    @Test
    void list_WhenIncludesInactive_ShouldUseAdministrativeQuery() {
        UUID schoolId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        Grado inactive = grade(UUID.randomUUID(), schoolId, levelId, "1", "A", false);
        ListSchoolGradesRequest request = new ListSchoolGradesRequest(schoolId, true);

        givenActiveSchool(schoolId);
        givenGradesIncludingInactive(schoolId, inactive);

        List<GradeDetailsResponse> response = whenListingGrades(request);

        assertEquals(1, response.size());
        assertFalse(response.get(0).activo());
        verify(gradeRepository).findAllByColegioIdIncludingInactive(schoolId);
    }

    private void givenActiveSchool(UUID schoolId) {
        when(schoolRepository.existsActiveById(schoolId)).thenReturn(true);
    }

    private void givenActiveLevel(UUID levelId) {
        when(levelRepository.findActiveById(levelId))
                .thenReturn(Optional.of(level(levelId)));
    }

    private void givenNoActiveDuplicate(
            UUID schoolId,
            UUID levelId,
            UUID excludedId,
            String anio,
            String division
    ) {
        when(gradeRepository.existsActiveByColegioIdAndNivelIdAndAnioAndDivision(
                schoolId,
                levelId,
                excludedId,
                anio,
                division)).thenReturn(false);
    }

    private void givenActiveDuplicate(
            UUID schoolId,
            UUID levelId,
            UUID excludedId,
            String anio,
            String division
    ) {
        when(gradeRepository.existsActiveByColegioIdAndNivelIdAndAnioAndDivision(
                schoolId,
                levelId,
                excludedId,
                anio,
                division)).thenReturn(true);
    }

    private void givenSavedGradeWithId(UUID gradeId) {
        when(gradeRepository.save(any(Grado.class))).thenAnswer(invocation -> {
            Grado grade = invocation.getArgument(0);
            grade.setId(gradeId);
            return grade;
        });
    }

    private void givenGradeExists(UUID gradeId, UUID schoolId, Grado grade) {
        when(gradeRepository.findByIdAndColegioId(gradeId, schoolId))
                .thenReturn(Optional.of(grade));
    }

    private void givenSavedGrade(Grado grade) {
        when(gradeRepository.save(grade)).thenReturn(grade);
    }

    private void givenGradesIncludingInactive(UUID schoolId, Grado... grades) {
        when(gradeRepository.findAllByColegioIdIncludingInactive(schoolId))
                .thenReturn(List.of(grades));
    }

    private GradeDetailsResponse whenCreatingGrade(CreateGradeRequest request) {
        return new CreateGrade(gradeRepository, schoolRepository, levelRepository)
                .execute(request);
    }

    private GradeDetailsResponse whenUpdatingGrade(UpdateGradeRequest request) {
        return new UpdateGrade(gradeRepository, schoolRepository, levelRepository)
                .execute(request);
    }

    private GradeDetailsResponse whenDeactivatingGrade(DeactivateGradeRequest request) {
        return new DeactivateGrade(gradeRepository).execute(request);
    }

    private GradeDetailsResponse whenReactivatingGrade(ReactivateGradeRequest request) {
        return new ReactivateGrade(gradeRepository).execute(request);
    }

    private List<GradeDetailsResponse> whenListingGrades(ListSchoolGradesRequest request) {
        return new ListSchoolGrades(gradeRepository, schoolRepository).execute(request);
    }

    private Grado grade(
            UUID gradeId,
            UUID schoolId,
            UUID levelId,
            String anio,
            String division,
            boolean activo
    ) {
        Colegio school = new Colegio();
        school.setId(schoolId);

        Grado grade = new Grado();
        grade.setId(gradeId);
        grade.setColegio(school);
        grade.setNivel(level(levelId));
        grade.setAnio(anio);
        grade.setDivision(division);
        grade.setActivo(activo);
        return grade;
    }

    private Nivel level(UUID levelId) {
        Nivel level = new Nivel();
        level.setId(levelId);
        level.setDescripcion("Primario");
        level.setActivo(true);
        return level;
    }
}
