package com.recred.backend.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class InfrastructureMapper {

    private final EntityManager entityManager;

    public InfrastructureMapper(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public <T> T reference(Class<T> entityClass, Object id) {
        if (id == null) {
            return null;
        }
        return entityManager.getReference(entityClass, id);
    }
}
