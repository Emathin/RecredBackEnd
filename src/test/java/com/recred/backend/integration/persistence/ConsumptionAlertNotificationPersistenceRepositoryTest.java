package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.ConsumptionAlert;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import com.recred.backend.domain.models.ConsumptionAlertPattern;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
import com.recred.backend.domain.models.ConsumptionAlertType;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.AIEventJpaRepository;
import com.recred.backend.infrastructure.persistence.ConsumptionAlertNotificationPersistenceRepository;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.SugerenciaIAJpaRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ConsumptionAlertNotificationPersistenceRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ConsumptionAlertNotificationPersistenceRepository repository;

    @Autowired
    private SugerenciaIAJpaRepository suggestionJpaRepository;

    @Autowired
    private NotificacionJpaRepository notificationJpaRepository;

    @Autowired
    private AIEventJpaRepository aiEventJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity cachedStudentUser;
    private AlumnoEntity cachedStudent;
    private UsuarioEntity cachedTutorUser;
    private ProductoEntity cachedProduct;
    private CategoriaProductoEntity cachedCategory;

    @Test
    void save_ShouldSavePatternAlertAsEventSuggestionAndNotifications() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity tutorUser = generateTutorUserEntity();
        ProductoEntity product = generateProductEntity();

        repository.save(alert(student.getId(), student.getUsuario().getId(), tutorUser.getId(), product.getId()));
        entityManager.flush();
        entityManager.clear();

        List<AIEventEntity> events = aiEventJpaRepository.findAll();
        assertFalse(events.isEmpty());
        AIEventEntity savedEvent = events.get(0);
        assertEquals(AIEventType.ANALISIS_CONSUMO, savedEvent.getType());
        assertEquals(student.getId(), savedEvent.getAlumnoEntity().getId());

        List<SugerenciaIAEntity> suggestions = suggestionJpaRepository.findAll();
        assertFalse(suggestions.isEmpty());
        SugerenciaIAEntity savedSuggestion = suggestions.get(0);
        assertEquals(TipoSugerenciaIA.RESUMEN_CONSUMO, savedSuggestion.getTipo());
        assertEquals(student.getId(), savedSuggestion.getAlumnoEntity().getId());
        assertEquals(product.getId(), savedSuggestion.getProductoEntity().getId());

        List<NotificacionEntity> notifications = notificationJpaRepository.findAll();
        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().anyMatch(n -> n.getUsuarioEntity().getId().equals(student.getUsuario().getId())));
        assertTrue(notifications.stream().anyMatch(n -> n.getUsuarioEntity().getId().equals(tutorUser.getId())));
    }

    @Test
    void save_ShouldSaveHealthyRecommendationWithoutProductReference() {
        AlumnoEntity student = generateStudentEntity();

        repository.save(healthyAlert(student.getId(), student.getUsuario().getId()));
        entityManager.flush();
        entityManager.clear();

        List<SugerenciaIAEntity> suggestions = suggestionJpaRepository.findAll();
        assertFalse(suggestions.isEmpty());
        SugerenciaIAEntity savedSuggestion = suggestions.get(0);
        assertEquals(TipoSugerenciaIA.RECOMENDACION_SALUDABLE, savedSuggestion.getTipo());
        assertNull(savedSuggestion.getProductoEntity());
    }

    @Test
    void hasAlerts_ShouldReturnTrueWhenAlertsExistInDatabase() {
        AlumnoEntity student = generateStudentEntity();
        LocalDateTime start = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime end = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MILLIS);

        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setAlumnoEntity(student);
        suggestion.setTipo(TipoSugerenciaIA.RESUMEN_CONSUMO);
        suggestion.setTitulo("Test Title");
        suggestion.setMensaje("Test Message");
        entityManager.persist(suggestion);
        entityManager.flush();
        entityManager.clear();

        boolean hasAny = repository.hasAnyAlertBetween(student.getId(), start, end);
        boolean hasPattern = repository.hasAlertSince(student.getId(), ConsumptionAlertType.STATISTICAL_PATTERN, start);

        assertTrue(hasAny);
        assertTrue(hasPattern);
    }

    private UsuarioEntity generateStudentUserEntity() {
        if (this.cachedStudentUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("cognito-student-user");
            user.setNombre("John");
            user.setApellido("Doe");
            user.setEmail("john.doe@test.com");
            user.setRol(RolUsuario.ALUMNO);
            user.setAutenticadoConCognito(false);
            user.setActivo(true);
            entityManager.persist(user);
            entityManager.flush();
            this.cachedStudentUser = user;
        }
        return this.cachedStudentUser;
    }

    private AlumnoEntity generateStudentEntity() {
        if (this.cachedStudent == null) {
            UsuarioEntity user = generateStudentUserEntity();
            AlumnoEntity student = new AlumnoEntity();
            student.setUsuario(user);
            student.setActivo(true);
            entityManager.persist(student);
            entityManager.flush();
            this.cachedStudent = student;
        }
        return this.cachedStudent;
    }

    private UsuarioEntity generateTutorUserEntity() {
        if (this.cachedTutorUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("cognito-tutor-user");
            user.setNombre("Jane");
            user.setApellido("Doe");
            user.setEmail("jane.doe@test.com");
            user.setRol(RolUsuario.PADRE);
            user.setAutenticadoConCognito(false);
            user.setActivo(true);
            entityManager.persist(user);
            entityManager.flush();
            this.cachedTutorUser = user;
        }
        return this.cachedTutorUser;
    }

    private CategoriaProductoEntity generateCategoryEntity() {
        if (this.cachedCategory == null) {
            CategoriaProductoEntity category = new CategoriaProductoEntity();
            category.setDescripcion("Beverages");
            category.setActivo(true);
            entityManager.persist(category);
            entityManager.flush();
            this.cachedCategory = category;
        }
        return this.cachedCategory;
    }

    private ProductoEntity generateProductEntity() {
        if (this.cachedProduct == null) {
            ProductoEntity product = new ProductoEntity();
            product.setNombre("Water");
            product.setDescripcion("Mineral Water");
            product.setPrecio(new BigDecimal("100.00"));
            product.setCategoria(generateCategoryEntity());
            product.setRequierePreparacion(false);
            entityManager.persist(product);
            entityManager.flush();
            this.cachedProduct = product;
        }
        return this.cachedProduct;
    }

    private ConsumptionAlert alert(UUID studentId, UUID studentUserId, UUID tutorUserId, UUID productId) {
        return new ConsumptionAlert(
                new ConsumptionAlertCandidate(
                        studentId,
                        studentUserId,
                        "John Doe",
                        List.of(new ConsumptionAlertTutor(tutorUserId, "Tutor Doe"))
                ),
                pattern(studentId, productId, ConsumptionAlertType.STATISTICAL_PATTERN),
                messages()
        );
    }

    private ConsumptionAlert healthyAlert(UUID studentId, UUID studentUserId) {
        return new ConsumptionAlert(
                new ConsumptionAlertCandidate(studentId, studentUserId, "John Doe", List.of()),
                pattern(studentId, null, ConsumptionAlertType.HEALTHY_RECOMMENDATION),
                messages()
        );
    }

    private ConsumptionAlertPattern pattern(UUID studentId, UUID productId, ConsumptionAlertType type) {
        return new ConsumptionAlertPattern(
                type,
                studentId,
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 4, 26),
                LocalDate.of(2026, 5, 25),
                4,
                new BigDecimal("100.00"),
                "Beverages",
                new BigDecimal("80.00"),
                productId,
                productId == null ? null : "Water"
        );
    }

    private ConsumptionAlertMessages messages() {
        return new ConsumptionAlertMessages(
                "Choose water",
                "Suggestion for John",
                "We recommend drinking more water.",
                "We recommend suggesting John to drink more water.",
                "Drink more water.",
                new BigDecimal("0.8000"),
                "gemini-test"
        );
    }
}
