package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.web.CarritoFavoritoResponse;
import com.recred.backend.infrastructure.web.PromotionResponseDTO;
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
    void promotionResponseFromDomainShouldMapProducts() {
        UUID productId = UUID.randomUUID();
        Promotion promotion = givenPromotion(productId);

        PromotionResponseDTO response = PromotionResponseDTO.fromDomain(promotion);

        thenMapsPromotionCorrectly(promotion, productId, response);
    }

    @Test
    void promotionResponseFromDomainWhenProductsAreNullShouldLeaveProductIdsNull() {
        Promotion promotion = givenPromotionWithNullProducts();

        PromotionResponseDTO response = PromotionResponseDTO.fromDomain(promotion);

        thenProductIdsAreNullAndStatusIsMapped(response);
    }

    @Test
    void favoriteCartResponseFromDomainShouldMapStudentAndItems() {
        UUID productId = UUID.randomUUID();
        CarritoFavorito cart = givenFavoriteCart(productId);

        CarritoFavoritoResponse response = CarritoFavoritoResponse.fromDomain(cart);

        thenMapsFavoriteCartCorrectly(cart, productId, response);
    }

    @Test
    void favoriteCartResponseFromDomainWhenStudentAndItemsAreNullShouldUseNullAndEmptyList() {
        CarritoFavorito cart = givenFavoriteCartWithNullStudentAndItems();

        CarritoFavoritoResponse response = CarritoFavoritoResponse.fromDomain(cart);

        thenNullStudentAndEmptyItemsAreMapped(response);
    }

    private Promotion givenPromotion(UUID productId) {
        Promotion promotion = new Promotion();
        promotion.setId(UUID.randomUUID());
        promotion.setName("Promo");
        promotion.setDiscountPercentage(new BigDecimal("15.00"));
        promotion.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        promotion.setEndDate(LocalDateTime.of(2026, 6, 30, 23, 59));
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setProducts(List.of(product(productId, "Alfajor")));
        return promotion;
    }

    private Promotion givenPromotionWithNullProducts() {
        Promotion promotion = new Promotion();
        promotion.setStatus(PromotionStatus.INACTIVE);
        return promotion;
    }

    private CarritoFavorito givenFavoriteCart(UUID productId) {
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        student.setNombre("Mateo");
        student.setApellido("Perez");
        Producto product = product(productId, "Yogur");
        product.setPrecio(new BigDecimal("800.00"));
        return new CarritoFavorito(
                UUID.randomUUID(),
                "Merienda",
                null,
                student,
                Map.of(product, 2)
        );
    }

    private CarritoFavorito givenFavoriteCartWithNullStudentAndItems() {
        CarritoFavorito cart = new CarritoFavorito();
        cart.setId(UUID.randomUUID());
        cart.setItems(null);
        return cart;
    }

    private Producto product(UUID productId, String name) {
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre(name);
        return product;
    }

    private void thenMapsPromotionCorrectly(Promotion promotion, UUID productId, PromotionResponseDTO response) {
        assertEquals(promotion.getId(), response.getId());
        assertEquals("Promo", response.getName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(List.of(productId), response.getProductIds());
    }

    private void thenProductIdsAreNullAndStatusIsMapped(PromotionResponseDTO response) {
        assertNull(response.getProductIds());
        assertEquals("INACTIVE", response.getStatus());
    }

    private void thenMapsFavoriteCartCorrectly(
            CarritoFavorito cart, 
            UUID productId, 
            CarritoFavoritoResponse response
    ) {
        assertEquals(cart.getId(), response.id());
        assertEquals("Merienda", response.nombre());
        assertEquals(cart.getAlumno().getId(), response.alumnoId());
        assertEquals("Mateo", response.alumnoNombre());
        assertEquals("Perez", response.alumnoApellido());
        assertEquals(productId, response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());
    }

    private void thenNullStudentAndEmptyItemsAreMapped(CarritoFavoritoResponse response) {
        assertNull(response.alumnoId());
        assertEquals(List.of(), response.items());
    }
}
