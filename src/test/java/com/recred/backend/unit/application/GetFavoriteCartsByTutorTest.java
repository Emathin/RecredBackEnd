package com.recred.backend.unit.application;

import com.recred.backend.application.*;

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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFavoriteCartsByTutorTest {

    @Mock
    private CarritoFavoritoRepository carritoFavoritoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TutorRepository tutorRepository;

    private GetFavoriteCartsByTutor useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetFavoriteCartsByTutor(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository
        );
    }

    @Test
    void execute_ShouldReturnTutorFavoriteCartsWithFilters() {
        String cognitoSub = "sub";
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        CarritoFavorito cart = new CarritoFavorito();
        cart.setId(UUID.randomUUID());
        GetFavoriteCartsByTutorRequest request =
                new GetFavoriteCartsByTutorRequest(cognitoSub, alumnoId, productoId);

        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor));
        when(carritoFavoritoRepository.findByTutorIdAndFilters(tutorId, alumnoId, productoId))
                .thenReturn(List.of(cart));

        List<GetFavoriteCartsByTutorResponse> result = useCase.execute(request);

        GetFavoriteCartsByTutorResponse expected = new GetFavoriteCartsByTutorResponse(
                cart.getId(),
                cart.getNombre(),
                null,
                null,
                null,
                null,
                List.of()
        );

        assertEquals(List.of(expected), result);
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowSecurityException() {
        String cognitoSub = "sub";
        GetFavoriteCartsByTutorRequest request =
                new GetFavoriteCartsByTutorRequest(cognitoSub, UUID.randomUUID(), UUID.randomUUID());

        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        assertThrows(SecurityException.class, execution);

        verifyNoInteractions(tutorRepository, carritoFavoritoRepository);
    }

    @Test
    void execute_WhenTutorProfileDoesNotExist_ShouldThrowSecurityException() {
        String cognitoSub = "sub";
        UUID userId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        GetFavoriteCartsByTutorRequest request =
                new GetFavoriteCartsByTutorRequest(cognitoSub, UUID.randomUUID(), UUID.randomUUID());

        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        assertThrows(SecurityException.class, execution);

        verifyNoInteractions(carritoFavoritoRepository);
    }
}
