package com.recred.backend.web;

import com.recred.backend.infrastructure.web.*;

import com.recred.backend.application.DeleteCarritoFavorito;
import com.recred.backend.application.DeleteCarritoFavoritoRequest;
import com.recred.backend.application.GetCarritosFavoritosByTutor;
import com.recred.backend.application.GetCarritosFavoritosByTutorRequest;
import com.recred.backend.application.GetCarritosFavoritosByTutorResponse;
import com.recred.backend.application.SaveCarritoFavorito;
import com.recred.backend.application.SaveCarritoFavoritoResponse;
import com.recred.backend.domain.models.Alumno;
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

        SaveCarritoFavoritoResponse cartResponse = new SaveCarritoFavoritoResponse(
                cartId,
                "Merienda",
                UUID.randomUUID(),
                alumnoId,
                "Lucia",
                "Alvarez",
                List.of(new SaveCarritoFavoritoResponse.CarritoFavoritoItemResponse(
                        productId,
                        "Yogur",
                        new BigDecimal("120.00"),
                        2
                ))
        );

        when(saveCarritoFavorito.execute(
                org.mockito.ArgumentMatchers.any(com.recred.backend.application.SaveCarritoFavoritoRequest.class)
        )).thenReturn(cartResponse);

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

        ArgumentCaptor<com.recred.backend.application.SaveCarritoFavoritoRequest> requestCaptor =
                ArgumentCaptor.forClass(com.recred.backend.application.SaveCarritoFavoritoRequest.class);
        verify(saveCarritoFavorito).execute(requestCaptor.capture());

        com.recred.backend.application.SaveCarritoFavoritoRequest capturedRequest = requestCaptor.getValue();
        assertEquals("tutor-sub", capturedRequest.cognitoSub());
        assertEquals(cartId, capturedRequest.cartId());
        assertEquals("Merienda", capturedRequest.nombre());
        assertEquals(alumnoId, capturedRequest.alumnoId());
        assertEquals(Map.of(productId, 2), capturedRequest.productQuantities());
    }

    @Test
    void getCartsDelegatesFiltersAndMapsResponse() {
        UUID cartId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        GetCarritosFavoritosByTutorResponse responseDto = new GetCarritosFavoritosByTutorResponse(
                cartId,
                "Merienda",
                null,
                alumnoId,
                "Lucia",
                "Alvarez",
                List.of(new GetCarritosFavoritosByTutorResponse.CarritoFavoritoItemResponse(
                        productId,
                        "Yogur",
                        new BigDecimal("120.00"),
                        2
                ))
        );

        when(getCarritosFavoritosByTutor.execute(new GetCarritosFavoritosByTutorRequest("tutor-sub", alumnoId, productId)))
                .thenReturn(List.of(responseDto));

        ResponseEntity<List<CarritoFavoritoResponse>> response = controller.getCarts(
                new TestingAuthenticationToken("tutor-sub", ""),
                alumnoId,
                productId
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(cartId, response.getBody().get(0).id());
        assertEquals("Alvarez", response.getBody().get(0).alumnoApellido());
        verify(getCarritosFavoritosByTutor).execute(new GetCarritosFavoritosByTutorRequest("tutor-sub", alumnoId, productId));
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
}
