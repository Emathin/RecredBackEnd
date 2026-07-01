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
        SchoolRegistrationRequest request = new SchoolRegistrationRequest();
        request.setSchoolName("Test School");
        request.setSchoolEmail("test@school.com");
        request.setSchoolCue("CUE123");
        request.setDirectorFirstName("John");
        request.setDirectorLastName("Doe");
        request.setDirectorEmail("director@school.com");
        request.setStatus(SchoolRegistrationStatus.PENDING);

        repository.save(request);

        List<SchoolRegistrationRequestEntity> all = springDataRepository.findAll();
        assertFalse(all.isEmpty());
        assertEquals("Test School", all.get(0).getSchoolName());
    }

    @Test
    void findById_ShouldReturnRequest_WhenExists() {
        SchoolRegistrationRequestEntity entity = generateRequest("school@test.com", "CUE1", "director@test.com", SchoolRegistrationStatus.PENDING);

        Optional<SchoolRegistrationRequest> result = repository.findById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
    }

    @Test
    void findByStatus_ShouldReturnRequestsWithGivenStatus() {
        generateRequest("school1@test.com", "CUE1", "director1@test.com", SchoolRegistrationStatus.PENDING);
        generateRequest("school2@test.com", "CUE2", "director2@test.com", SchoolRegistrationStatus.APPROVED);

        List<SchoolRegistrationRequest> pendingRequests = repository.findByStatus(SchoolRegistrationStatus.PENDING);

        assertFalse(pendingRequests.isEmpty());
        assertTrue(pendingRequests.stream().allMatch(r -> r.getStatus() == SchoolRegistrationStatus.PENDING));
    }

    @Test
    void existsBySchoolEmailAndStatus_ShouldReturnTrue_WhenMatches() {
        generateRequest("match@school.com", "CUE1", "director1@test.com", SchoolRegistrationStatus.PENDING);

        assertTrue(repository.existsBySchoolEmailAndStatus("match@school.com", SchoolRegistrationStatus.PENDING));
        assertFalse(repository.existsBySchoolEmailAndStatus("match@school.com", SchoolRegistrationStatus.APPROVED));
    }

    @Test
    void existsBySchoolCueAndStatus_ShouldReturnTrue_WhenMatches() {
        generateRequest("school1@test.com", "MATCH_CUE", "director1@test.com", SchoolRegistrationStatus.PENDING);

        assertTrue(repository.existsBySchoolCueAndStatus("MATCH_CUE", SchoolRegistrationStatus.PENDING));
    }

    @Test
    void existsByDirectorEmailAndStatus_ShouldReturnTrue_WhenMatches() {
        generateRequest("school1@test.com", "CUE1", "match@director.com", SchoolRegistrationStatus.PENDING);

        assertTrue(repository.existsByDirectorEmailAndStatus("match@director.com", SchoolRegistrationStatus.PENDING));
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
