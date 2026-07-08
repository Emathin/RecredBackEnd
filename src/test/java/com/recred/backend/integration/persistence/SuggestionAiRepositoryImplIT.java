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

    @Test
    void save_ShouldMapRecommendationToSuggestionEntityAndPersist() {
        AlumnoEntity student = generateStudent();
        ProductoEntity product = generateProduct();
        AIEventEntity event = generateAIEvent(student);
        PurchaseRecommendationResult result = createPurchaseRecommendation(student, product);

        SugerenciaIAEntity saved = repository.save(event.getId(), result);
        entityManager.flush();
        entityManager.clear();

        assertSuggestionWasSavedAndMappedCorrectly(saved.getId(), event.getId(), student.getId(), product.getId());
    }

    @Test
    void findByTipo_ShouldReturnMatchingNotifications() {
        AlumnoEntity student = generateStudent();
        SugerenciaIAEntity suggestion = generateSuggestion(student);
        UsuarioEntity user = generateUser(RolUsuario.PADRE);
        NotificacionEntity notification = generateNotification(user, suggestion, null);
        entityManager.flush();

        List<NotificacionEntity> result = repository.findByTipo("MONDAY");

        assertEquals(1, result.size());
        assertEquals(notification.getId(), result.get(0).getId());
    }

    @Test
    void findByTipoAndStudentId_ShouldReturnMatchingNotification() {
        AlumnoEntity student = generateStudent();
        SugerenciaIAEntity suggestion = generateSuggestion(student);
        UsuarioEntity user = generateUser(RolUsuario.PADRE);
        NotificacionEntity notification = generateNotification(user, suggestion, student);
        entityManager.flush();

        NotificacionEntity result = repository.findByTipoAndStudentId("MONDAY", student.getId());

        assertNotNull(result);
        assertEquals(notification.getId(), result.getId());
    }

    @Test
    void getSuggestionByNotificationId_ShouldReturnMappedDomainObject() {
        AlumnoEntity student = generateStudent();
        SugerenciaIAEntity suggestion = generateSuggestion(student);
        UsuarioEntity user = generateUser(RolUsuario.PADRE);
        NotificacionEntity notification = generateNotification(user, suggestion, null);
        entityManager.flush();

        SugerenciaIA result = repository.getSuggestionByNotificationId(notification.getId());

        assertNotNull(result);
        assertEquals(suggestion.getId(), result.getId());
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

    private SugerenciaIAEntity generateSuggestion(AlumnoEntity student) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setAlumnoEntity(student);
        suggestion.setTipo(TipoSugerenciaIA.MONDAY);
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

    private void assertSuggestionWasSavedAndMappedCorrectly(UUID savedId, UUID eventId, UUID studentId, UUID productId) {
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
}
