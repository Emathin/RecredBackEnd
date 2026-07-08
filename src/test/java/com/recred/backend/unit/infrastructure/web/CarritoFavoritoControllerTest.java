package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.SaveFavoriteCartRequest;
import com.recred.backend.infrastructure.web.*;

import com.recred.backend.application.DeleteFavoriteCart;
import com.recred.backend.application.DeleteFavoriteCartRequest;
import com.recred.backend.application.GetFavoriteCartsByTutor;
import com.recred.backend.application.GetFavoriteCartsByTutorRequest;
import com.recred.backend.application.GetFavoriteCartsByTutorResponse;
import com.recred.backend.application.SaveFavoriteCart;
import com.recred.backend.application.SaveFavoriteCartResponse;

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

    private SaveFavoriteCart saveFavoriteCart;
    private GetFavoriteCartsByTutor getFavoriteCartsByTutor;
    private DeleteFavoriteCart deleteFavoriteCart;
    private SchoolLicenseAccessGuard schoolLicenseAccessGuard;
    private CarritoFavoritoController controller;

    @BeforeEach
    void setUp() {
        saveFavoriteCart = mock(SaveFavoriteCart.class);
        getFavoriteCartsByTutor = mock(GetFavoriteCartsByTutor.class);
        deleteFavoriteCart = mock(DeleteFavoriteCart.class);
        schoolLicenseAccessGuard = mock(SchoolLicenseAccessGuard.class);
        controller = new CarritoFavoritoController(
                saveFavoriteCart,
                getFavoriteCartsByTutor,
                deleteFavoriteCart,
                schoolLicenseAccessGuard
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

        SaveFavoriteCartResponse cartResponse = new SaveFavoriteCartResponse(
                cartId,
                "Merienda",
                UUID.randomUUID(),
                alumnoId,
                "Lucia",
                "Alvarez",
                List.of(new SaveFavoriteCartResponse.CarritoFavoritoItemResponse(
                        productId,
                        "Yogur",
                        new BigDecimal("120.00"),
                        2
                ))
        );

        when(saveFavoriteCart.execute(
                org.mockito.ArgumentMatchers.any(SaveFavoriteCartRequest.class)
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

        ArgumentCaptor<SaveFavoriteCartRequest> requestCaptor =
                ArgumentCaptor.forClass(SaveFavoriteCartRequest.class);
        verify(saveFavoriteCart).execute(requestCaptor.capture());
        verify(schoolLicenseAccessGuard).requireActiveSchoolForStudent(alumnoId);

        SaveFavoriteCartRequest capturedRequest = requestCaptor.getValue();
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
        GetFavoriteCartsByTutorResponse responseDto = new GetFavoriteCartsByTutorResponse(
                cartId,
                "Merienda",
                null,
                alumnoId,
                "Lucia",
                "Alvarez",
                List.of(new GetFavoriteCartsByTutorResponse.CarritoFavoritoItemResponse(
                        productId,
                        "Yogur",
                        new BigDecimal("120.00"),
                        2
                ))
        );

        when(getFavoriteCartsByTutor.execute(new GetFavoriteCartsByTutorRequest("tutor-sub", alumnoId, productId)))
                .thenReturn(List.of(responseDto));

        ResponseEntity<List<CarritoFavoritoResponse>> response = controller.getCarts(
                new TestingAuthenticationToken("tutor-sub", ""),
                alumnoId,
                productId
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(cartId, response.getBody().get(0).id());
        assertEquals("Alvarez", response.getBody().get(0).alumnoApellido());
        verify(schoolLicenseAccessGuard).requireActiveSchoolForStudent(alumnoId);
        verify(getFavoriteCartsByTutor).execute(new GetFavoriteCartsByTutorRequest("tutor-sub", alumnoId, productId));
    }

    @Test
    void deleteCartDelegatesAndReturnsNoContent() {
        UUID cartId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteCart(
                new TestingAuthenticationToken("tutor-sub", ""),
                cartId
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteFavoriteCart).execute(new DeleteFavoriteCartRequest("tutor-sub", cartId));
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
