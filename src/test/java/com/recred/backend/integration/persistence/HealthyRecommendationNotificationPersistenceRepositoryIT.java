package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.HealthyProductRecommendation;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.models.HealthyRecommendationPattern;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.HealthyRecommendationNotificationPersistenceRepository;
import com.recred.backend.infrastructure.persistence.SugerenciaIAJpaRepository;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.recred.backend.domain.models.RolUsuario.ALUMNO;
import static com.recred.backend.domain.models.TipoSugerenciaIA.RECOMENDACION_SALUDABLE;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@RecordApplicationEvents
class HealthyRecommendationNotificationPersistenceRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private HealthyRecommendationNotificationPersistenceRepository repository;

    @Autowired
    private SugerenciaIAJpaRepository suggestionJpaRepository;

    @Autowired
    private EntityManager entityManager;
    private ColegioEntity cachedSchool;
    private UsuarioEntity cachedStudentUser;
    private AlumnoEntity cachedStudent;
    private ProductoEntity cachedProduct;
    private ProductoEntity cachedRepeatedProduct;

    @Test
    void save_ShouldPersistRecommendationAndPublishEvent(ApplicationEvents events) {
        HealthyProductRecommendation recommendation = givenRecommendationDomain();

        whenSavingRecommendation(recommendation);

        thenRecommendationIsPersistedAndEventPublished(events, recommendation.candidate().studentId());
    }

    @Test
    void hasRecommendationSince_ShouldReturnTrueWhenSuggestionExists() {
        AlumnoEntity student = givenPersistedRecommendationSuggestion();

        boolean exists = whenCheckingRecommendationExistence(student.getId());

        thenRecommendationExists(exists);
    }

    private HealthyProductRecommendation givenRecommendationDomain() {
        AlumnoEntity student = generateStudentEntity();
        ProductoEntity repeatedProduct = generateRepeatedProductEntity();
        ProductoEntity suggestedProduct = generateProductEntity();
        return createRecommendationDomain(
                student.getId(),
                student.getUsuario().getId(),
                repeatedProduct.getId(),
                suggestedProduct.getId()
        );
    }

    private AlumnoEntity givenPersistedRecommendationSuggestion() {
        AlumnoEntity student = generateStudentEntity();
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setAlumnoEntity(student);
        suggestion.setTipo(RECOMENDACION_SALUDABLE);
        suggestion.setTitulo("Test Title");
        suggestion.setMensaje("Test Message");
        entityManager.persist(suggestion);
        entityManager.flush();
        entityManager.clear();
        return student;
    }

    private void whenSavingRecommendation(HealthyProductRecommendation recommendation) {
        repository.save(recommendation);
        entityManager.flush();
        entityManager.clear();
    }

    private boolean whenCheckingRecommendationExistence(UUID studentId) {
        return repository.hasRecommendationSince(studentId, LocalDateTime.now().minusDays(1).truncatedTo(MILLIS));
    }

    private void thenRecommendationIsPersistedAndEventPublished(ApplicationEvents events, UUID studentId) {
        long pushEventsCount = events.stream(PushNotificationRequestedEvent.class).count();
        assertEquals(1, pushEventsCount, "Should publish a push notification event");

        boolean exists = suggestionJpaRepository.existsForStudentAndTypeSince(
                studentId,
                RECOMENDACION_SALUDABLE,
                LocalDateTime.now().minusDays(1).truncatedTo(MILLIS)
        );
        assertTrue(exists, "Suggestion should be persisted in DB");
    }

    private void thenRecommendationExists(boolean exists) {
        assertTrue(exists);
    }

    private ColegioEntity generateSchoolEntity() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("Healthy School");
            school.setCue("CUE-HEALTHY");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private UsuarioEntity generateStudentUserEntity() {
        if (this.cachedStudentUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-student-healthy");
            user.setNombre("Student Name");
            user.setApellido("Student Surname");
            user.setEmail("student.healthy@test.com");
            user.setRol(ALUMNO);
            user.setActivo(true);
            entityManager.persist(user);
            this.cachedStudentUser = user;
        }
        return this.cachedStudentUser;
    }

    private AlumnoEntity generateStudentEntity() {
        if (this.cachedStudent == null) {
            AlumnoEntity student = new AlumnoEntity();
            student.setUsuario(generateStudentUserEntity());
            entityManager.persist(student);
            this.cachedStudent = student;
        }
        return this.cachedStudent;
    }

    private com.recred.backend.infrastructure.entities.CategoriaProductoEntity generateCategoryEntity() {
        com.recred.backend.infrastructure.entities.CategoriaProductoEntity cat =
                new com.recred.backend.infrastructure.entities.CategoriaProductoEntity();
        cat.setDescripcion("Bebidas-" + UUID.randomUUID().toString());
        cat.setActivo(true);
        entityManager.persist(cat);
        return cat;
    }

    private ProductoEntity generateProductEntity() {
        if (this.cachedProduct == null) {
            ProductoEntity product = new ProductoEntity();
            product.setCategoria(generateCategoryEntity());
            product.setNombre("Agua Mineral");
            product.setDescripcion("Agua");
            product.setPrecio(BigDecimal.TEN);
            product.setRequierePreparacion(false);
            product.setActivo(true);
            entityManager.persist(product);
            this.cachedProduct = product;
        }
        return this.cachedProduct;
    }

    private ProductoEntity generateRepeatedProductEntity() {
        if (this.cachedRepeatedProduct == null) {
            ProductoEntity product = new ProductoEntity();
            product.setCategoria(generateCategoryEntity());
            product.setNombre("Gaseosa");
            product.setDescripcion("Gas");
            product.setPrecio(BigDecimal.TEN);
            product.setRequierePreparacion(false);
            product.setActivo(true);
            entityManager.persist(product);
            this.cachedRepeatedProduct = product;
        }
        return this.cachedRepeatedProduct;
    }

    private HealthyProductRecommendation createRecommendationDomain(
            UUID studentId,
            UUID studentUserId,
            UUID repeatedProductId,
            UUID suggestedProductId
    ) {
        return new HealthyProductRecommendation(
                new ConsumptionAlertCandidate(studentId, studentUserId, "Student First", List.of()),
                new HealthyRecommendationPattern(
                        studentId,
                        LocalDate.now(),
                        LocalDate.now().minusDays(2),
                        LocalDate.now().minusDays(1),
                        repeatedProductId,
                        "Gaseosa",
                        "Bebidas",
                        UUID.randomUUID(),
                        2,
                        2L,
                        suggestedProductId,
                        "Agua mineral",
                        "Bebidas"
                ),
                new HealthyRecommendationMessages(
                        "Try water",
                        "You chose soda recently. Try water.",
                        "Choose water.",
                        suggestedProductId,
                        new BigDecimal("0.8500"),
                        "gemini-test"
                )
        );
    }
}
