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
    private JsonNode json;

    @Test
    void givenSupplierResponseWhenSerializingThenKeepsPreviousJsonFieldNames()
            throws Exception {
        json = whenSerializing(givenSupplierResponse());

        thenHasSpanishSupplierFields();
        thenDoesNotHaveEnglishSupplierFields();
        thenHasSpanishPriceListFields();
        thenDoesNotHaveEnglishPriceListFields();
        thenHasSpanishItemFields();
        thenDoesNotHaveEnglishItemFields();
    }

    @Test
    void givenRecommendationResponseWhenSerializingThenKeepsPreviousJsonFieldNames()
            throws Exception {
        json = whenSerializing(givenRecommendationResponse());

        thenHasSpanishRecommendationFields();
        thenDoesNotHaveEnglishRecommendationFields();
        thenHasSpanishAlternativeFields();
        thenDoesNotHaveEnglishAlternativeFields();
    }

    private SupplierResponse givenSupplierResponse() {
        return new SupplierResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                "Proveedor", "111", "supplier@test.com",
                "lunes", "nota", null, null,
                List.of(givenPriceListResponse()));
    }

    private SupplierRecommendationResponse givenRecommendationResponse() {
        return new SupplierRecommendationResponse(
                UUID.randomUUID(), "Yogur", UUID.randomUUID(),
                "Proveedor", new BigDecimal("10.00"), "unidad",
                new BigDecimal("10.00"),
                List.of(givenAlternativeResponse()));
    }

    private ListaPrecioProveedorResponse givenPriceListResponse() {
        return new ListaPrecioProveedorResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                "https://files.test/list.csv", "list.csv",
                true, null, List.of(givenItemResponse()));
    }

    private ItemListaPrecioProveedorResponse givenItemResponse() {
        return new ItemListaPrecioProveedorResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                "Yogur proveedor", UUID.randomUUID(), "Yogur",
                new BigDecimal("10.00"), "unidad", "nota", true);
    }

    private SupplierRecommendationAlternativeResponse givenAlternativeResponse() {
        return new SupplierRecommendationAlternativeResponse(
                UUID.randomUUID(), "Proveedor alternativo",
                new BigDecimal("12.00"), "unidad",
                new BigDecimal("12.00"));
    }

    private JsonNode whenSerializing(Object value) throws Exception {
        return objectMapper.readTree(
                objectMapper.writeValueAsString(value));
    }

    private void thenHasSpanishSupplierFields() {
        assertTrue(json.has("kiosqueroId"));
        assertTrue(json.has("nombre"));
        assertTrue(json.has("telefono"));
        assertTrue(json.has("diasVisita"));
        assertTrue(json.has("listasPrecios"));
    }

    private void thenDoesNotHaveEnglishSupplierFields() {
        assertFalse(json.has("kioskOwnerId"));
        assertFalse(json.has("name"));
        assertFalse(json.has("phone"));
        assertFalse(json.has("visitDays"));
        assertFalse(json.has("priceLists"));
    }

    private void thenHasSpanishPriceListFields() {
        JsonNode priceList = json.get("listasPrecios").get(0);
        assertTrue(priceList.has("proveedorId"));
        assertTrue(priceList.has("urlArchivo"));
        assertTrue(priceList.has("nombreOriginal"));
        assertTrue(priceList.has("activa"));
    }

    private void thenDoesNotHaveEnglishPriceListFields() {
        JsonNode priceList = json.get("listasPrecios").get(0);
        assertFalse(priceList.has("supplierId"));
        assertFalse(priceList.has("fileUrl"));
        assertFalse(priceList.has("originalFilename"));
        assertFalse(priceList.has("active"));
    }

    private void thenHasSpanishItemFields() {
        JsonNode item = json.get("listasPrecios").get(0)
                .get("items").get(0);
        assertTrue(item.has("listaPrecioId"));
        assertTrue(item.has("nombreProductoProveedor"));
        assertTrue(item.has("productoInventarioId"));
        assertTrue(item.has("nombreProductoInventario"));
        assertTrue(item.has("mappingConfirmado"));
    }

    private void thenDoesNotHaveEnglishItemFields() {
        JsonNode item = json.get("listasPrecios").get(0)
                .get("items").get(0);
        assertFalse(item.has("priceListId"));
        assertFalse(item.has("supplierProductName"));
        assertFalse(item.has("inventoryProductId"));
        assertFalse(item.has("inventoryProductName"));
        assertFalse(item.has("mappingConfirmed"));
    }

    private void thenHasSpanishRecommendationFields() {
        assertTrue(json.has("productoInventarioId"));
        assertTrue(json.has("nombreProducto"));
        assertTrue(json.has("proveedorRecomendadoId"));
        assertTrue(json.has("nombreProveedorRecomendado"));
        assertTrue(json.has("mejorPrecio"));
        assertTrue(json.has("mejorPrecioUnitario"));
        assertTrue(json.has("alternativas"));
    }

    private void thenDoesNotHaveEnglishRecommendationFields() {
        assertFalse(json.has("inventoryProductId"));
        assertFalse(json.has("productName"));
        assertFalse(json.has("recommendedSupplierId"));
        assertFalse(json.has("recommendedSupplierName"));
        assertFalse(json.has("bestPrice"));
        assertFalse(json.has("bestUnitPrice"));
    }

    private void thenHasSpanishAlternativeFields() {
        JsonNode alternative = json.get("alternativas").get(0);
        assertTrue(alternative.has("proveedorId"));
        assertTrue(alternative.has("nombreProveedor"));
        assertTrue(alternative.has("precio"));
        assertTrue(alternative.has("precioUnitario"));
    }

    private void thenDoesNotHaveEnglishAlternativeFields() {
        JsonNode alternative = json.get("alternativas").get(0);
        assertFalse(alternative.has("supplierId"));
        assertFalse(alternative.has("supplierName"));
        assertFalse(alternative.has("price"));
        assertFalse(alternative.has("unitPrice"));
    }
}
