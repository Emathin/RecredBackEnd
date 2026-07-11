package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.ProductAdditionNotificationRepositoryImpl;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@RecordApplicationEvents
class ProductAdditionNotificationRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private ProductAdditionNotificationRepositoryImpl repository;

    @Autowired
    private NotificacionJpaRepository notificationJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationEvents applicationEvents;

    private UsuarioEntity cachedKiosqueroUser;
    private ProductoEntity cachedProduct;
    private SugerenciaIAEntity cachedSuggestion;

    @Test
    void save_ShouldPersistNotificationAndPublishPushEvent() {
        givenProductAdditionNotificationContext();

        whenSavingNotification();

        thenNotificationIsPersistedAndPushEventPublished();
    }

    private void givenProductAdditionNotificationContext() {
        this.cachedKiosqueroUser = generateUserEntity();
        this.cachedProduct = generateProductEntity();
        this.cachedSuggestion = generateSuggestionEntity(this.cachedProduct);
    }

    private void whenSavingNotification() {
        repository.save(
                createDomainProduct(this.cachedProduct),
                createDomainKiosquero(this.cachedKiosqueroUser),
                createDomainSuggestion(this.cachedSuggestion)
        );
        entityManager.flush();
        entityManager.clear();
    }

    private void thenNotificationIsPersistedAndPushEventPublished() {
        NotificacionEntity savedNotification = assertNotificationPersisted(this.cachedKiosqueroUser, this.cachedProduct, this.cachedSuggestion);
        assertPushNotificationEventPublished(this.cachedKiosqueroUser, this.cachedProduct, savedNotification);
    }

    private NotificacionEntity assertNotificationPersisted(UsuarioEntity user, ProductoEntity product, SugerenciaIAEntity suggestion) {
        List<NotificacionEntity> savedNotifications = notificationJpaRepository.findAll();
        assertEquals(1, savedNotifications.size());
        NotificacionEntity notification = savedNotifications.get(0);
        
        assertEquals(user.getId(), notification.getUsuarioEntity().getId());
        assertEquals(product.getId(), notification.getProductoEntity().getId());
        assertEquals(suggestion.getId(), notification.getSugerenciaIaEntity().getId());
        assertEquals(TipoNotificacion.AGREGAR_PRODUCTO, notification.getTipo());
        assertEquals("Sugerencia de producto", notification.getTitulo());
        assertFalse(notification.getLeida());
        assertTrue(notification.getMensaje().contains("Chocolatina"));
        return notification;
    }

    private void assertPushNotificationEventPublished(UsuarioEntity user, ProductoEntity product, NotificacionEntity notification) {
        long eventCount = applicationEvents.stream(PushNotificationRequestedEvent.class).count();
        assertEquals(1, eventCount);
        
        PushNotificationRequestedEvent event = applicationEvents.stream(PushNotificationRequestedEvent.class).findFirst().get();
        PushNotification push = event.notification();
        
        assertEquals(user.getId(), push.userId());
        assertEquals(notification.getId().toString(), push.data().get("notificacionId"));
        assertEquals(product.getId().toString(), push.data().get("productoId"));
        assertEquals(TipoNotificacion.AGREGAR_PRODUCTO.name(), push.data().get("tipo"));
    }

    private UsuarioEntity generateUserEntity() {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-kiosk-prod-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Kiosquero");
        user.setApellido("Notif");
        user.setEmail("kiosk.notif@test.com");
        user.setRol(RolUsuario.VENDEDOR);
        user.setActivo(true);
        entityManager.persist(user);
        return user;
    }

    private ProductoEntity generateProductEntity() {
        CategoriaProductoEntity cat = new CategoriaProductoEntity();
        cat.setDescripcion("Cat");
        cat.setActivo(true);
        entityManager.persist(cat);

        ProductoEntity p = new ProductoEntity();
        p.setCategoria(cat);
        p.setNombre("Chocolatina");
        p.setDescripcion("Rica");
        p.setPrecio(new BigDecimal("100"));
        p.setActivo(true);
        p.setRequierePreparacion(false);
        entityManager.persist(p);
        return p;
    }

    private SugerenciaIAEntity generateSuggestionEntity(ProductoEntity p) {
        SugerenciaIAEntity sug = new SugerenciaIAEntity();
        sug.setProductoEntity(p);
        sug.setTipo(TipoSugerenciaIA.AGREGAR_PRODUCTO);
        sug.setTitulo("Sugerencia");
        sug.setMensaje("Saludable");
        sug.setAceptada(true);
        entityManager.persist(sug);
        return sug;
    }

    private Producto createDomainProduct(ProductoEntity entity) {
        Producto domainProduct = new Producto();
        domainProduct.setId(entity.getId());
        domainProduct.setNombre(entity.getNombre());
        return domainProduct;
    }

    private Kiosquero createDomainKiosquero(UsuarioEntity entity) {
        Usuario domainUser = new Usuario();
        domainUser.setId(entity.getId());
        Kiosquero domainKiosquero = new Kiosquero();
        domainKiosquero.setUsuario(domainUser);
        return domainKiosquero;
    }

    private SugerenciaIA createDomainSuggestion(SugerenciaIAEntity entity) {
        SugerenciaIA domainSuggestion = new SugerenciaIA();
        domainSuggestion.setId(entity.getId());
        return domainSuggestion;
    }
}
