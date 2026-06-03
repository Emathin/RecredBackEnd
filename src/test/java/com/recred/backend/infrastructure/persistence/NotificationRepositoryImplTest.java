package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRepositoryImplTest {

    @Test
    void findByUserIdMapsEntitiesToDomainAndUsesPagination() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificacionEntity entity = notificationEntity(notificationId);
        NotificacionJpaRepository jpaRepository = mock(NotificacionJpaRepository.class);
        when(jpaRepository.findByUserId(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(List.of(entity));
        NotificationRepositoryImpl repository = new NotificationRepositoryImpl(jpaRepository);

        List<Notification> result = repository.findByUserId(userId, true, 1, 5);

        assertEquals(1, result.size());
        assertEquals(notificationId, result.get(0).id());
        assertEquals(TipoNotificacion.SISTEMA, result.get(0).type());
        assertEquals("Title", result.get(0).title());
        assertEquals("Message", result.get(0).message());
        assertEquals(false, result.get(0).read());
        assertEquals(LocalDateTime.of(2026, 6, 2, 14, 30), result.get(0).createdAt());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findByUserId(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(true),
                pageableCaptor.capture()
        );
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
    }

    private NotificacionEntity notificationEntity(UUID notificationId) {
        NotificacionEntity entity = new NotificacionEntity();
        entity.setId(notificationId);
        entity.setTipo(TipoNotificacion.SISTEMA);
        entity.setTitulo("Title");
        entity.setMensaje("Message");
        entity.setLeida(false);
        entity.setCreadoEn(LocalDateTime.of(2026, 6, 2, 14, 30));
        return entity;
    }
}
