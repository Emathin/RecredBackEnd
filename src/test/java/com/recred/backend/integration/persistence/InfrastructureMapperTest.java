package com.recred.backend.integration.persistence;

import com.recred.backend.infrastructure.persistence.*;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InfrastructureMapperTest {

    @Test
    void reference_WhenIdIsNull_ShouldReturnNull() {
        EntityManager entityManager = mock(EntityManager.class);
        InfrastructureMapper mapper = new InfrastructureMapper(entityManager);

        assertNull(mapper.reference(TestEntity.class, null));
        verifyNoInteractions(entityManager);
    }

    @Test
    void reference_WhenIdHasValue_ShouldReturnJpaReference() {
        EntityManager entityManager = mock(EntityManager.class);
        InfrastructureMapper mapper = new InfrastructureMapper(entityManager);
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity();
        when(entityManager.getReference(TestEntity.class, id)).thenReturn(entity);

        TestEntity result = mapper.reference(TestEntity.class, id);

        assertSame(entity, result);
        verify(entityManager).getReference(TestEntity.class, id);
    }

    private static class TestEntity {
    }
}
