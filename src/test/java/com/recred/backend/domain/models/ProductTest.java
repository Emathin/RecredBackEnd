package com.recred.backend.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void addMedida_DeberiaAgregarMeasureYVincularProducto() {
        Product product = new Product();
        ProductMeasure medida = new ProductMeasure();

        product.addMeasure(medida);

        assertTrue(product.getMeasures().contains(medida));
        assertEquals(product, medida.getProduct());
    }

    @Test
    void addMedida_NoDeberiaHacerNada_CuandoMeasureEsNula() {
        Product product = new Product();
        product.addMeasure(null);
        assertTrue(product.getMeasures().isEmpty());
    }

    @Test
    void removeMedida_DeberiaQuitarMeasureYDesvincularProducto() {
        Product product = new Product();
        ProductMeasure medida = new ProductMeasure();
        product.addMeasure(medida);

        product.removeMeasure(medida);

        assertFalse(product.getMeasures().contains(medida));
        assertNull(medida.getProduct());
    }

    @Test
    void removeMedida_NoDeberiaHacerNada_CuandoMeasureEsNula() {
        Product product = new Product();
        product.removeMeasure(null);
        assertTrue(product.getMeasures().isEmpty());
    }

    @Test
    void getClasificaciones_DeberiaRetornarSetDeHealthClassifications() {
        Product product = new Product();
        Set<HealthClassification> clasificaciones = new HashSet<>();
        HealthClassification cs = new HealthClassification();
        clasificaciones.add(cs);
        product.setHealthClassifications(clasificaciones);

        assertEquals(clasificaciones, product.getHealthClassifications());
    }

    @Test
    void testGettersAndSetters() {
        Product product = new Product();
        UUID id = UUID.randomUUID();
        ProductCategory cat = new ProductCategory();
        BigDecimal precio = new BigDecimal("100.00");
        BigDecimal peso = new BigDecimal("1.0");

        product.setId(id);
        product.setName("Test");
        product.setDescription("Desc");
        product.setPrice(precio);
        product.setWeight(peso);
        product.setRequiresPreparation(true);
        product.setActive(false);
        product.setCurrentStock(10);

        assertEquals(id, product.getId());
        assertEquals("Test", product.getName());
        assertEquals("Desc", product.getDescription());
        assertEquals(precio, product.getPrice());
        assertEquals(peso, product.getWeight());
        assertTrue(product.getRequiresPreparation());
        assertFalse(product.getActive());
        assertEquals(10, product.getCurrentStock());
    }
}
