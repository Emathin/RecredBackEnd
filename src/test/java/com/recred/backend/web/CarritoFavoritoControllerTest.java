package com.recred.backend.web;

import com.recred.backend.infrastructure.web.*;

import com.recred.backend.application.DeleteCarritoFavorito;
import com.recred.backend.application.DeleteCarritoFavoritoRequest;
import com.recred.backend.application.GetCarritosFavoritosByTutor;
import com.recred.backend.application.SaveCarritoFavorito;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CarritoFavoritoControllerTest {

    private SaveCarritoFavorito saveCarritoFavorito;
    private GetCarritosFavoritosByTutor getCarritosFavoritosByTutor;
    private DeleteCarritoFavorito deleteCarritoFavorito;
    private CarritoFavoritoController controller;

    @BeforeEach
    void setUp() {
        saveCarritoFavorito = mock(SaveCarritoFavorito.class);
        getCarritosFavoritosByTutor = mock(GetCarritosFavoritosByTutor.class);
        deleteCarritoFavorito = mock(DeleteCarritoFavorito.class);
        controller = new CarritoFavoritoController(
                saveCarritoFavorito,
                getCarritosFavoritosByTutor,
                deleteCarritoFavorito
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveCartDelegatesWithProductQuantitiesAndReturnsCreatedCart() {
        UUID cartId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        SaveCarritoFavoritoRequest request = new SaveCarritoFavoritoRequest(
                cartId,
                "Merienda",
                alumnoId,
                List.of(new SaveCarritoFavoritoRequest.ItemRequest(productId, 2))
        );
        CarritoFavorito cart = cart(cartId, alumnoId, productId);
        when(saveCarritoFavorito.execute(
                eq("tutor-sub"),
                eq(cartId),
                eq("Merienda"),
                eq(alumnoId),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(cart);

        ResponseEntity<CarritoFavoritoResponse> response = controller.saveCart(
                new TestingAuthenticationToken("tutor-sub", ""),
                request
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(cartId, response.getBody().id());
        assertEquals("Merienda", response.getBody().nombre());
        assertEquals(alumnoId, response.getBody().alumnoId());
        assertEquals("Lucia", response.getBody().alumnoNombre());
        assertEquals(productId, response.getBody().items().get(0).productId());
        assertEquals(2, response.getBody().items().get(0).quantity());

        ArgumentCaptor<Map<UUID, Integer>> quantitiesCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(saveCarritoFavorito).execute(
                eq("tutor-sub"),
                eq(cartId),
                eq("Merienda"),
                eq(alumnoId),
                quantitiesCaptor.capture()
        );
        assertEquals(Map.of(productId, 2), quantitiesCaptor.getValue());
    }

    @Test
    void getCartsDelegatesFiltersAndMapsResponse() {
        UUID cartId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(getCarritosFavoritosByTutor.execute("tutor-sub", alumnoId, productId))
                .thenReturn(List.of(cart(cartId, alumnoId, productId)));

        ResponseEntity<List<CarritoFavoritoResponse>> response = controller.getCarts(
                new TestingAuthenticationToken("tutor-sub", ""),
                alumnoId,
                productId
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(cartId, response.getBody().get(0).id());
        assertEquals("Alvarez", response.getBody().get(0).alumnoApellido());
        verify(getCarritosFavoritosByTutor).execute("tutor-sub", alumnoId, productId);
    }

    @Test
    void deleteCartDelegatesAndReturnsNoContent() {
        UUID cartId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteCart(
                new TestingAuthenticationToken("tutor-sub", ""),
                cartId
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteCarritoFavorito).execute(new DeleteCarritoFavoritoRequest("tutor-sub", cartId));
    }

    @Test
    void saveCartWithoutAuthenticationThrowsCredentialsException() {
        SaveCarritoFavoritoRequest request = new SaveCarritoFavoritoRequest(
                UUID.randomUUID(),
                "Merienda",
                UUID.randomUUID(),
                List.of(new SaveCarritoFavoritoRequest.ItemRequest(UUID.randomUUID(), 1))
        );

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.saveCart(null, request));
    }

    private CarritoFavorito cart(UUID cartId, UUID alumnoId, UUID productId) {
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setNombre("Lucia");
        alumno.setApellido("Alvarez");

        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Yogur");
        product.setPrecio(new BigDecimal("120.00"));

        return new CarritoFavorito(
                cartId,
                "Merienda",
                null,
                alumno,
                Map.of(product, 2)
        );
    }
}
