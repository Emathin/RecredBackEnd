package com.recred.backend.unit.application;

import com.recred.backend.application.GetFavoriteCartsByTutor;
import com.recred.backend.application.GetFavoriteCartsByTutorRequest;
import com.recred.backend.application.GetFavoriteCartsByTutorResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFavoriteCartsByTutorTest {

    private static final String COGNITO_SUB = "parent-sub";

    @Mock
    private CarritoFavoritoRepository carritoFavoritoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TutorRepository tutorRepository;

    private GetFavoriteCartsByTutor useCase;
    private GetFavoriteCartsByTutorRequest request;
    private List<GetFavoriteCartsByTutorResponse> response;
    private SecurityException thrownException;
    private UUID userId;
    private UUID tutorId;
    private UUID studentId;
    private UUID productId;
    private UUID cartId;

    @BeforeEach
    void setUp() {
        useCase = new GetFavoriteCartsByTutor(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository
        );
    }

    @Test
    void givenAuthenticatedTutorWithFavoriteCarts_whenGettingCarts_thenReturnsFilteredCarts() {
        givenFavoriteCartRequestWithFilters();
        givenAuthenticatedTutor();
        givenTutorHasFavoriteCartWithItem();

        whenGetFavoriteCartsByTutor();

        thenFavoriteCartWasReturned();
        thenFavoriteCartsWereRequestedWithFilters();
    }

    @Test
    void givenFavoriteCartWithoutItems_whenGettingCarts_thenReturnsEmptyItems() {
        givenFavoriteCartRequestWithFilters();
        givenAuthenticatedTutor();
        givenTutorHasFavoriteCartWithoutItems();

        whenGetFavoriteCartsByTutor();

        thenFavoriteCartWasReturnedWithoutItems();
    }

    @Test
    void givenAuthenticatedUserDoesNotExist_whenGettingCarts_thenThrowsSecurityException() {
        givenFavoriteCartRequestWithFilters();
        givenAuthenticatedUserDoesNotExist();

        whenGetFavoriteCartsByTutorFails();

        thenSecurityExceptionWasThrown("Usuario no encontrado");
        thenTutorAndCartsWereNotRequested();
    }

    @Test
    void givenTutorProfileDoesNotExist_whenGettingCarts_thenThrowsSecurityException() {
        givenFavoriteCartRequestWithFilters();
        givenAuthenticatedUserExists();
        givenTutorProfileDoesNotExist();

        whenGetFavoriteCartsByTutorFails();

        thenSecurityExceptionWasThrown("No se encontró perfil de tutor para el usuario");
        thenFavoriteCartsWereNotRequested();
    }

    private void givenFavoriteCartRequestWithFilters() {
        studentId = randomUUID();
        productId = randomUUID();
        request = new GetFavoriteCartsByTutorRequest(COGNITO_SUB, studentId, productId);
    }

    private void givenAuthenticatedTutor() {
        givenAuthenticatedUserExists();
        tutorId = randomUUID();

        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(tutor(tutorId)));
    }

    private void givenAuthenticatedUserExists() {
        userId = randomUUID();
        when(usuarioRepository.findByIdCognito(COGNITO_SUB)).thenReturn(Optional.of(user(userId)));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUB)).thenReturn(Optional.empty());
    }

    private void givenTutorProfileDoesNotExist() {
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.empty());
    }

    private void givenTutorHasFavoriteCartWithItem() {
        cartId = randomUUID();
        when(carritoFavoritoRepository.findByTutorIdAndFilters(tutorId, studentId, productId))
                .thenReturn(List.of(favoriteCartWithItem()));
    }

    private void givenTutorHasFavoriteCartWithoutItems() {
        cartId = randomUUID();
        when(carritoFavoritoRepository.findByTutorIdAndFilters(tutorId, studentId, productId))
                .thenReturn(List.of(favoriteCartWithoutItems()));
    }

    private void whenGetFavoriteCartsByTutor() {
        response = useCase.execute(request);
    }

    private void whenGetFavoriteCartsByTutorFails() {
        thrownException = assertThrows(SecurityException.class, executeUseCase());
    }

    private void thenFavoriteCartWasReturned() {
        GetFavoriteCartsByTutorResponse cart = response.get(0);
        assertEquals(1, response.size());
        assertEquals(cartId, cart.id());
        assertEquals("Merienda", cart.nombre());
        assertEquals(tutorId, cart.tutorId());
        assertEquals(studentId, cart.alumnoId());
        assertEquals("Juan", cart.alumnoNombre());
        assertEquals("Perez", cart.alumnoApellido());
        assertEquals(productId, cart.items().get(0).productId());
        assertEquals("Agua", cart.items().get(0).productoNombre());
        assertEquals(new BigDecimal("120.00"), cart.items().get(0).productoPrecio());
        assertEquals(2, cart.items().get(0).quantity());
    }

    private void thenFavoriteCartWasReturnedWithoutItems() {
        GetFavoriteCartsByTutorResponse cart = response.get(0);
        assertEquals(1, response.size());
        assertEquals(cartId, cart.id());
        assertEquals(List.of(), cart.items());
    }

    private void thenSecurityExceptionWasThrown(String message) {
        assertEquals(message, thrownException.getMessage());
    }

    private void thenFavoriteCartsWereRequestedWithFilters() {
        verify(carritoFavoritoRepository).findByTutorIdAndFilters(
                tutorId,
                studentId,
                productId
        );
    }

    private void thenTutorAndCartsWereNotRequested() {
        verifyNoInteractions(tutorRepository, carritoFavoritoRepository);
    }

    private void thenFavoriteCartsWereNotRequested() {
        verifyNoInteractions(carritoFavoritoRepository);
    }

    private Executable executeUseCase() {
        return () -> useCase.execute(request);
    }

    private Usuario user(UUID requestedUserId) {
        Usuario user = new Usuario();
        user.setId(requestedUserId);
        return user;
    }

    private Tutor tutor(UUID requestedTutorId) {
        Tutor tutor = new Tutor();
        tutor.setId(requestedTutorId);
        return tutor;
    }

    private CarritoFavorito favoriteCartWithItem() {
        return new CarritoFavorito(
                cartId,
                "Merienda",
                tutor(tutorId),
                student(),
                Map.of(product(), 2)
        );
    }

    private CarritoFavorito favoriteCartWithoutItems() {
        return new CarritoFavorito(
                cartId,
                "Merienda",
                tutor(tutorId),
                student(),
                null
        );
    }

    private Alumno student() {
        Alumno student = new Alumno();
        student.setId(studentId);
        student.setNombre("Juan");
        student.setApellido("Perez");
        return student;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Agua");
        product.setPrecio(new BigDecimal("120.00"));
        return product;
    }
}
