package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.DeleteFavoriteCart;
import com.recred.backend.application.DeleteFavoriteCartRequest;
import com.recred.backend.application.GetFavoriteCartsByTutor;
import com.recred.backend.application.GetFavoriteCartsByTutorRequest;
import com.recred.backend.application.GetFavoriteCartsByTutorResponse;
import com.recred.backend.application.SaveFavoriteCart;
import com.recred.backend.application.SaveFavoriteCartRequest;
import com.recred.backend.application.SaveFavoriteCartResponse;
import com.recred.backend.infrastructure.web.CarritoFavoritoController;
import com.recred.backend.infrastructure.web.CarritoFavoritoResponse;
import com.recred.backend.infrastructure.web.SaveCarritoFavoritoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarritoFavoritoControllerTest {

    private static final String TUTOR_SUB = "tutor-sub";
    private static final UUID CART_ID = UUID.randomUUID();
    private static final UUID ALUMNO_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String CART_NAME = "Merienda";
    private static final String PRODUCT_NAME = "Yogur";
    private static final String ALUMNO_LAST_NAME = "Alvarez";
    private static final String ALUMNO_NAME = "Lucia";
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("120.00");
    private static final int QUANTITY = 2;

    @Mock private SaveFavoriteCart saveFavoriteCart;
    @Mock private GetFavoriteCartsByTutor getFavoriteCartsByTutor;
    @Mock private DeleteFavoriteCart deleteFavoriteCart;

    private CarritoFavoritoController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new CarritoFavoritoController(
                saveFavoriteCart,
                getFavoriteCartsByTutor,
                deleteFavoriteCart
        );
        authentication = new TestingAuthenticationToken(TUTOR_SUB, "");
    }

    @Test
    void saveCartDelegatesWithProductQuantitiesAndReturnsCreatedCart() {
        givenCartIsSaved();

        ResponseEntity<CarritoFavoritoResponse> response = whenSavingCart();

        thenCartResponseIsCreated(response);
        thenDelegatesToSaveFavoriteCart();
    }

    @Test
    void getCartsDelegatesFiltersAndMapsResponse() {
        givenCartsRetrieved();

        ResponseEntity<List<CarritoFavoritoResponse>> response = whenGettingCarts();

        thenCartListResponseIsOk(response);
        thenDelegatesToGetFavoriteCartsByTutor();
    }

    @Test
    void deleteCartDelegatesAndReturnsNoContent() {
        ResponseEntity<Void> response = whenDeletingCart();

        thenResponseIsNoContent(response);
        thenDelegatesToDeleteFavoriteCart();
    }

    @Test
    void saveCartWithoutAuthenticationThrowsCredentialsException() {
        Executable execution = this::whenSavingCartWithoutAuthentication;

        thenThrowsAuthenticationCredentialsNotFoundException(execution);
    }

    private void givenCartIsSaved() {
        SaveFavoriteCartResponse cartResponse = new SaveFavoriteCartResponse(
                CART_ID,
                CART_NAME,
                UUID.randomUUID(),
                ALUMNO_ID,
                ALUMNO_NAME,
                ALUMNO_LAST_NAME,
                List.of(new SaveFavoriteCartResponse.CarritoFavoritoItemResponse(
                        PRODUCT_ID,
                        PRODUCT_NAME,
                        PRODUCT_PRICE,
                        QUANTITY
                ))
        );

        when(saveFavoriteCart.execute(any(SaveFavoriteCartRequest.class)))
                .thenReturn(cartResponse);
    }

    private void givenCartsRetrieved() {
        GetFavoriteCartsByTutorResponse responseDto = new GetFavoriteCartsByTutorResponse(
                CART_ID,
                CART_NAME,
                null,
                ALUMNO_ID,
                ALUMNO_NAME,
                ALUMNO_LAST_NAME,
                List.of(new GetFavoriteCartsByTutorResponse.CarritoFavoritoItemResponse(
                        PRODUCT_ID,
                        PRODUCT_NAME,
                        PRODUCT_PRICE,
                        QUANTITY
                ))
        );

        when(getFavoriteCartsByTutor.execute(new GetFavoriteCartsByTutorRequest(TUTOR_SUB, ALUMNO_ID, PRODUCT_ID)))
                .thenReturn(List.of(responseDto));
    }

    private ResponseEntity<CarritoFavoritoResponse> whenSavingCart() {
        return controller.saveCart(authentication, saveCartRequest());
    }

    private void whenSavingCartWithoutAuthentication() {
        controller.saveCart(null, saveCartRequest());
    }

    private ResponseEntity<List<CarritoFavoritoResponse>> whenGettingCarts() {
        return controller.getCarts(authentication, ALUMNO_ID, PRODUCT_ID);
    }

    private ResponseEntity<Void> whenDeletingCart() {
        return controller.deleteCart(authentication, CART_ID);
    }

    private void thenCartResponseIsCreated(ResponseEntity<CarritoFavoritoResponse> response) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        CarritoFavoritoResponse body = response.getBody();
        assertEquals(CART_ID, body.id());
        assertEquals(CART_NAME, body.nombre());
        assertEquals(ALUMNO_ID, body.alumnoId());
        assertEquals(ALUMNO_NAME, body.alumnoNombre());
        assertEquals(PRODUCT_ID, body.items().get(0).productId());
        assertEquals(QUANTITY, body.items().get(0).quantity());
    }

    private void thenDelegatesToSaveFavoriteCart() {
        ArgumentCaptor<SaveFavoriteCartRequest> requestCaptor =
                ArgumentCaptor.forClass(SaveFavoriteCartRequest.class);
        verify(saveFavoriteCart).execute(requestCaptor.capture());

        SaveFavoriteCartRequest capturedRequest = requestCaptor.getValue();
        assertEquals(TUTOR_SUB, capturedRequest.cognitoSub());
        assertEquals(CART_ID, capturedRequest.cartId());
        assertEquals(CART_NAME, capturedRequest.nombre());
        assertEquals(ALUMNO_ID, capturedRequest.alumnoId());
        assertEquals(Map.of(PRODUCT_ID, QUANTITY), capturedRequest.productQuantities());
    }

    private void thenCartListResponseIsOk(ResponseEntity<List<CarritoFavoritoResponse>> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(CART_ID, response.getBody().get(0).id());
        assertEquals(ALUMNO_LAST_NAME, response.getBody().get(0).alumnoApellido());
    }

    private void thenDelegatesToGetFavoriteCartsByTutor() {
        verify(getFavoriteCartsByTutor).execute(
                new GetFavoriteCartsByTutorRequest(TUTOR_SUB, ALUMNO_ID, PRODUCT_ID));
    }

    private void thenResponseIsNoContent(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    private void thenDelegatesToDeleteFavoriteCart() {
        verify(deleteFavoriteCart).execute(new DeleteFavoriteCartRequest(TUTOR_SUB, CART_ID));
    }

    private void thenThrowsAuthenticationCredentialsNotFoundException(Executable execution) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }

    private SaveCarritoFavoritoRequest saveCartRequest() {
        return new SaveCarritoFavoritoRequest(
                CART_ID,
                CART_NAME,
                ALUMNO_ID,
                List.of(new SaveCarritoFavoritoRequest.ItemRequest(PRODUCT_ID, QUANTITY))
        );
    }
}
