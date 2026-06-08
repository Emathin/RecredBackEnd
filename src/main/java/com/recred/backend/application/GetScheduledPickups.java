package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class GetScheduledPickups {

    private final PurchaseRepository purchaseRepository;

    public GetScheduledPickups(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<Purchase> execute(
            UUID buffetId,
            LocalDate fecha,
            EstadoRetiro estadoRetiro,
            UUID franjaId,
            String search
    ) {
        return purchaseRepository.findScheduledPickups(buffetId, fecha, estadoRetiro, franjaId, search);
    }
}
