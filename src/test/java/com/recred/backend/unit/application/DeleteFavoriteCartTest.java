package com.recred.backend.unit.application;

import com.recred.backend.application.DeleteFavoriteCart;
import com.recred.backend.application.DeleteFavoriteCartRequest;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteFavoriteCartTest {

    private static final String TUTOR_SUB = "tutor-sub";

    @Mock
    private CarritoFavoritoRepository carritoFavoritoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TutorRepository tutorRepository;

    private DeleteFavoriteCart deleteFavoriteCart;
    private DeleteFavoriteCartRequest deleteFavoriteCartRequest;
    private Throwable thrownException;
    private UUID userId;
    private UUID tutorId;
    private UUID cartId;

    @BeforeEach
    void setUp() {
        deleteFavoriteCart = new DeleteFavoriteCart(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository
        );
    }

    @Test
    void givenFavoriteCartOwnedByTutor_whenDeletingFavoriteCart_thenDeletesCart() {
        givenFavoriteCartOwnedByAuthenticatedTutor();

        whenDeleteFavoriteCart();

        thenFavoriteCartWasDeleted();
    }

    @Test
    void givenMissingAuthenticatedUser_whenDeletingFavoriteCart_thenThrowsException() {
        givenDeleteFavoriteCartRequest();
        givenAuthenticatedUserDoesNotExist();

        whenDeleteFavoriteCartFails(SecurityException.class);

        thenErrorMessageIs("Usuario no encontrado");
        thenDeleteWasNotStarted();
    }

    @Test
    void givenAuthenticatedUserWithoutTutorProfile_whenDeletingFavoriteCart_thenThrowsException() {
        givenDeleteFavoriteCartRequest();
        givenAuthenticatedUserExists();
        givenTutorProfileDoesNotExist();

        whenDeleteFavoriteCartFails(SecurityException.class);

        thenErrorMessageContains("perfil de tutor");
        thenFavoriteCartWasNotDeleted();
    }

    @Test
    void givenMissingFavoriteCart_whenDeletingFavoriteCart_thenThrowsException() {
        givenDeleteFavoriteCartRequest();
        givenAuthenticatedTutorExists();
        givenFavoriteCartDoesNotExist();

        whenDeleteFavoriteCartFails(IllegalArgumentException.class);

        thenErrorMessageIs("Carrito no encontrado");
        thenFavoriteCartWasNotDeleted();
    }

    @Test
    void givenFavoriteCartOwnedByAnotherTutor_whenDeletingFavoriteCart_thenThrowsException() {
        givenFavoriteCartOwnedByAnotherTutor();

        whenDeleteFavoriteCartFails(SecurityException.class);

        thenErrorMessageIs("No autorizado para eliminar este carrito");
        thenFavoriteCartWasNotDeleted();
    }

    private void givenFavoriteCartOwnedByAuthenticatedTutor() {
        givenDeleteFavoriteCartRequest();
        givenAuthenticatedTutorExists();
        givenFavoriteCartExists(tutor(tutorId));
    }

    private void givenFavoriteCartOwnedByAnotherTutor() {
        givenDeleteFavoriteCartRequest();
        givenAuthenticatedTutorExists();
        givenFavoriteCartExists(tutor(randomUUID()));
    }

    private void givenDeleteFavoriteCartRequest() {
        cartId = randomUUID();
        deleteFavoriteCartRequest = new DeleteFavoriteCartRequest(TUTOR_SUB, cartId);
    }

    private void givenAuthenticatedTutorExists() {
        givenAuthenticatedUserExists();
        givenTutorProfileExists();
    }

    private void givenAuthenticatedUserExists() {
        userId = randomUUID();
        when(usuarioRepository.findByIdCognito(TUTOR_SUB))
                .thenReturn(Optional.of(user(userId)));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(TUTOR_SUB)).thenReturn(Optional.empty());
    }

    private void givenTutorProfileExists() {
        tutorId = randomUUID();
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor(tutorId)));
    }

    private void givenTutorProfileDoesNotExist() {
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.empty());
    }

    private void givenFavoriteCartExists(Tutor tutor) {
        when(carritoFavoritoRepository.findById(cartId))
                .thenReturn(Optional.of(favoriteCart(tutor)));
    }

    private void givenFavoriteCartDoesNotExist() {
        when(carritoFavoritoRepository.findById(cartId)).thenReturn(Optional.empty());
    }

    private void whenDeleteFavoriteCart() {
        deleteFavoriteCart.execute(deleteFavoriteCartRequest);
    }

    private <T extends Throwable> void whenDeleteFavoriteCartFails(Class<T> expectedException) {
        Executable execution = () -> deleteFavoriteCart.execute(deleteFavoriteCartRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenFavoriteCartWasDeleted() {
        verify(carritoFavoritoRepository).delete(cartId);
    }

    private void thenDeleteWasNotStarted() {
        verify(tutorRepository, never()).findByUsuarioId(any());
        verify(carritoFavoritoRepository, never()).findById(any());
        thenFavoriteCartWasNotDeleted();
    }

    private void thenFavoriteCartWasNotDeleted() {
        verify(carritoFavoritoRepository, never()).delete(any());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenErrorMessageContains(String expectedMessage) {
        assertTrue(thrownException.getMessage().contains(expectedMessage));
    }

    private Usuario user(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        return user;
    }

    private Tutor tutor(UUID tutorId) {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        return tutor;
    }

    private CarritoFavorito favoriteCart(Tutor tutor) {
        CarritoFavorito cart = new CarritoFavorito();
        cart.setTutor(tutor);
        return cart;
    }
}
