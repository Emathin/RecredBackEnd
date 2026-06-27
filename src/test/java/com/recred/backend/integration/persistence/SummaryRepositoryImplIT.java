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

    @Test
    void findLastByUsuarioIdShouldMapEntity() {
        UsuarioEntity user = generateUser();
        generateSummaryEntity(user, LocalDateTime.of(2026, 5, 1, 0, 0), LocalDateTime.of(2026, 5, 15, 23, 59), "{\"resumen\": \"Old Summary\"}");
        SummaryEntity newSummary = generateSummaryEntity(user, LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 15, 23, 59), "{\"resumen\": \"New Summary\"}");
        entityManager.flush();

        Optional<Summary> result = repository.findLastByUsuarioId(user.getId());

        assertTrue(result.isPresent());
        assertEquals(newSummary.getId(), result.get().getId());
        assertEquals("{\"resumen\": \"New Summary\"}", result.get().getResumen());
    }

    @Test
    void findAllByUsuarioIdShouldMapEntities() {
        UsuarioEntity user = generateUser();
        generateSummaryEntity(user, LocalDateTime.of(2026, 5, 1, 0, 0), LocalDateTime.of(2026, 5, 15, 23, 59), "{\"resumen\": \"Summary 1\"}");
        generateSummaryEntity(user, LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 15, 23, 59), "{\"resumen\": \"Summary 2\"}");
        entityManager.flush();

        List<Summary> result = repository.findAllByUsuarioId(user.getId());

        assertEquals(2, result.size());
    }

    @Test
    void saveShouldMapSummaryAndAssignGeneratedId() {
        UsuarioEntity userEntity = generateUser();
        Summary summaryCommand = createSummaryDomain(userEntity.getId());

        Summary result = repository.save(summaryCommand);
        entityManager.flush();
        entityManager.clear();

        assertSummaryWasSavedCorrectly(result.getId(), summaryCommand, userEntity.getId());
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

    private void assertSummaryWasSavedCorrectly(UUID savedId, Summary originalCommand, UUID expectedUserId) {
        assertNotNull(savedId);
        Optional<SummaryEntity> savedOpt = springDataSummaryRepository.findById(savedId);
        assertTrue(savedOpt.isPresent());
        SummaryEntity saved = savedOpt.get();
        assertEquals(originalCommand.getFechaDesde(), saved.getStartDate());
        assertEquals(originalCommand.getFechaHasta(), saved.getEndDate());
        assertEquals(originalCommand.getResumen().replace(" ", ""), saved.getSummary().replace(" ", ""));
        assertEquals(expectedUserId, saved.getUser().getId());
    }
}
