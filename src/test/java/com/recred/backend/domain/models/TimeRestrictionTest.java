package com.recred.backend.domain.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TimeRestrictionTest {

    private TimeBadge franjaRecreo;
    private LocalDateTime momentoCompra;
    private Product product;

    @BeforeEach
    void setUp() {
        franjaRecreo = new TimeBadge(UUID.randomUUID(), UUID.randomUUID(), "Recreo",
                LocalTime.of(10, 0), LocalTime.of(10, 30), true);

        momentoCompra = LocalDateTime.of(2026, 5, 25, 10, 15);

        product = new Product();
        ProductCategory catKiosco = new ProductCategory();
        catKiosco.setId(UUID.randomUUID());
        product.setCategory(catKiosco);

        TypeClassification tipoGolosina = new TypeClassification();
        tipoGolosina.setId(UUID.randomUUID());
        product.setTypes(Set.of(tipoGolosina));
    }

    @Test
    void noBlocks_SiRestriccionEstaInactiva() {
        TimeRestriction restriccion = new TimeRestriction(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo, null, null);
        restriccion.setActive(false);

        assertFalse(restriccion.blocks(product, momentoCompra));
    }

    @Test
    void noBlocks_SiEstaFueraDeLaFranjaHoraria() {
        TimeRestriction restriccion = new TimeRestriction(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo, null, null);
        LocalDateTime momentoFueraDeHora = LocalDateTime.of(2026, 5, 25, 11, 0);

        assertFalse(restriccion.blocks(product, momentoFueraDeHora));
    }

    @Test
    void blocks_Totalmente_SiNoHayFiltrosConfigurados() {
        TimeRestriction restriccion = new TimeRestriction(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo, null, null);

        assertTrue(restriccion.blocks(product, momentoCompra));
    }

    @Test
    void blocks_SiCoincideLaCategoria() {
        TimeRestriction restriccion = new TimeRestriction(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo, product.getCategory(), null);

        assertTrue(restriccion.blocks(product, momentoCompra));
    }

    @Test
    void blocks_SiCoincideLaClasificacion() {
        TypeClassification clasificacionRestringida = product.getTypes().iterator().next();
        TimeRestriction restriccion = new TimeRestriction(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo, null, clasificacionRestringida);

        assertTrue(restriccion.blocks(product, momentoCompra));
    }

    @Test
    void noBlocks_SiFiltrosNoCoincidenConElProducto() {
        ProductCategory otraCategoria = new ProductCategory();
        otraCategoria.setId(UUID.randomUUID());

        TimeRestriction restriccion = new TimeRestriction(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo, otraCategoria, null);

        assertFalse(restriccion.blocks(product, momentoCompra));
    }
}