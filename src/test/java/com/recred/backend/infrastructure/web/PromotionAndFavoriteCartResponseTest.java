package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PromotionAndFavoriteCartResponseTest {

    @Test
    void promotionResponseFromDomain_ShouldMapProducts() {
        UUID productId = UUID.randomUUID();
        Promotion promotion = new Promotion();
        promotion.setId(UUID.randomUUID());
        promotion.setName("Promo");
        promotion.setDiscountPercentage(new BigDecimal("15.00"));
        promotion.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        promotion.setEndDate(LocalDateTime.of(2026, 6, 30, 23, 59));
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setProducts(List.of(product(productId, "Alfajor")));

        PromotionResponseDTO response = PromotionResponseDTO.fromDomain(promotion);

        assertEquals(promotion.getId(), response.getId());
        assertEquals("Promo", response.getName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(List.of(productId), response.getProductIds());
    }

    @Test
    void promotionResponseFromDomain_WhenProductsAreNull_ShouldLeaveProductIdsNull() {
        Promotion promotion = new Promotion();
        promotion.setStatus(PromotionStatus.DRAFT);

        PromotionResponseDTO response = PromotionResponseDTO.fromDomain(promotion);

        assertNull(response.getProductIds());
        assertEquals("DRAFT", response.getStatus());
    }

    @Test
    void favoriteCartResponseFromDomain_ShouldMapStudentAndItems() {
        UUID productId = UUID.randomUUID();
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        student.setNombre("Mateo");
        student.setApellido("Perez");
        Producto product = product(productId, "Yogur");
        product.setPrecio(new BigDecimal("800.00"));
        CarritoFavorito cart = new CarritoFavorito(
                UUID.randomUUID(),
                "Merienda",
                null,
                student,
                Map.of(product, 2)
        );

        CarritoFavoritoResponse response = CarritoFavoritoResponse.fromDomain(cart);

        assertEquals(cart.getId(), response.id());
        assertEquals("Merienda", response.nombre());
        assertEquals(student.getId(), response.alumnoId());
        assertEquals("Mateo", response.alumnoNombre());
        assertEquals("Perez", response.alumnoApellido());
        assertEquals(productId, response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
    }

    @Test
    void favoriteCartResponseFromDomain_WhenStudentAndItemsAreNull_ShouldUseNullAndEmptyList() {
        CarritoFavorito cart = new CarritoFavorito();
        cart.setId(UUID.randomUUID());
        cart.setItems(null);

        CarritoFavoritoResponse response = CarritoFavoritoResponse.fromDomain(cart);

        assertNull(response.alumnoId());
        assertEquals(List.of(), response.items());
    }

    private Producto product(UUID productId, String name) {
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre(name);
        return product;
    }
}
