package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;

public interface ProductAdditionNotificationRepository {
    void save(Producto product, Kiosquero kiosquero);
}
