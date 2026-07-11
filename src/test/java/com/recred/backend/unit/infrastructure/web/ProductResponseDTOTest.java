package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetFavoritesByStudentResponse;
import com.recred.backend.application.ListInventoryProductsResponse;
import com.recred.backend.application.ProductResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductResponseDTOTest {

    @Test
    void fromDomainShouldMapFullProduct() {
        Producto product = givenFullProduct();

        ProductResponseDTO dto = ProductResponseDTO.fromDomain(product);

        thenMapsFullProductCorrectly(product, dto);
    }

    @Test
    void fromDomainShouldHandleNullFields() {
        Producto product = givenProductWithNullFields();

        ProductResponseDTO dto = ProductResponseDTO.fromDomain(product);

        thenMapsNullFieldsCorrectly(dto);
    }

    @Test
    void fromApplicationShouldMapNullCategoryAndClassificationSets() {
        ProductResponse response = givenProductResponseFromApplication();

        ProductResponseDTO dto = ProductResponseDTO.fromApplication(response);

        thenMapsApplicationResponseCorrectly(response, dto);
    }

    @Test
    void fromUseCaseResponseShouldMapFavoriteProductClassifications() {
        GetFavoritesByStudentResponse response = givenGetFavoritesByStudentResponse();

        ProductResponseDTO dto = ProductResponseDTO.fromUseCaseResponse(response);

        thenMapsFavoriteProductClassificationsCorrectly(response, dto);
    }

    @Test
    void fromAppResponseShouldMapCategoryAndClassifications() {
        ProductResponse response = givenProductResponseWithCategoryAndClassifications();

        ProductResponseDTO dto = ProductResponseDTO.fromAppResponse(response);

        thenMapsCategoryAndClassificationsCorrectly(response, dto);
    }

    @Test
    void fromInventoryProductShouldMapNullCategoryAndClassifications() {
        ListInventoryProductsResponse.InventoryProductResponse response = 
                givenInventoryProductResponse();

        ProductResponseDTO dto = ProductResponseDTO.fromInventoryProduct(response);

        thenMapsInventoryProductCorrectly(response, dto);
    }

    private Producto givenFullProduct() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Product");
        product.setPrecio(new BigDecimal("10.00"));
        product.setActivo(true);
        product.setCreadoEn(LocalDateTime.now());
        product.setEstadoInventario(EstadoInventario.DISPONIBLE);
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);

        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Cat");
        product.setCategoria(category);

        ClasificacionSalud salud = new ClasificacionSalud();
        salud.setId(UUID.randomUUID());
        salud.setDescripcion("Salud");
        product.setClasificacionesSalud(Set.of(salud));

        ClasificacionTipo tipo = new ClasificacionTipo();
        tipo.setId(UUID.randomUUID());
        tipo.setDescripcion("Tipo");
        product.setTipos(Set.of(tipo));

        return product;
    }

    private Producto givenProductWithNullFields() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setCategoria(null);
        product.setClasificacionesSalud(null);
        product.setTipos(null);
        product.setEstadoInventario(null);
        return product;
    }

    private ProductResponse givenProductResponseFromApplication() {
        return new ProductResponse(
                UUID.randomUUID(),
                "Product",
                "Desc",
                new BigDecimal("10.00"),
                new BigDecimal("200.00"),
                false,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                10,
                2,
                3,
                8,
                "DISPONIBLE",
                "STOCK_EXACTO",
                20,
                15,
                "https://example.com/product.png",
                null,
                null,
                null
        );
    }

    private GetFavoritesByStudentResponse givenGetFavoritesByStudentResponse() {
        UUID categoryId = UUID.randomUUID();
        UUID healthId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        return new GetFavoritesByStudentResponse(
                UUID.randomUUID(),
                "Barrita",
                "Cereal",
                new BigDecimal("1200.00"),
                new BigDecimal("50.00"),
                false,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                12,
                1,
                3,
                11,
                "DISPONIBLE",
                "STOCK_EXACTO",
                30,
                22,
                "https://example.com/barrita.png",
                new GetFavoritesByStudentResponse.CategoriaResponse(categoryId, "Snacks"),
                Set.of(new GetFavoritesByStudentResponse.ClasificacionResponse(
                        healthId,
                        "Sin TACC")),
                Set.of(new GetFavoritesByStudentResponse.ClasificacionResponse(
                        typeId,
                        "Dulce"))
        );
    }

    private ProductResponse givenProductResponseWithCategoryAndClassifications() {
        return new ProductResponse(
                UUID.randomUUID(),
                "Jugo",
                "Naranja",
                new BigDecimal("900.00"),
                null,
                false,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                20,
                0,
                5,
                20,
                "DISPONIBLE",
                "STOCK_EXACTO",
                null,
                null,
                null,
                new ProductResponse.CategoryResponse(UUID.randomUUID(), "Bebidas"),
                Set.of(new ProductResponse.ClassificationResponse(
                        UUID.randomUUID(),
                        "Natural")),
                Set.of(new ProductResponse.ClassificationResponse(
                        UUID.randomUUID(),
                        "Bebida"))
        );
    }

    private ListInventoryProductsResponse.InventoryProductResponse givenInventoryProductResponse() {
        return new ListInventoryProductsResponse.InventoryProductResponse(
                UUID.randomUUID(),
                "Agua",
                "Sin gas",
                new BigDecimal("700.00"),
                null,
                false,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                50,
                3,
                10,
                47,
                "DISPONIBLE",
                "STOCK_EXACTO",
                null,
                null,
                null,
                null,
                null,
                null,
                "STOCK",
                null,
                null,
                true,
                false,
                false
        );
    }

    private void thenMapsFullProductCorrectly(Producto product, ProductResponseDTO dto) {
        assertEquals(product.getId(), dto.id());
        assertEquals("Product", dto.nombre());
        assertEquals(new BigDecimal("10.00"), dto.precio());
        assertTrue(dto.activo());
        assertEquals("DISPONIBLE", dto.estadoInventario());
        assertEquals("STOCK_EXACTO", dto.tipoManejoInventario());
        assertEquals(product.getCategoria().getId(), dto.categoria().id());
        assertEquals(1, dto.clasificacionesSalud().size());
        assertEquals("Salud", dto.clasificacionesSalud().iterator().next().descripcion());
        assertEquals(1, dto.tipos().size());
        assertEquals("Tipo", dto.tipos().iterator().next().descripcion());
    }

    private void thenMapsNullFieldsCorrectly(ProductResponseDTO dto) {
        assertNull(dto.categoria());
        assertEquals(0, dto.clasificacionesSalud().size());
        assertEquals(0, dto.tipos().size());
        assertNull(dto.estadoInventario());
    }

    private void thenMapsApplicationResponseCorrectly(ProductResponse response, ProductResponseDTO dto) {
        assertEquals(response.id(), dto.id());
        assertEquals(response.urlImagen(), dto.urlImagen());
        assertEquals(response.urlImagen(), dto.image());
        assertNull(dto.categoria());
        assertTrue(dto.clasificacionesSalud().isEmpty());
        assertTrue(dto.tipos().isEmpty());
    }

    private void thenMapsFavoriteProductClassificationsCorrectly(
            GetFavoritesByStudentResponse response, 
            ProductResponseDTO dto
    ) {
        assertEquals(response.categoria().id(), dto.categoria().id());
        assertEquals("Snacks", dto.categoria().descripcion());
        assertEquals(
                response.clasificacionesSalud().iterator().next().id(), 
                dto.clasificacionesSalud().iterator().next().id()
        );
        assertEquals(
                response.tipos().iterator().next().id(), 
                dto.tipos().iterator().next().id()
        );
    }

    private void thenMapsCategoryAndClassificationsCorrectly(
            ProductResponse response, 
            ProductResponseDTO dto
    ) {
        assertEquals(response.categoria().id(), dto.categoria().id());
        assertEquals(1, dto.clasificacionesSalud().size());
        assertEquals(1, dto.tipos().size());
    }

    private void thenMapsInventoryProductCorrectly(
            ListInventoryProductsResponse.InventoryProductResponse response, 
            ProductResponseDTO dto
    ) {
        assertEquals(response.id(), dto.id());
        assertNull(dto.categoria());
        assertTrue(dto.clasificacionesSalud().isEmpty());
        assertTrue(dto.tipos().isEmpty());
    }
}
