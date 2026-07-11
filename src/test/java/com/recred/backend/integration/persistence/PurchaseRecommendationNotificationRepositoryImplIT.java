package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.PurchaseRecommendationNotificationRepositoryImpl;
import com.recred.backend.infrastructure.push.PushNotificationRequestedEvent;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Transactional
@RecordApplicationEvents
class PurchaseRecommendationNotificationRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private PurchaseRecommendationNotificationRepositoryImpl repository;

    @Autowired
    private NotificacionJpaRepository notificationJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationEvents applicationEvents;

    private UsuarioEntity cachedUser;
    private AlumnoEntity cachedStudent;
    private ProductoEntity cachedProduct;
    private SugerenciaIAEntity cachedSuggestion;

    @Test
    void saveShouldPersistRecommendationNotificationAndPublishPushEvent() {
        PurchaseRecommendationResult recommendation = givenPurchaseRecommendationContext();

        whenSavingRecommendation(recommendation);

        thenNotificationIsPersistedAndPushEventPublished();
    }

    private PurchaseRecommendationResult givenPurchaseRecommendationContext() {
        this.cachedUser = generateUserEntity();
        this.cachedStudent = generateStudentEntity(this.cachedUser);
        this.cachedProduct = generateProductEntity();
        this.cachedSuggestion = generateSuggestionEntity(this.cachedProduct, this.cachedStudent);

        return createRecommendation(this.cachedStudent.getId(), this.cachedProduct.getId());
    }

    private void whenSavingRecommendation(PurchaseRecommendationResult recommendation) {
        repository.save(recommendation, this.cachedSuggestion, this.cachedUser.getId());
        entityManager.flush();
        entityManager.clear();
    }

    private void thenNotificationIsPersistedAndPushEventPublished() {
        NotificacionEntity savedNotification = assertNotificationPersisted(this.cachedUser, this.cachedStudent, this.cachedProduct, this.cachedSuggestion);
        assertPushNotificationEventPublished(this.cachedUser, this.cachedStudent, this.cachedProduct, this.cachedSuggestion, savedNotification);
    }

    private NotificacionEntity assertNotificationPersisted(UsuarioEntity user, AlumnoEntity student, ProductoEntity product, SugerenciaIAEntity suggestion) {
        List<NotificacionEntity> saved = notificationJpaRepository.findAll();
        assertEquals(1, saved.size());
        NotificacionEntity notification = saved.get(0);

        assertEquals(user.getId(), notification.getUsuarioEntity().getId());
        assertEquals(product.getId(), notification.getProductoEntity().getId());
        assertEquals(student.getId(), notification.getAlumnoEntity().getId());
        assertEquals(suggestion.getId(), notification.getSugerenciaIaEntity().getId());
        assertEquals(TipoNotificacion.SUGERENCIA_IA, notification.getTipo());
        assertEquals("Compra tu Yogurt", notification.getTitulo());
        assertEquals("Frequent purchase.", notification.getMensaje());
        assertFalse(notification.getLeida());
        return notification;
    }

    private void assertPushNotificationEventPublished(UsuarioEntity user, AlumnoEntity student, ProductoEntity product, SugerenciaIAEntity suggestion, NotificacionEntity notification) {
        long eventCount = applicationEvents.stream(PushNotificationRequestedEvent.class).count();
        assertEquals(1, eventCount);
        
        PushNotificationRequestedEvent event = applicationEvents.stream(PushNotificationRequestedEvent.class).findFirst().get();
        PushNotification push = event.notification();
        
        assertEquals(user.getId(), push.userId());
        assertEquals(notification.getId().toString(), push.data().get("notificacionId"));
        assertEquals(student.getId().toString(), push.data().get("alumnoId"));
        assertEquals(product.getId().toString(), push.data().get("productoId"));
        assertEquals(suggestion.getId().toString(), push.data().get("sugerenciaIaId"));
    }

    private UsuarioEntity generateUserEntity() {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-parent-rec-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Parent");
        user.setApellido("Rec");
        user.setEmail("parent.rec@test.com");
        user.setRol(RolUsuario.PADRE);
        user.setActivo(true);
        entityManager.persist(user);
        return user;
    }

    private AlumnoEntity generateStudentEntity(UsuarioEntity parent) {
        UsuarioEntity studentUser = new UsuarioEntity();
        studentUser.setIdCognito("sub-student-rec-" + UUID.randomUUID().toString().substring(0, 8));
        studentUser.setNombre("Student");
        studentUser.setApellido("Rec");
        studentUser.setEmail("student.rec@test.com");
        studentUser.setRol(RolUsuario.ALUMNO);
        studentUser.setActivo(true);
        entityManager.persist(studentUser);

        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(studentUser);
        student.setActivo(true);
        entityManager.persist(student);
        return student;
    }

    private ProductoEntity generateProductEntity() {
        CategoriaProductoEntity cat = new CategoriaProductoEntity();
        cat.setDescripcion("Dairy");
        cat.setActivo(true);
        entityManager.persist(cat);

        ProductoEntity product = new ProductoEntity();
        product.setCategoria(cat);
        product.setNombre("Yogurt");
        product.setDescripcion("Strawberry Yogurt");
        product.setPrecio(new BigDecimal("150"));
        product.setActivo(true);
        product.setRequierePreparacion(false);
        entityManager.persist(product);
        return product;
    }

    private SugerenciaIAEntity generateSuggestionEntity(ProductoEntity product, AlumnoEntity student) {
        SugerenciaIAEntity sug = new SugerenciaIAEntity();
        sug.setProductoEntity(product);
        sug.setAlumnoEntity(student);
        sug.setTipo(com.recred.backend.domain.models.TipoSugerenciaIA.SUGERENCIA_COMPRA);
        sug.setTitulo("Recomendacion");
        sug.setMensaje("Porque le gusta el dulce");
        sug.setAceptada(false);
        entityManager.persist(sug);
        return sug;
    }

    private PurchaseRecommendationResult createRecommendation(UUID studentId, UUID productId) {
        return new PurchaseRecommendationResult(
                studentId,
                List.of(),
                Optional.of(new PurchasePatternAnalysis(
                        "Frequent purchase.",
                        productId,
                        "Yogurt",
                        "Monday"
                ))
        );
    }
}
