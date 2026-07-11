package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.*;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.*;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class SuggestionAiRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SuggestionAiRepositoryImpl repository;

    @Autowired
    private SpringDataSuggestionAiRepository springDataRepository;

    @Autowired
    private NotificacionJpaRepository notificacionRepository;

    @Autowired
    private AIEventJpaRepository aiEventRepository;

    @Autowired
    private EntityManager entityManager;

    private AlumnoEntity cachedStudent;
    private SugerenciaIAEntity cachedSuggestion;
    private NotificacionEntity cachedNotification;

    @Test
    void save_ShouldMapRecommendationToSuggestionEntityAndPersist() {
        SuggestionSaveContext ctx = givenContextForSavingSuggestion();

        SugerenciaIAEntity saved = whenSavingPurchaseRecommendation(ctx.event().getId(), ctx.result());

        thenSuggestionWasSavedAndMappedCorrectly(saved.getId(), ctx.event().getId(), ctx.student().getId(), ctx.product().getId());
    }

    @Test
    void findByTipo_ShouldReturnMatchingNotifications() {
        givenNotificationWithSuggestionType("MONDAY", false);

        List<NotificacionEntity> result = whenFindingNotificationsByType("MONDAY");

        thenNotificationListHasSizeAndMatches(result, 1, this.cachedNotification.getId());
    }

    @Test
    void findByTipoAndStudentId_ShouldReturnMatchingNotification() {
        givenNotificationWithSuggestionType("MONDAY", true);

        NotificacionEntity result = whenFindingNotificationByTypeAndStudentId("MONDAY", this.cachedStudent.getId());

        thenSingleNotificationMatches(result, this.cachedNotification.getId());
    }

    @Test
    void getSuggestionByNotificationId_ShouldReturnMappedDomainObject() {
        givenNotificationWithSuggestionType("MONDAY", false);

        SugerenciaIA result = whenGettingSuggestionByNotificationId(this.cachedNotification.getId());

        thenDomainSuggestionIsMappedCorrectly(result, this.cachedSuggestion.getId());
    }

    private SuggestionSaveContext givenContextForSavingSuggestion() {
        AlumnoEntity student = generateStudent();
        ProductoEntity product = generateProduct();
        AIEventEntity event = generateAIEvent(student);
        PurchaseRecommendationResult result = createPurchaseRecommendation(student, product);
        return new SuggestionSaveContext(student, product, event, result);
    }

    private void givenNotificationWithSuggestionType(String type, boolean withStudent) {
        this.cachedStudent = generateStudent();
        this.cachedSuggestion = generateSuggestion(this.cachedStudent, type);
        UsuarioEntity user = generateUser(RolUsuario.PADRE);
        this.cachedNotification = generateNotification(user, this.cachedSuggestion, withStudent ? this.cachedStudent : null);
        entityManager.flush();
        entityManager.clear();
    }

    private SugerenciaIAEntity whenSavingPurchaseRecommendation(UUID eventId, PurchaseRecommendationResult result) {
        SugerenciaIAEntity saved = repository.save(eventId, result);
        entityManager.flush();
        entityManager.clear();
        return saved;
    }

    private List<NotificacionEntity> whenFindingNotificationsByType(String type) {
        return repository.findByTipo(type);
    }

    private NotificacionEntity whenFindingNotificationByTypeAndStudentId(String type, UUID studentId) {
        return repository.findByTipoAndStudentId(type, studentId);
    }

    private SugerenciaIA whenGettingSuggestionByNotificationId(UUID notificationId) {
        return repository.getSuggestionByNotificationId(notificationId);
    }

    private void thenSuggestionWasSavedAndMappedCorrectly(UUID savedId, UUID eventId, UUID studentId, UUID productId) {
        assertNotNull(savedId);
        SugerenciaIAEntity fetched = springDataRepository.findById(savedId).orElseThrow();
        assertEquals(Boolean.FALSE, fetched.getAceptada());
        assertEquals(TipoSugerenciaIA.MONDAY, fetched.getTipo());
        assertEquals(eventId, fetched.getEventoIaEntity().getId());
        assertEquals(studentId, fetched.getAlumnoEntity().getId());
        assertEquals(productId, fetched.getProductoEntity().getId());
        assertEquals("Compra frecuente", fetched.getTitulo());
        assertTrue(fetched.getMensaje().contains("MONDAY"));
        assertTrue(fetched.getMensaje().contains("Test Product"));
    }

    private void thenNotificationListHasSizeAndMatches(List<NotificacionEntity> result, int expectedSize, UUID expectedNotificationId) {
        assertEquals(expectedSize, result.size());
        assertEquals(expectedNotificationId, result.get(0).getId());
    }

    private void thenSingleNotificationMatches(NotificacionEntity result, UUID expectedNotificationId) {
        assertNotNull(result);
        assertEquals(expectedNotificationId, result.getId());
    }

    private void thenDomainSuggestionIsMappedCorrectly(SugerenciaIA result, UUID expectedSuggestionId) {
        assertNotNull(result);
        assertEquals(expectedSuggestionId, result.getId());
        assertEquals(TipoSugerenciaIA.MONDAY, result.getTipo());
        assertEquals("Test Title", result.getTitulo());
        assertEquals("Test Message", result.getMensaje());
    }

    private AlumnoEntity generateStudent() {
        UsuarioEntity user = generateUser(RolUsuario.ALUMNO);
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(user);
        student.setActivo(true);
        entityManager.persist(student);
        return student;
    }

    private UsuarioEntity generateUser(RolUsuario role) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Test");
        user.setApellido("User");
        user.setEmail("user" + UUID.randomUUID().toString().substring(0, 5) + "@test.com");
        user.setRol(role);
        user.setActivo(true);
        entityManager.persist(user);
        return user;
    }

    private ProductoEntity generateProduct() {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setDescripcion("Snacks");
        category.setActivo(true);
        entityManager.persist(category);

        ProductoEntity product = new ProductoEntity();
        product.setCategoria(category);
        product.setNombre("Test Product");
        product.setDescripcion("Desc");
        product.setPrecio(new BigDecimal("100.00"));
        product.setActivo(true);
        product.setRequierePreparacion(false);
        entityManager.persist(product);
        return product;
    }

    private AIEventEntity generateAIEvent(AlumnoEntity student) {
        AIEventEntity event = new AIEventEntity();
        event.setType(AIEventType.ANALISIS_CONSUMO);
        event.setAlumnoEntity(student);
        return aiEventRepository.save(event);
    }

    private PurchaseRecommendationResult createPurchaseRecommendation(AlumnoEntity student, ProductoEntity product) {
        return new PurchaseRecommendationResult(
                student.getId(),
                List.of(),
                Optional.of(new PurchasePatternAnalysis(
                        "Compra frecuente",
                        product.getId(),
                        "Test Product",
                        "MONDAY"
                ))
        );
    }

    private SugerenciaIAEntity generateSuggestion(AlumnoEntity student, String type) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setAlumnoEntity(student);
        suggestion.setTipo(TipoSugerenciaIA.valueOf(type));
        suggestion.setTitulo("Test Title");
        suggestion.setMensaje("Test Message");
        return springDataRepository.save(suggestion);
    }

    private NotificacionEntity generateNotification(UsuarioEntity user, SugerenciaIAEntity suggestion, AlumnoEntity student) {
        NotificacionEntity notification = new NotificacionEntity();
        notification.setUsuarioEntity(user);
        if (student != null) {
            notification.setAlumnoEntity(student);
        }
        notification.setTipo(TipoNotificacion.SUGERENCIA_IA);
        notification.setSugerenciaIaEntity(suggestion);
        notification.setTitulo("Test Title");
        notification.setMensaje("Test Message");
        return notificacionRepository.save(notification);
    }

    private record SuggestionSaveContext(AlumnoEntity student, ProductoEntity product, AIEventEntity event, PurchaseRecommendationResult result) {}
}
