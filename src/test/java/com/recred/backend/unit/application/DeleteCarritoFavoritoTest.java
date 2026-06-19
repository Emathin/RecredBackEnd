package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteCarritoFavoritoTest {

    private CarritoFavoritoRepository carritoFavoritoRepository;
    private UsuarioRepository usuarioRepository;
    private TutorRepository tutorRepository;
    private DeleteCarritoFavorito useCase;

    @BeforeEach
    void setUp() {
        carritoFavoritoRepository = mock(CarritoFavoritoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        tutorRepository = mock(TutorRepository.class);
        useCase = new DeleteCarritoFavorito(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository
        );
    }

    @Test
    void executeDeletesCartWhenItBelongsToAuthenticatedTutor() {
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        Usuario user = user(userId);
        Tutor tutor = tutor(tutorId);
        CarritoFavorito cart = new CarritoFavorito();
        cart.setTutor(tutor(tutorId));
        when(usuarioRepository.findByIdCognito("tutor-sub")).thenReturn(Optional.of(user));
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor));
        when(carritoFavoritoRepository.findById(cartId)).thenReturn(Optional.of(cart));

        useCase.execute("tutor-sub", cartId);

        verify(carritoFavoritoRepository).delete(cartId);
    }

    @Test
    void executeThrowsWhenAuthenticatedUserDoesNotExist() {
        UUID cartId = UUID.randomUUID();
        when(usuarioRepository.findByIdCognito("tutor-sub")).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> useCase.execute("tutor-sub", cartId));

        verify(carritoFavoritoRepository, never()).delete(cartId);
    }

    @Test
    void executeThrowsWhenTutorProfileDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        when(usuarioRepository.findByIdCognito("tutor-sub"))
                .thenReturn(Optional.of(user(userId)));
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> useCase.execute("tutor-sub", cartId));

        verify(carritoFavoritoRepository, never()).delete(cartId);
    }

    @Test
    void executeThrowsWhenCartDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        when(usuarioRepository.findByIdCognito("tutor-sub"))
                .thenReturn(Optional.of(user(userId)));
        when(tutorRepository.findByUsuarioId(userId))
                .thenReturn(Optional.of(tutor(UUID.randomUUID())));
        when(carritoFavoritoRepository.findById(cartId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute("tutor-sub", cartId));

        verify(carritoFavoritoRepository, never()).delete(cartId);
    }

    @Test
    void executeThrowsWhenCartBelongsToAnotherTutor() {
        UUID userId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        CarritoFavorito cart = new CarritoFavorito();
        cart.setTutor(tutor(UUID.randomUUID()));
        when(usuarioRepository.findByIdCognito("tutor-sub"))
                .thenReturn(Optional.of(user(userId)));
        when(tutorRepository.findByUsuarioId(userId))
                .thenReturn(Optional.of(tutor(UUID.randomUUID())));
        when(carritoFavoritoRepository.findById(cartId)).thenReturn(Optional.of(cart));

        assertThrows(SecurityException.class, () -> useCase.execute("tutor-sub", cartId));

        verify(carritoFavoritoRepository, never()).delete(cartId);
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
}
