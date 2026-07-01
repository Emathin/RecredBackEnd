package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.infrastructure.web.ItemListaPrecioProveedorResponse;
import com.recred.backend.infrastructure.web.ListaPrecioProveedorResponse;
import com.recred.backend.infrastructure.web.SupplierRecommendationAlternativeResponse;
import com.recred.backend.infrastructure.web.SupplierRecommendationResponse;
import com.recred.backend.infrastructure.web.SupplierResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierResponseContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void supplierResponsesKeepPreviousJsonFieldNames() throws Exception {
        JsonNode supplier = json(supplierResponse());
        JsonNode priceList = supplier.get("listasPrecios").get(0);
        JsonNode item = priceList.get("items").get(0);

        assertTrue(supplier.has("kiosqueroId"));
        assertTrue(supplier.has("nombre"));
        assertTrue(supplier.has("telefono"));
        assertTrue(supplier.has("diasVisita"));
        assertTrue(supplier.has("listasPrecios"));
        assertFalse(supplier.has("kioskOwnerId"));
        assertFalse(supplier.has("name"));
        assertFalse(supplier.has("phone"));
        assertFalse(supplier.has("visitDays"));
        assertFalse(supplier.has("priceLists"));

        assertTrue(priceList.has("proveedorId"));
        assertTrue(priceList.has("urlArchivo"));
        assertTrue(priceList.has("nombreOriginal"));
        assertTrue(priceList.has("activa"));
        assertFalse(priceList.has("supplierId"));
        assertFalse(priceList.has("fileUrl"));
        assertFalse(priceList.has("originalFilename"));
        assertFalse(priceList.has("active"));

        assertTrue(item.has("listaPrecioId"));
        assertTrue(item.has("nombreProductoProveedor"));
        assertTrue(item.has("productoInventarioId"));
        assertTrue(item.has("nombreProductoInventario"));
        assertTrue(item.has("mappingConfirmado"));
        assertFalse(item.has("priceListId"));
        assertFalse(item.has("supplierProductName"));
        assertFalse(item.has("inventoryProductId"));
        assertFalse(item.has("inventoryProductName"));
        assertFalse(item.has("mappingConfirmed"));
    }

    @Test
    void recommendationResponseKeepsPreviousJsonFieldNames() throws Exception {
        JsonNode recommendation = json(recommendationResponse());
        JsonNode alternative = recommendation.get("alternativas").get(0);

        assertTrue(recommendation.has("productoInventarioId"));
        assertTrue(recommendation.has("nombreProducto"));
        assertTrue(recommendation.has("proveedorRecomendadoId"));
        assertTrue(recommendation.has("nombreProveedorRecomendado"));
        assertTrue(recommendation.has("mejorPrecio"));
        assertTrue(recommendation.has("mejorPrecioUnitario"));
        assertTrue(recommendation.has("alternativas"));
        assertFalse(recommendation.has("inventoryProductId"));
        assertFalse(recommendation.has("productName"));
        assertFalse(recommendation.has("recommendedSupplierId"));
        assertFalse(recommendation.has("recommendedSupplierName"));
        assertFalse(recommendation.has("bestPrice"));
        assertFalse(recommendation.has("bestUnitPrice"));

        assertTrue(alternative.has("proveedorId"));
        assertTrue(alternative.has("nombreProveedor"));
        assertTrue(alternative.has("precio"));
        assertTrue(alternative.has("precioUnitario"));
        assertFalse(alternative.has("supplierId"));
        assertFalse(alternative.has("supplierName"));
        assertFalse(alternative.has("price"));
        assertFalse(alternative.has("unitPrice"));
    }

    private SupplierResponse supplierResponse() {
        return new SupplierResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Proveedor",
                "111",
                "supplier@test.com",
                "lunes",
                "nota",
                null,
                null,
                List.of(priceListResponse())
        );
    }

    private ListaPrecioProveedorResponse priceListResponse() {
        return new ListaPrecioProveedorResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "https://files.test/list.csv",
                "list.csv",
                true,
                null,
                List.of(itemResponse())
        );
    }

    private ItemListaPrecioProveedorResponse itemResponse() {
        return new ItemListaPrecioProveedorResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Yogur proveedor",
                UUID.randomUUID(),
                "Yogur",
                new BigDecimal("10.00"),
                "unidad",
                "nota",
                true
        );
    }

    private SupplierRecommendationResponse recommendationResponse() {
        return new SupplierRecommendationResponse(
                UUID.randomUUID(),
                "Yogur",
                UUID.randomUUID(),
                "Proveedor",
                new BigDecimal("10.00"),
                "unidad",
                new BigDecimal("10.00"),
                List.of(alternativeResponse())
        );
    }

    private SupplierRecommendationAlternativeResponse alternativeResponse() {
        return new SupplierRecommendationAlternativeResponse(
                UUID.randomUUID(),
                "Proveedor alternativo",
                new BigDecimal("12.00"),
                "unidad",
                new BigDecimal("12.00")
        );
    }

    private JsonNode json(Object value) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(value));
    }
}
