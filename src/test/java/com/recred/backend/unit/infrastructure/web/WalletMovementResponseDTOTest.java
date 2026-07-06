package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;
import com.recred.backend.infrastructure.web.WalletMovementResponseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletMovementResponseDTOTest {

    @Test
    void fromDomainShouldMapWalletMovement() {
        UUID movementId = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.of(2026, 7, 1, 10, 30);
        WalletMovement movement = new WalletMovement(
                movementId,
                dateTime,
                TipoMovimientoBilletera.COMPRA,
                "Compra en buffet",
                new BigDecimal("350.00"),
                WalletMovementDirection.SALIDA
        );

        WalletMovementResponseDTO response = WalletMovementResponseDTO.fromDomain(movement);

        assertEquals(movementId, response.id());
        assertEquals(dateTime, response.fechaHora());
        assertEquals("COMPRA", response.tipo());
        assertEquals("Compra en buffet", response.descripcion());
        assertEquals(new BigDecimal("350.00"), response.monto());
        assertEquals(WalletMovementDirection.SALIDA, response.direccion());
    }

    @Test
    void fromApplicationShouldMapWalletSummaryMovement() {
        UUID movementId = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.of(2026, 7, 2, 11, 0);
        GetStudentWalletSummaryResponse.Movement movement =
                new GetStudentWalletSummaryResponse.Movement(
                        movementId,
                        dateTime,
                        "CARGA",
                        "Carga de saldo",
                        new BigDecimal("1000.00"),
                        WalletMovementDirection.ENTRADA
                );

        WalletMovementResponseDTO response = WalletMovementResponseDTO.fromApplication(movement);

        assertEquals(movementId, response.id());
        assertEquals(dateTime, response.fechaHora());
        assertEquals("CARGA", response.tipo());
        assertEquals("Carga de saldo", response.descripcion());
        assertEquals(new BigDecimal("1000.00"), response.monto());
        assertEquals(WalletMovementDirection.ENTRADA, response.direccion());
    }
}
