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

    private static final UUID MOVEMENT_ID = UUID.randomUUID();
    private static final LocalDateTime PURCHASE_DATE =
            LocalDateTime.of(2026, 7, 1, 10, 30);
    private static final LocalDateTime LOAD_DATE =
            LocalDateTime.of(2026, 7, 2, 11, 0);

    private WalletMovementResponseDTO result;

    @Test
    void givenDomainModelWhenMappingFromDomainThenReturnsMappedWalletMovement() {
        WalletMovement movement = givenDomainMovement();

        result = whenMappingFromDomain(movement);

        thenIdIs(MOVEMENT_ID);
        thenFechaHoraIs(PURCHASE_DATE);
        thenTipoIs("COMPRA");
        thenDescripcionIs("Compra en buffet");
        thenMontoIs(new BigDecimal("350.00"));
        thenDireccionIs(WalletMovementDirection.SALIDA);
    }

    @Test
    void givenApplicationModelWhenMappingFromApplicationThenReturnsMappedMovement() {
        GetStudentWalletSummaryResponse.Movement movement =
                givenApplicationMovement();

        result = whenMappingFromApplication(movement);

        thenIdIs(MOVEMENT_ID);
        thenFechaHoraIs(LOAD_DATE);
        thenTipoIs("CARGA");
        thenDescripcionIs("Carga de saldo");
        thenMontoIs(new BigDecimal("1000.00"));
        thenDireccionIs(WalletMovementDirection.ENTRADA);
    }

    private WalletMovement givenDomainMovement() {
        return new WalletMovement(
                MOVEMENT_ID,
                PURCHASE_DATE,
                TipoMovimientoBilletera.COMPRA,
                "Compra en buffet",
                new BigDecimal("350.00"),
                WalletMovementDirection.SALIDA
        );
    }

    private GetStudentWalletSummaryResponse.Movement givenApplicationMovement() {
        return new GetStudentWalletSummaryResponse.Movement(
                MOVEMENT_ID,
                LOAD_DATE,
                "CARGA",
                "Carga de saldo",
                new BigDecimal("1000.00"),
                WalletMovementDirection.ENTRADA
        );
    }

    private WalletMovementResponseDTO whenMappingFromDomain(
            WalletMovement movement) {
        return WalletMovementResponseDTO.fromDomain(movement);
    }

    private WalletMovementResponseDTO whenMappingFromApplication(
            GetStudentWalletSummaryResponse.Movement movement) {
        return WalletMovementResponseDTO.fromApplication(movement);
    }

    private void thenIdIs(UUID expected) {
        assertEquals(expected, result.id());
    }

    private void thenFechaHoraIs(LocalDateTime expected) {
        assertEquals(expected, result.fechaHora());
    }

    private void thenTipoIs(String expected) {
        assertEquals(expected, result.tipo());
    }

    private void thenDescripcionIs(String expected) {
        assertEquals(expected, result.descripcion());
    }

    private void thenMontoIs(BigDecimal expected) {
        assertEquals(expected, result.monto());
    }

    private void thenDireccionIs(WalletMovementDirection expected) {
        assertEquals(expected, result.direccion());
    }
}
