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

import static org.junit.jupiter.api.Assertions.*;

class ProductResponseDTOTest {

    @Test
    void fromDomain_ShouldMapFullProduct() {
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

        ProductResponseDTO dto = ProductResponseDTO.fromDomain(product);

        assertEquals(product.getId(), dto.id());
        assertEquals("Product", dto.nombre());
        assertEquals(new BigDecimal("10.00"), dto.precio());
        assertTrue(dto.activo());
        assertEquals("DISPONIBLE", dto.estadoInventario());
        assertEquals("STOCK_EXACTO", dto.tipoManejoInventario());
        assertEquals(category.getId(), dto.categoria().id());
        assertEquals(1, dto.clasificacionesSalud().size());
        assertEquals("Salud", dto.clasificacionesSalud().iterator().next().descripcion());
        assertEquals(1, dto.tipos().size());
        assertEquals("Tipo", dto.tipos().iterator().next().descripcion());
    }

    @Test
    void fromDomain_ShouldHandleNullFields() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setCategoria(null);
        product.setClasificacionesSalud(null);
        product.setTipos(null);
        product.setEstadoInventario(null);

        ProductResponseDTO dto = ProductResponseDTO.fromDomain(product);

        assertNull(dto.categoria());
        assertEquals(0, dto.clasificacionesSalud().size());
        assertEquals(0, dto.tipos().size());
        assertNull(dto.estadoInventario());
    }

    @Test
    void fromApplication_ShouldMapNullCategoryAndClassificationSets() {
        ProductResponse response = new ProductResponse(
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

        ProductResponseDTO dto = ProductResponseDTO.fromApplication(response);

        assertEquals(response.id(), dto.id());
        assertEquals(response.urlImagen(), dto.urlImagen());
        assertEquals(response.urlImagen(), dto.image());
        assertNull(dto.categoria());
        assertTrue(dto.clasificacionesSalud().isEmpty());
        assertTrue(dto.tipos().isEmpty());
    }

    @Test
    void fromUseCaseResponse_ShouldMapFavoriteProductClassifications() {
        UUID categoryId = UUID.randomUUID();
        UUID healthId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        GetFavoritesByStudentResponse response = new GetFavoritesByStudentResponse(
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

        ProductResponseDTO dto = ProductResponseDTO.fromUseCaseResponse(response);

        assertEquals(categoryId, dto.categoria().id());
        assertEquals("Snacks", dto.categoria().descripcion());
        assertEquals(healthId, dto.clasificacionesSalud().iterator().next().id());
        assertEquals(typeId, dto.tipos().iterator().next().id());
    }

    @Test
    void fromAppResponse_ShouldMapCategoryAndClassifications() {
        ProductResponse response = new ProductResponse(
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

        ProductResponseDTO dto = ProductResponseDTO.fromAppResponse(response);

        assertEquals(response.categoria().id(), dto.categoria().id());
        assertEquals(1, dto.clasificacionesSalud().size());
        assertEquals(1, dto.tipos().size());
    }

    @Test
    void fromInventoryProduct_ShouldMapNullCategoryAndClassifications() {
        ListInventoryProductsResponse.InventoryProductResponse response =
                new ListInventoryProductsResponse.InventoryProductResponse(
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

        ProductResponseDTO dto = ProductResponseDTO.fromInventoryProduct(response);

        assertEquals(response.id(), dto.id());
        assertNull(dto.categoria());
        assertTrue(dto.clasificacionesSalud().isEmpty());
        assertTrue(dto.tipos().isEmpty());
    }
}
