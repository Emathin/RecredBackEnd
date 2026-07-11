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
import com.recred.backend.application.GetSchoolOverview;
import com.recred.backend.application.GetSchoolOverviewRequest;
import com.recred.backend.application.GetSchoolOverviewResponse;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

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

    private static final UUID SCHOOL_ID = randomUUID();
    private static final UUID GRADE_ID = randomUUID();
    private static final UUID LEVEL_ID = randomUUID();
    private static final String COGNITO_SUBJECT = "test-subject";

    @Mock private GetActiveSchools getActiveSchools;
    @Mock private GetGradesBySchool getGradesBySchool;
    @Mock private GetSchoolOverview getSchoolOverview;
    @Mock private ListSchoolGrades listSchoolGrades;
    @Mock private GetGrade getGrade;
    @Mock private CreateGrade createGrade;
    @Mock private UpdateGrade updateGrade;
    @Mock private DeactivateGrade deactivateGrade;
    @Mock private ReactivateGrade reactivateGrade;
    @Mock private SchoolAccessGuard schoolAccessGuard;

    private SchoolController schoolController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        schoolController = new SchoolController(
                getActiveSchools,
                getGradesBySchool,
                getSchoolOverview,
                listSchoolGrades,
                getGrade,
                createGrade,
                updateGrade,
                deactivateGrade,
                reactivateGrade,
                schoolAccessGuard);
        authentication = new TestingAuthenticationToken(COGNITO_SUBJECT, null);
    }

    @Test
    void getSchoolsReturnsActiveSchools() {
        givenActiveSchools();

        ResponseEntity<List<SchoolResponseDTO>> response = whenGettingSchools();

        thenSchoolResponseContainsActiveSchools(response);
        thenDelegatesToGetActiveSchools();
    }

    @Test
    void getGradesReturnsActiveGradesForSchool() {
        givenActiveGradesForSchool();

        ResponseEntity<List<GradeResponseDTO>> response = whenGettingActiveGrades();

        thenGradeResponseContainsActiveGrades(response);
        thenDelegatesToGetGradesBySchool();
    }

    @Test
    void getGradesWhenIncludesInactiveShouldRequireManagementAndUseAdminList() {
        givenGradesIncludingInactive();

        ResponseEntity<List<GradeResponseDTO>> response =
                whenGettingGradesIncludingInactive();

        thenGradeResponseContainsInactiveGrades(response);
        thenSchoolManagementAccessIsRequired();
        thenDelegatesToListSchoolGradesIncludingInactive();
    }

    @Test
    void getGradeShouldRequireManagementAndReturnGrade() {
        givenGradeDetails();

        ResponseEntity<GradeResponseDTO> response = whenGettingGrade();

        thenGradeResponseIsOk(response);
        thenSchoolManagementAccessIsRequired();
        thenDelegatesToGetGrade();
    }

    @Test
    void createGradeShouldRequireManagementAndReturnCreatedGrade() {
        givenCreatedGrade();

        ResponseEntity<GradeResponseDTO> response = whenCreatingGrade();

        thenGradeResponseIsCreated(response);
        thenSchoolManagementAccessIsRequired();
        thenDelegatesToCreateGrade();
    }

    @Test
    void updateGradeShouldRequireManagementAndReturnUpdatedGrade() {
        givenUpdatedGrade();

        ResponseEntity<GradeResponseDTO> response = whenUpdatingGrade();

        thenGradeResponseIsOk(response);
        thenSchoolManagementAccessIsRequired();
        thenDelegatesToUpdateGrade();
    }

    @Test
    void deactivateGradeShouldRequireManagementAndReturnNoContent() {
        ResponseEntity<Void> response = whenDeactivatingGrade();

        thenResponseIsNoContent(response);
        thenSchoolManagementAccessIsRequired();
        thenDelegatesToDeactivateGrade();
    }

    @Test
    void reactivateGradeShouldRequireManagementAndReturnGrade() {
        givenReactivatedGrade();

        ResponseEntity<GradeResponseDTO> response = whenReactivatingGrade();

        thenReactivatedGradeResponseIsOk(response);
        thenSchoolManagementAccessIsRequired();
        thenDelegatesToReactivateGrade();
    }

    @Test
    void getMySchoolOverviewReturnsOverview() {
        givenSchoolOverview();

        ResponseEntity<GetSchoolOverviewResponse> response =
                whenGettingMySchoolOverview();

        thenSchoolOverviewIsReturned(response);
    }

    @Test
    void handleSchoolNotFoundReturnsErrorMessage() {
        SchoolNotFoundException exception = new SchoolNotFoundException(SCHOOL_ID);

        Map<String, String> response = whenHandlingSchoolNotFound(exception);

        thenErrorMessageIsReturned(response);
    }

    private void givenActiveSchools() {
        when(getActiveSchools.execute(new GetActiveSchoolsRequest()))
                .thenReturn(GetActiveSchoolsResponse.fromDomain(List.of(school(SCHOOL_ID))));
    }

    private void givenActiveGradesForSchool() {
        when(getGradesBySchool.execute(new GetGradesBySchoolRequest(SCHOOL_ID)))
                .thenReturn(GetGradesBySchoolResponse.fromDomain(List.of(grade(GRADE_ID, LEVEL_ID))));
    }

    private void givenSchoolOverview() {
        when(getSchoolOverview.execute(new GetSchoolOverviewRequest(COGNITO_SUBJECT)))
                .thenReturn(new GetSchoolOverviewResponse(
                        SCHOOL_ID,
                        "Colegio Test",
                        "12345",
                        List.of()
                ));
    }

    private void givenGradesIncludingInactive() {
        when(listSchoolGrades.execute(new ListSchoolGradesRequest(SCHOOL_ID, true)))
                .thenReturn(List.of(gradeResponse(SCHOOL_ID, LEVEL_ID, GRADE_ID, false)));
    }

    private void givenGradeDetails() {
        when(getGrade.execute(new GetGradeRequest(SCHOOL_ID, GRADE_ID)))
                .thenReturn(gradeResponse(SCHOOL_ID, LEVEL_ID, GRADE_ID, true));
    }

    private void givenCreatedGrade() {
        when(createGrade.execute(new CreateGradeRequest(SCHOOL_ID, LEVEL_ID, "1", "A")))
                .thenReturn(gradeResponse(SCHOOL_ID, LEVEL_ID, GRADE_ID, true));
    }

    private void givenUpdatedGrade() {
        when(updateGrade.execute(new UpdateGradeRequest(SCHOOL_ID, GRADE_ID, LEVEL_ID, "2", "B")))
                .thenReturn(gradeResponse(SCHOOL_ID, LEVEL_ID, GRADE_ID, true));
    }

    private void givenReactivatedGrade() {
        when(reactivateGrade.execute(new ReactivateGradeRequest(SCHOOL_ID, GRADE_ID)))
                .thenReturn(gradeResponse(SCHOOL_ID, LEVEL_ID, GRADE_ID, true));
    }

    private ResponseEntity<List<SchoolResponseDTO>> whenGettingSchools() {
        return schoolController.getSchools();
    }

    private ResponseEntity<List<GradeResponseDTO>> whenGettingActiveGrades() {
        return schoolController.getGrades(SCHOOL_ID, false, null);
    }

    private ResponseEntity<List<GradeResponseDTO>> whenGettingGradesIncludingInactive() {
        return schoolController.getGrades(SCHOOL_ID, true, null);
    }

    private ResponseEntity<GradeResponseDTO> whenGettingGrade() {
        return schoolController.getGrade(SCHOOL_ID, GRADE_ID, null);
    }

    private ResponseEntity<GradeResponseDTO> whenCreatingGrade() {
        GradeRequestDTO request = new GradeRequestDTO(LEVEL_ID, "1", "A");
        return schoolController.createGrade(SCHOOL_ID, request, null);
    }

    private ResponseEntity<GradeResponseDTO> whenUpdatingGrade() {
        GradeRequestDTO request = new GradeRequestDTO(LEVEL_ID, "2", "B");
        return schoolController.updateGrade(SCHOOL_ID, GRADE_ID, request, null);
    }

    private ResponseEntity<Void> whenDeactivatingGrade() {
        return schoolController.deactivateGrade(SCHOOL_ID, GRADE_ID, null);
    }

    private ResponseEntity<GradeResponseDTO> whenReactivatingGrade() {
        return schoolController.reactivateGrade(SCHOOL_ID, GRADE_ID, null);
    }

    private ResponseEntity<GetSchoolOverviewResponse> whenGettingMySchoolOverview() {
        return schoolController.getMySchoolOverview(authentication);
    }

    private Map<String, String> whenHandlingSchoolNotFound(SchoolNotFoundException exception) {
        return schoolController.handleSchoolNotFound(exception);
    }

    private void thenSchoolResponseContainsActiveSchools(
            ResponseEntity<List<SchoolResponseDTO>> response
    ) {
        SchoolResponseDTO school = firstOkBody(response);
        assertEquals(SCHOOL_ID, school.id());
        assertEquals("North School", school.nombre());
    }

    private void thenGradeResponseContainsActiveGrades(
            ResponseEntity<List<GradeResponseDTO>> response
    ) {
        GradeResponseDTO grade = firstOkBody(response);
        assertEquals(GRADE_ID, grade.id());
        assertEquals(LEVEL_ID, grade.nivelId());
        assertEquals("1 A", grade.nombre());
    }

    private void thenGradeResponseContainsInactiveGrades(
            ResponseEntity<List<GradeResponseDTO>> response
    ) {
        GradeResponseDTO grade = firstOkBody(response);
        assertEquals(GRADE_ID, grade.id());
        assertEquals(false, grade.activo());
    }

    private void thenGradeResponseIsOk(ResponseEntity<GradeResponseDTO> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(GRADE_ID, response.getBody().id());
    }

    private void thenGradeResponseIsCreated(ResponseEntity<GradeResponseDTO> response) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(GRADE_ID, response.getBody().id());
    }

    private void thenReactivatedGradeResponseIsOk(ResponseEntity<GradeResponseDTO> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().activo());
    }

    private void thenResponseIsNoContent(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    private void thenSchoolOverviewIsReturned(ResponseEntity<GetSchoolOverviewResponse> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(SCHOOL_ID, response.getBody().id());
        assertEquals("Colegio Test", response.getBody().nombre());
    }

    private void thenErrorMessageIsReturned(Map<String, String> response) {
        assertEquals("School not found: " + SCHOOL_ID, response.get("error"));
    }

    private void thenDelegatesToGetActiveSchools() {
        verify(getActiveSchools).execute(new GetActiveSchoolsRequest());
    }

    private void thenDelegatesToGetGradesBySchool() {
        verify(getGradesBySchool).execute(new GetGradesBySchoolRequest(SCHOOL_ID));
    }

    private void thenDelegatesToListSchoolGradesIncludingInactive() {
        verify(listSchoolGrades).execute(new ListSchoolGradesRequest(SCHOOL_ID, true));
    }

    private void thenDelegatesToGetGrade() {
        verify(getGrade).execute(new GetGradeRequest(SCHOOL_ID, GRADE_ID));
    }

    private void thenDelegatesToCreateGrade() {
        verify(createGrade).execute(new CreateGradeRequest(SCHOOL_ID, LEVEL_ID, "1", "A"));
    }

    private void thenDelegatesToUpdateGrade() {
        verify(updateGrade).execute(new UpdateGradeRequest(SCHOOL_ID, GRADE_ID, LEVEL_ID, "2", "B"));
    }

    private void thenDelegatesToDeactivateGrade() {
        verify(deactivateGrade).execute(new DeactivateGradeRequest(SCHOOL_ID, GRADE_ID));
    }

    private void thenDelegatesToReactivateGrade() {
        verify(reactivateGrade).execute(new ReactivateGradeRequest(SCHOOL_ID, GRADE_ID));
    }

    private void thenSchoolManagementAccessIsRequired() {
        verify(schoolAccessGuard).requireSchoolManagementAccess(null, SCHOOL_ID);
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
