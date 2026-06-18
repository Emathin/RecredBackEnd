package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.SugerenciaIA;

public interface ProductAdditionNotificationRepository {
    void save(Producto product, Kiosquero kiosquero, SugerenciaIA suggestion);
}
