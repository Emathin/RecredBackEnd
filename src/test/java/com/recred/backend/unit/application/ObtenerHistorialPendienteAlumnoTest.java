package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerHistorialPendienteAlumnoTest {

    @Test
    void executeShouldReturnPendingPurchasesFromRepository() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ObtenerHistorialPendienteAlumno useCase =
                new ObtenerHistorialPendienteAlumno(purchaseRepository);
        UUID alumnoId = UUID.randomUUID();
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setTotalAmount(new BigDecimal("1200.00"));
        
        when(purchaseRepository.findPendientesByAlumno(alumnoId))
                .thenReturn(List.of(purchase));

        List<ObtenerHistorialPendienteAlumnoResponse> result = useCase.execute(
                new ObtenerHistorialPendienteAlumnoRequest(alumnoId)
        );

        assertEquals(1, result.size());
        assertEquals(purchase.getId(), result.get(0).id());
        assertEquals(purchase.getTotalAmount(), result.get(0).totalAmount());
    }
}
