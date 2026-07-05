package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreateGrade;
import com.recred.backend.application.CreateGradeRequest;
import com.recred.backend.application.DeactivateGrade;
import com.recred.backend.application.DeactivateGradeRequest;
import com.recred.backend.application.GetActiveSchools;
import com.recred.backend.application.GetActiveSchoolsRequest;
import com.recred.backend.application.GetActiveSchoolsResponse;
import com.recred.backend.application.GetGrade;
import com.recred.backend.application.GetGradeRequest;
import com.recred.backend.application.GetGradesBySchool;
import com.recred.backend.application.GetGradesBySchoolRequest;
import com.recred.backend.application.GetGradesBySchoolResponse;
import com.recred.backend.application.GradeDetailsResponse;
import com.recred.backend.application.ListSchoolGrades;
import com.recred.backend.application.ListSchoolGradesRequest;
import com.recred.backend.application.ReactivateGrade;
import com.recred.backend.application.ReactivateGradeRequest;
import com.recred.backend.application.UpdateGrade;
import com.recred.backend.application.UpdateGradeRequest;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.infrastructure.web.GradeRequestDTO;
import com.recred.backend.infrastructure.web.GradeResponseDTO;
import com.recred.backend.infrastructure.web.SchoolAccessGuard;
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
    @Mock
    private ListSchoolGrades listSchoolGrades;
    @Mock
    private GetGrade getGrade;
    @Mock
    private CreateGrade createGrade;
    @Mock
    private UpdateGrade updateGrade;
    @Mock
    private DeactivateGrade deactivateGrade;
    @Mock
    private ReactivateGrade reactivateGrade;
    @Mock
    private SchoolAccessGuard schoolAccessGuard;

    private SchoolController schoolController;

    @BeforeEach
    void setUp() {
        schoolController = new SchoolController(
                getActiveSchools,
                getGradesBySchool,
                listSchoolGrades,
                getGrade,
                createGrade,
                updateGrade,
                deactivateGrade,
                reactivateGrade,
                schoolAccessGuard);
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
        UUID levelId = randomUUID();

        givenActiveGradesForSchool(schoolId, grade(gradeId, levelId));

        ResponseEntity<List<GradeResponseDTO>> response = whenGettingGrades(schoolId);

        GradeResponseDTO grade = firstOkBody(response);
        assertEquals(gradeId, grade.id());
        assertEquals(levelId, grade.nivelId());
        assertEquals("1 A", grade.nombre());
        verify(getGradesBySchool).execute(new GetGradesBySchoolRequest(schoolId));
    }

    @Test
    void getGrades_WhenIncludesInactive_ShouldRequireManagementAndUseAdminList() {
        UUID schoolId = randomUUID();
        UUID levelId = randomUUID();
        UUID gradeId = randomUUID();
        givenGradesIncludingInactive(schoolId,
                gradeResponse(schoolId, levelId, gradeId, false));

        ResponseEntity<List<GradeResponseDTO>> response =
                whenGettingGradesIncludingInactive(schoolId);

        GradeResponseDTO grade = firstOkBody(response);
        assertEquals(gradeId, grade.id());
        assertEquals(false, grade.activo());
        verify(schoolAccessGuard).requireSchoolManagementAccess(null, schoolId);
        verify(listSchoolGrades).execute(new ListSchoolGradesRequest(schoolId, true));
    }

    @Test
    void getGrade_ShouldRequireManagementAndReturnGrade() {
        UUID schoolId = randomUUID();
        UUID levelId = randomUUID();
        UUID gradeId = randomUUID();
        GetGradeRequest request = new GetGradeRequest(schoolId, gradeId);
        givenGradeDetails(request, gradeResponse(schoolId, levelId, gradeId, true));

        ResponseEntity<GradeResponseDTO> response = whenGettingGrade(schoolId, gradeId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(gradeId, response.getBody().id());
        verify(schoolAccessGuard).requireSchoolManagementAccess(null, schoolId);
        verify(getGrade).execute(request);
    }

    @Test
    void createGrade_ShouldRequireManagementAndReturnCreatedGrade() {
        UUID schoolId = randomUUID();
        UUID levelId = randomUUID();
        UUID gradeId = randomUUID();
        GradeRequestDTO request = new GradeRequestDTO(levelId, "1", "A");
        CreateGradeRequest useCaseRequest = new CreateGradeRequest(
                schoolId,
                levelId,
                "1",
                "A");
        givenCreatedGrade(useCaseRequest, gradeResponse(schoolId, levelId, gradeId, true));

        ResponseEntity<GradeResponseDTO> response = whenCreatingGrade(schoolId, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(gradeId, response.getBody().id());
        verify(schoolAccessGuard).requireSchoolManagementAccess(null, schoolId);
        verify(createGrade).execute(useCaseRequest);
    }

    @Test
    void updateGrade_ShouldRequireManagementAndReturnUpdatedGrade() {
        UUID schoolId = randomUUID();
        UUID levelId = randomUUID();
        UUID gradeId = randomUUID();
        GradeRequestDTO request = new GradeRequestDTO(levelId, "2", "B");
        UpdateGradeRequest useCaseRequest = new UpdateGradeRequest(
                schoolId,
                gradeId,
                levelId,
                "2",
                "B");
        givenUpdatedGrade(useCaseRequest, gradeResponse(schoolId, levelId, gradeId, true));

        ResponseEntity<GradeResponseDTO> response =
                whenUpdatingGrade(schoolId, gradeId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(gradeId, response.getBody().id());
        verify(schoolAccessGuard).requireSchoolManagementAccess(null, schoolId);
        verify(updateGrade).execute(useCaseRequest);
    }

    @Test
    void deactivateGrade_ShouldRequireManagementAndReturnNoContent() {
        UUID schoolId = randomUUID();
        UUID gradeId = randomUUID();
        DeactivateGradeRequest request = new DeactivateGradeRequest(schoolId, gradeId);

        ResponseEntity<Void> response = whenDeactivatingGrade(schoolId, gradeId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(schoolAccessGuard).requireSchoolManagementAccess(null, schoolId);
        verify(deactivateGrade).execute(request);
    }

    @Test
    void reactivateGrade_ShouldRequireManagementAndReturnGrade() {
        UUID schoolId = randomUUID();
        UUID levelId = randomUUID();
        UUID gradeId = randomUUID();
        ReactivateGradeRequest request = new ReactivateGradeRequest(schoolId, gradeId);
        givenReactivatedGrade(request, gradeResponse(schoolId, levelId, gradeId, true));

        ResponseEntity<GradeResponseDTO> response = whenReactivatingGrade(schoolId, gradeId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().activo());
        verify(schoolAccessGuard).requireSchoolManagementAccess(null, schoolId);
        verify(reactivateGrade).execute(request);
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

    private void givenGradesIncludingInactive(
            UUID schoolId,
            GradeDetailsResponse... grades
    ) {
        when(listSchoolGrades.execute(new ListSchoolGradesRequest(schoolId, true)))
                .thenReturn(List.of(grades));
    }

    private void givenGradeDetails(GetGradeRequest request, GradeDetailsResponse response) {
        when(getGrade.execute(request)).thenReturn(response);
    }

    private void givenCreatedGrade(CreateGradeRequest request, GradeDetailsResponse response) {
        when(createGrade.execute(request)).thenReturn(response);
    }

    private void givenUpdatedGrade(UpdateGradeRequest request, GradeDetailsResponse response) {
        when(updateGrade.execute(request)).thenReturn(response);
    }

    private void givenReactivatedGrade(
            ReactivateGradeRequest request,
            GradeDetailsResponse response
    ) {
        when(reactivateGrade.execute(request)).thenReturn(response);
    }

    private ResponseEntity<List<SchoolResponseDTO>> whenGettingSchools() {
        return schoolController.getSchools();
    }

    private ResponseEntity<List<GradeResponseDTO>> whenGettingGrades(UUID schoolId) {
        return schoolController.getGrades(schoolId, false, null);
    }

    private ResponseEntity<List<GradeResponseDTO>> whenGettingGradesIncludingInactive(
            UUID schoolId
    ) {
        return schoolController.getGrades(schoolId, true, null);
    }

    private ResponseEntity<GradeResponseDTO> whenGettingGrade(
            UUID schoolId,
            UUID gradeId
    ) {
        return schoolController.getGrade(schoolId, gradeId, null);
    }

    private ResponseEntity<GradeResponseDTO> whenCreatingGrade(
            UUID schoolId,
            GradeRequestDTO request
    ) {
        return schoolController.createGrade(schoolId, request, null);
    }

    private ResponseEntity<GradeResponseDTO> whenUpdatingGrade(
            UUID schoolId,
            UUID gradeId,
            GradeRequestDTO request
    ) {
        return schoolController.updateGrade(schoolId, gradeId, request, null);
    }

    private ResponseEntity<Void> whenDeactivatingGrade(UUID schoolId, UUID gradeId) {
        return schoolController.deactivateGrade(schoolId, gradeId, null);
    }

    private ResponseEntity<GradeResponseDTO> whenReactivatingGrade(
            UUID schoolId,
            UUID gradeId
    ) {
        return schoolController.reactivateGrade(schoolId, gradeId, null);
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

    private Grado grade(UUID gradeId, UUID levelId) {
        Grado grade = new Grado();
        grade.setId(gradeId);
        grade.setNivel(level(levelId));
        grade.setAnio("1");
        grade.setDivision("A");
        return grade;
    }

    private GradeDetailsResponse gradeResponse(
            UUID schoolId,
            UUID levelId,
            UUID gradeId,
            boolean active
    ) {
        return new GradeDetailsResponse(
                gradeId,
                schoolId,
                levelId,
                "Primario",
                "1",
                "A",
                active);
    }

    private Nivel level(UUID levelId) {
        Nivel level = new Nivel();
        level.setId(levelId);
        level.setDescripcion("Primario");
        level.setActivo(true);
        return level;
    }

    private <T> T firstOkBody(ResponseEntity<List<T>> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        return response.getBody().get(0);
    }
}
