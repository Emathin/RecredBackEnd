package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.SummaryEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataSummaryRepository;
import com.recred.backend.infrastructure.persistence.SummaryRepositoryImpl;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class SummaryRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SummaryRepositoryImpl repository;

    @Autowired
    private SpringDataSummaryRepository springDataSummaryRepository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity cachedUser;
    private SummaryEntity cachedNewSummary;

    @Test
    void findLastByUsuarioIdShouldMapEntity() {
        givenUserWithMultipleSummaries();

        Optional<Summary> result = whenFindingLastSummaryByUserId(this.cachedUser.getId());

        thenSummaryMatches(result, this.cachedNewSummary.getId(), "{\"resumen\": \"New Summary\"}");
    }

    @Test
    void findAllByUsuarioIdShouldMapEntities() {
        givenUserWithMultipleSummaries();

        List<Summary> result = whenFindingAllSummariesByUserId(this.cachedUser.getId());

        thenSummaryListHasSize(result, 2);
    }

    @Test
    void saveShouldMapSummaryAndAssignGeneratedId() {
        UsuarioEntity userEntity = givenUser();
        Summary summaryCommand = createSummaryDomain(userEntity.getId());

        Summary result = whenSavingSummary(summaryCommand);

        thenSummaryWasSavedCorrectly(result.getId(), summaryCommand, userEntity.getId());
    }

    private void givenUserWithMultipleSummaries() {
        this.cachedUser = generateUser();
        generateSummaryEntity(this.cachedUser, LocalDateTime.of(2026, 5, 1, 0, 0), LocalDateTime.of(2026, 5, 15, 23, 59), "{\"resumen\": \"Old Summary\"}");
        this.cachedNewSummary = generateSummaryEntity(this.cachedUser, LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 15, 23, 59), "{\"resumen\": \"New Summary\"}");
        entityManager.flush();
        entityManager.clear();
    }

    private UsuarioEntity givenUser() {
        UsuarioEntity userEntity = generateUser();
        entityManager.flush();
        entityManager.clear();
        return userEntity;
    }

    private Optional<Summary> whenFindingLastSummaryByUserId(UUID userId) {
        return repository.findLastByUsuarioId(userId);
    }

    private List<Summary> whenFindingAllSummariesByUserId(UUID userId) {
        return repository.findAllByUsuarioId(userId);
    }

    private Summary whenSavingSummary(Summary summary) {
        Summary result = repository.save(summary);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private void thenSummaryMatches(Optional<Summary> result, UUID expectedId, String expectedResumen) {
        assertTrue(result.isPresent());
        assertEquals(expectedId, result.get().getId());
        assertEquals(expectedResumen, result.get().getResumen());
    }

    private void thenSummaryListHasSize(List<Summary> result, int expectedSize) {
        assertEquals(expectedSize, result.size());
    }

    private void thenSummaryWasSavedCorrectly(UUID savedId, Summary originalCommand, UUID expectedUserId) {
        assertNotNull(savedId);
        Optional<SummaryEntity> savedOpt = springDataSummaryRepository.findById(savedId);
        assertTrue(savedOpt.isPresent());
        SummaryEntity saved = savedOpt.get();
        assertEquals(originalCommand.getFechaDesde(), saved.getStartDate());
        assertEquals(originalCommand.getFechaHasta(), saved.getEndDate());
        assertEquals(originalCommand.getResumen().replace(" ", ""), saved.getSummary().replace(" ", ""));
        assertEquals(expectedUserId, saved.getUser().getId());
    }

    private UsuarioEntity generateUser() {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Test");
        user.setApellido("User");
        user.setEmail("user" + UUID.randomUUID().toString().substring(0, 5) + "@test.com");
        user.setRol(RolUsuario.PADRE);
        user.setActivo(true);
        entityManager.persist(user);
        return user;
    }

    private SummaryEntity generateSummaryEntity(UsuarioEntity user, LocalDateTime startDate, LocalDateTime endDate, String jsonSummary) {
        SummaryEntity summary = new SummaryEntity();
        summary.setUser(user);
        summary.setStartDate(startDate);
        summary.setEndDate(endDate);
        summary.setSummary(jsonSummary);
        return springDataSummaryRepository.save(summary);
    }

    private Summary createSummaryDomain(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        
        Summary summary = new Summary();
        summary.setFechaDesde(LocalDateTime.of(2026, 6, 1, 0, 0));
        summary.setFechaHasta(LocalDateTime.of(2026, 6, 15, 23, 59));
        summary.setResumen("{\"mensaje\":\"ok\"}");
        summary.setUsuario(user);
        return summary;
    }
}
