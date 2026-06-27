package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.NotificacionJpaRepository;
import com.recred.backend.infrastructure.persistence.NotificationRepositoryImpl;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Transactional
class NotificationRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private NotificationRepositoryImpl repository;

    @Autowired
    private NotificacionJpaRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity cachedUser;

    @Test
    void findByUserIdMapsEntitiesToDomainAndUsesPagination() {
        UsuarioEntity user = generateUserEntity();
        
        generateNotificationEntity(user, "Unread 1", false, LocalDateTime.now().minusDays(1));
        generateNotificationEntity(user, "Read 1", true, LocalDateTime.now().minusDays(2));
        generateNotificationEntity(user, "Unread 2", false, LocalDateTime.now().minusDays(3));
        
        List<Notification> unreadResults = repository.findByUserId(user.getId(), true, 0, 5);

        assertEquals(2, unreadResults.size());
        assertFalse(unreadResults.get(0).read());
        assertEquals("Unread 1", unreadResults.get(0).title());
        
        List<Notification> allResults = repository.findByUserId(user.getId(), false, 0, 5);
        assertEquals(3, allResults.size());
    }

    private UsuarioEntity generateUserEntity() {
        if (this.cachedUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-notif-" + UUID.randomUUID().toString().substring(0, 8));
            user.setNombre("Notif");
            user.setApellido("User");
            user.setEmail("notif.user@test.com");
            user.setRol(RolUsuario.PADRE);
            user.setActivo(true);
            entityManager.persist(user);
            this.cachedUser = user;
        }
        return this.cachedUser;
    }

    private NotificacionEntity generateNotificationEntity(UsuarioEntity user, String title, boolean leida, LocalDateTime date) {
        NotificacionEntity entity = new NotificacionEntity();
        entity.setUsuarioEntity(user);
        entity.setTipo(TipoNotificacion.SISTEMA);
        entity.setTitulo(title);
        entity.setMensaje("Message body for " + title);
        entity.setLeida(leida);
        entity.setCreadoEn(date);
        
        return springDataRepository.save(entity);
    }
}
