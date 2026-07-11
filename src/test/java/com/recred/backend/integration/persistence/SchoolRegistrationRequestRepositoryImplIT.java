package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.infrastructure.entities.SchoolRegistrationRequestEntity;
import com.recred.backend.infrastructure.persistence.SchoolRegistrationRequestRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataSchoolRegistrationRequestRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.recred.backend.domain.models.SchoolRegistrationStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class SchoolRegistrationRequestRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SpringDataSchoolRegistrationRequestRepository springDataRepository;

    @Autowired
    private SchoolRegistrationRequestRepositoryImpl repository;

    @Test
    void save_ShouldPersistNewRequest() {
        SchoolRegistrationRequest request = givenNewSchoolRegistrationRequest();

        whenSavingRequest(request);

        thenRequestIsPersisted(request.getSchoolName());
    }

    @Test
    void findById_ShouldReturnRequest_WhenExists() {
        SchoolRegistrationRequestEntity entity = givenExistingPendingRequest();

        Optional<SchoolRegistrationRequest> result = whenFindingRequestById(entity.getId());

        thenRequestIsFound(result, entity.getId());
    }

    @Test
    void findByStatus_ShouldReturnRequestsWithGivenStatus() {
        givenRequestsWithMixedStatuses();

        List<SchoolRegistrationRequest> pendingRequests = whenFindingRequestsByStatus(PENDING);

        thenOnlyRequestsWithStatusAreReturned(pendingRequests, PENDING);
    }

    @Test
    void existsBySchoolEmailAndStatus_ShouldReturnTrue_WhenMatches() {
        givenRequestWithSpecificEmailAndStatus("match@school.com", PENDING);

        boolean existsMatch = whenCheckingExistsBySchoolEmailAndStatus("match@school.com", PENDING);
        boolean existsNoMatch = whenCheckingExistsBySchoolEmailAndStatus("match@school.com", APPROVED);

        thenExistsResultIs(existsMatch, true);
        thenExistsResultIs(existsNoMatch, false);
    }

    @Test
    void existsBySchoolCueAndStatus_ShouldReturnTrue_WhenMatches() {
        givenRequestWithSpecificCueAndStatus("MATCH_CUE", PENDING);

        boolean existsMatch = whenCheckingExistsBySchoolCueAndStatus("MATCH_CUE", PENDING);

        thenExistsResultIs(existsMatch, true);
    }

    @Test
    void existsByDirectorEmailAndStatus_ShouldReturnTrue_WhenMatches() {
        givenRequestWithSpecificDirectorEmailAndStatus("match@director.com", PENDING);

        boolean existsMatch = whenCheckingExistsByDirectorEmailAndStatus("match@director.com", PENDING);

        thenExistsResultIs(existsMatch, true);
    }

    private SchoolRegistrationRequest givenNewSchoolRegistrationRequest() {
        SchoolRegistrationRequest request = new SchoolRegistrationRequest();
        request.setSchoolName("Test School");
        request.setSchoolEmail("test@school.com");
        request.setSchoolCue("CUE123");
        request.setDirectorFirstName("John");
        request.setDirectorLastName("Doe");
        request.setDirectorEmail("director@school.com");
        request.setStatus(PENDING);
        return request;
    }

    private SchoolRegistrationRequestEntity givenExistingPendingRequest() {
        return generateRequest("school@test.com", "CUE1", "director@test.com", PENDING);
    }

    private void givenRequestsWithMixedStatuses() {
        generateRequest("school1@test.com", "CUE1", "director1@test.com", PENDING);
        generateRequest("school2@test.com", "CUE2", "director2@test.com", APPROVED);
    }

    private void givenRequestWithSpecificEmailAndStatus(String email, SchoolRegistrationStatus status) {
        generateRequest(email, "CUE_RND", "director_rnd@test.com", status);
    }

    private void givenRequestWithSpecificCueAndStatus(String cue, SchoolRegistrationStatus status) {
        generateRequest("school_rnd@test.com", cue, "director_rnd@test.com", status);
    }

    private void givenRequestWithSpecificDirectorEmailAndStatus(String directorEmail, SchoolRegistrationStatus status) {
        generateRequest("school_rnd@test.com", "CUE_RND", directorEmail, status);
    }

    private void whenSavingRequest(SchoolRegistrationRequest request) {
        repository.save(request);
    }

    private Optional<SchoolRegistrationRequest> whenFindingRequestById(java.util.UUID id) {
        return repository.findById(id);
    }

    private List<SchoolRegistrationRequest> whenFindingRequestsByStatus(SchoolRegistrationStatus status) {
        return repository.findByStatus(status);
    }

    private boolean whenCheckingExistsBySchoolEmailAndStatus(String email, SchoolRegistrationStatus status) {
        return repository.existsBySchoolEmailAndStatus(email, status);
    }

    private boolean whenCheckingExistsBySchoolCueAndStatus(String cue, SchoolRegistrationStatus status) {
        return repository.existsBySchoolCueAndStatus(cue, status);
    }

    private boolean whenCheckingExistsByDirectorEmailAndStatus(String directorEmail, SchoolRegistrationStatus status) {
        return repository.existsByDirectorEmailAndStatus(directorEmail, status);
    }

    private void thenRequestIsPersisted(String expectedSchoolName) {
        List<SchoolRegistrationRequestEntity> all = springDataRepository.findAll();
        assertFalse(all.isEmpty());
        assertEquals(expectedSchoolName, all.get(0).getSchoolName());
    }

    private void thenRequestIsFound(Optional<SchoolRegistrationRequest> result, java.util.UUID expectedId) {
        assertTrue(result.isPresent());
        assertEquals(expectedId, result.get().getId());
    }

    private void thenOnlyRequestsWithStatusAreReturned(List<SchoolRegistrationRequest> requests, SchoolRegistrationStatus expectedStatus) {
        assertFalse(requests.isEmpty());
        assertTrue(requests.stream().allMatch(r -> r.getStatus() == expectedStatus));
    }

    private void thenExistsResultIs(boolean actual, boolean expected) {
        assertEquals(expected, actual);
    }

    private SchoolRegistrationRequestEntity generateRequest(String schoolEmail, String cue, String directorEmail, SchoolRegistrationStatus status) {
        SchoolRegistrationRequestEntity entity = new SchoolRegistrationRequestEntity();
        entity.setSchoolName("Test School Name");
        entity.setSchoolEmail(schoolEmail);
        entity.setSchoolCue(cue);
        entity.setDirectorFirstName("Director Name");
        entity.setDirectorLastName("Director Last Name");
        entity.setDirectorEmail(directorEmail);
        entity.setStatus(status);
        return springDataRepository.saveAndFlush(entity);
    }
}
