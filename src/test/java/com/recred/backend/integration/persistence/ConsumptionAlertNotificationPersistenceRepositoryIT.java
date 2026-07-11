package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.ConsumptionAlert;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import com.recred.backend.domain.models.ConsumptionAlertPattern;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
import com.recred.backend.domain.models.ConsumptionAlertType;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
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

import static com.recred.backend.domain.models.AIEventType.ANALISIS_CONSUMO;
import static com.recred.backend.domain.models.ConsumptionAlertType.HEALTHY_RECOMMENDATION;
import static com.recred.backend.domain.models.ConsumptionAlertType.STATISTICAL_PATTERN;
import static com.recred.backend.domain.models.RolUsuario.ALUMNO;
import static com.recred.backend.domain.models.RolUsuario.PADRE;
import static com.recred.backend.domain.models.TipoPlan.AVANZADO;
import static com.recred.backend.domain.models.TipoSugerenciaIA.RECOMENDACION_SALUDABLE;
import static com.recred.backend.domain.models.TipoSugerenciaIA.RESUMEN_CONSUMO;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ConsumptionAlertNotificationPersistenceRepositoryIT extends AbstractIntegrationTest {

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
        AlumnoEntity student = givenStudentEntity();
        UsuarioEntity tutorUser = givenTutorUserEntity();
        ProductoEntity product = givenProductEntity();
        ConsumptionAlert alert = givenPatternAlert(student.getId(), student.getUsuario().getId(), tutorUser.getId(), product.getId());

        whenSavingAlert(alert);

        thenAlertEventAndNotificationsAreSaved(student, tutorUser, product);
    }

    @Test
    void save_ShouldSaveHealthyRecommendationWithoutProductReference() {
        AlumnoEntity student = givenStudentEntity();
        ConsumptionAlert alert = givenHealthyAlert(student.getId(), student.getUsuario().getId());

        whenSavingAlert(alert);

        thenHealthyRecommendationIsSavedWithoutProduct();
    }

    @Test
    void hasAlerts_ShouldReturnTrueWhenAlertsExistInDatabase() {
        AlumnoEntity student = givenStudentEntity();
        LocalDateTime start = LocalDateTime.now().minusDays(1).truncatedTo(MILLIS);
        LocalDateTime end = LocalDateTime.now().plusDays(1).truncatedTo(MILLIS);
        givenPersistedSuggestion(student);

        boolean hasAny = whenCheckingIfHasAnyAlertBetween(student.getId(), start, end);
        boolean hasPattern = whenCheckingIfHasAlertSince(student.getId(), STATISTICAL_PATTERN, start);

        thenFlagsReturnTrue(hasAny, hasPattern);
    }

    private AlumnoEntity givenStudentEntity() {
        return generateStudentEntity();
    }

    private UsuarioEntity givenTutorUserEntity() {
        return generateTutorUserEntity();
    }

    private ProductoEntity givenProductEntity() {
        return generateProductEntity();
    }

    private ConsumptionAlert givenPatternAlert(UUID studentId, UUID studentUserId, UUID tutorUserId, UUID productId) {
        return alert(studentId, studentUserId, tutorUserId, productId);
    }

    private ConsumptionAlert givenHealthyAlert(UUID studentId, UUID studentUserId) {
        return healthyAlert(studentId, studentUserId);
    }

    private void givenPersistedSuggestion(AlumnoEntity student) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setAlumnoEntity(student);
        suggestion.setTipo(RESUMEN_CONSUMO);
        suggestion.setTitulo("Test Title");
        suggestion.setMensaje("Test Message");
        entityManager.persist(suggestion);
        entityManager.flush();
        entityManager.clear();
    }

    private void whenSavingAlert(ConsumptionAlert alert) {
        repository.save(alert);
        entityManager.flush();
        entityManager.clear();
    }

    private boolean whenCheckingIfHasAnyAlertBetween(UUID studentId, LocalDateTime start, LocalDateTime end) {
        return repository.hasAnyAlertBetween(studentId, start, end);
    }

    private boolean whenCheckingIfHasAlertSince(UUID studentId, ConsumptionAlertType type, LocalDateTime start) {
        return repository.hasAlertSince(studentId, type, start);
    }

    private void thenAlertEventAndNotificationsAreSaved(AlumnoEntity student, UsuarioEntity tutorUser, ProductoEntity product) {
        List<AIEventEntity> events = aiEventJpaRepository.findAll();
        assertFalse(events.isEmpty());
        AIEventEntity savedEvent = events.get(0);
        assertEquals(ANALISIS_CONSUMO, savedEvent.getType());
        assertEquals(student.getId(), savedEvent.getAlumnoEntity().getId());

        List<SugerenciaIAEntity> suggestions = suggestionJpaRepository.findAll();
        assertFalse(suggestions.isEmpty());
        SugerenciaIAEntity savedSuggestion = suggestions.get(0);
        assertEquals(RESUMEN_CONSUMO, savedSuggestion.getTipo());
        assertEquals(student.getId(), savedSuggestion.getAlumnoEntity().getId());
        assertEquals(product.getId(), savedSuggestion.getProductoEntity().getId());

        List<NotificacionEntity> notifications = notificationJpaRepository.findAll();
        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().anyMatch(n -> n.getUsuarioEntity().getId().equals(student.getUsuario().getId())));
        assertTrue(notifications.stream().anyMatch(n -> n.getUsuarioEntity().getId().equals(tutorUser.getId())));
    }

    private void thenHealthyRecommendationIsSavedWithoutProduct() {
        List<SugerenciaIAEntity> suggestions = suggestionJpaRepository.findAll();
        assertFalse(suggestions.isEmpty());
        SugerenciaIAEntity savedSuggestion = suggestions.get(0);
        assertEquals(RECOMENDACION_SALUDABLE, savedSuggestion.getTipo());
        assertNull(savedSuggestion.getProductoEntity());
    }

    private void thenFlagsReturnTrue(boolean hasAny, boolean hasPattern) {
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
            user.setRol(ALUMNO);
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
            user.setRol(PADRE);
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
                        List.of(new ConsumptionAlertTutor(tutorUserId, "Tutor Doe",
                                AVANZADO))
                ),
                pattern(studentId, productId, ConsumptionAlertType.STATISTICAL_PATTERN),
                messages()
        );
    }

    private ConsumptionAlert healthyAlert(UUID studentId, UUID studentUserId) {
        return new ConsumptionAlert(
                new ConsumptionAlertCandidate(studentId, studentUserId, "John Doe", List.of()),
                pattern(studentId, null, HEALTHY_RECOMMENDATION),
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
