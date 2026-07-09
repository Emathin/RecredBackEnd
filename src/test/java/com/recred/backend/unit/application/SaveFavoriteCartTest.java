package com.recred.backend.unit.application;

import com.recred.backend.application.SaveFavoriteCart;
import com.recred.backend.application.SaveFavoriteCartRequest;
import com.recred.backend.application.SaveFavoriteCartResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaveFavoriteCartTest {

    private static final String COGNITO_SUBJECT = "cognito-subject";
    private static final String CART_NAME = "Merienda";

    private final CarritoFavoritoRepository carritoFavoritoRepository =
            mock(CarritoFavoritoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final TutorRepository tutorRepository = mock(TutorRepository.class);
    private final AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final SaveFavoriteCart useCase = new SaveFavoriteCart(
            carritoFavoritoRepository,
            usuarioRepository,
            tutorRepository,
            alumnoRepository,
            productRepository
    );

    private SaveFavoriteCartRequest request;
    private SaveFavoriteCartResponse response;
    private Throwable exception;
    private Usuario user;
    private Tutor tutor;
    private Tutor anotherTutor;
    private Alumno student;
    private Producto product;
    private CarritoFavorito existingCart;
    private UUID productId;

    @Test
    void execute_WhenTutorCreatesCartForChild_SavesFavoriteCart() {
        givenTutorWithChild();
        givenActiveProduct();
        givenNewCartRequestWithProductQuantity(2);
        givenCartCanBeSaved();

        whenSavingFavoriteCart();

        thenCreatedCartIsReturned(2);
        thenNewCartWasSaved();
    }

    @Test
    void execute_WhenExistingCartBelongsToTutor_UpdatesFavoriteCart() {
        givenTutorWithChild();
        givenExistingCartOwnedByTutor();
        givenExistingCartRequest();
        givenExistingCartCanBeSaved();

        whenSavingFavoriteCart();

        thenUpdatedCartIsReturned();
        thenExistingCartWasSaved();
    }

    @Test
    void execute_WhenStudentDoesNotBelongToTutor_RejectsRequest() {
        givenTutorWithoutChild();
        givenNewCartRequestWithoutProducts();

        whenSavingFavoriteCartFails(SecurityException.class);

        thenCartWasNotSaved();
    }

    @Test
    void execute_WhenCartBelongsToAnotherTutor_RejectsRequest() {
        givenTutorWithChild();
        givenExistingCartOwnedByAnotherTutor();
        givenExistingCartRequest();

        whenSavingFavoriteCartFails(SecurityException.class);

        thenCartWasNotSaved();
    }

    @Test
    void execute_WhenQuantityIsZero_RejectsRequestBeforeReadingProduct() {
        givenTutorWithChild();
        givenNewCartRequestWithProductQuantity(0);

        whenSavingFavoriteCartFails(IllegalArgumentException.class);

        thenProductWasNotRead();
        thenCartWasNotSaved();
    }

    @Test
    void execute_WhenFreePlanHasFiveCarts_RejectsNewCart() {
        givenTutorWithChild();
        givenNewCartRequestWithoutProducts();
        givenTutorAlreadyHasFavoriteCarts(5);

        whenSavingFavoriteCartFails(IllegalArgumentException.class);

        thenErrorMessageIs(
                "Límite de carritos favoritos alcanzado para cuenta gratuita (máximo 5)"
        );
        thenCartWasNotSaved();
    }

    @Test
    void execute_WhenQuantityIsNull_RejectsRequestBeforeReadingProduct() {
        givenTutorWithChild();
        givenNewCartRequestWithNullProductQuantity();

        whenSavingFavoriteCartFails(IllegalArgumentException.class);

        thenProductWasNotRead();
        thenCartWasNotSaved();
    }

    @Test
    void execute_WhenProductIsInactive_RejectsRequest() {
        givenTutorWithChild();
        givenInactiveProduct();
        givenNewCartRequestWithProductQuantity(1);

        whenSavingFavoriteCartFails(IllegalArgumentException.class);

        thenCartWasNotSaved();
    }

    @Test
    void execute_WhenProductDoesNotExist_RejectsRequest() {
        givenTutorWithChild();
        givenMissingProduct();
        givenNewCartRequestWithProductQuantity(1);

        whenSavingFavoriteCartFails(IllegalArgumentException.class);

        thenCartWasNotSaved();
    }

    @Test
    void execute_WhenAuthenticatedUserDoesNotExist_RejectsRequestBeforeReadingTutor() {
        givenMissingAuthenticatedUser();
        givenNewCartRequestWithoutProducts();

        whenSavingFavoriteCartFails(SecurityException.class);

        thenTutorWasNotRead();
    }

    @Test
    void execute_WhenSavingCartWithProduct_SavesResolvedProductQuantity() {
        givenTutorWithChild();
        givenActiveProduct();
        givenNewCartRequestWithProductQuantity(3);
        givenCartCanBeSaved();

        whenSavingFavoriteCart();

        thenCartWasSavedWithResolvedProductQuantity(3);
    }

    private void givenTutorWithChild() {
        givenAuthenticatedUser();
        givenTutor();
        givenStudent();
        tutor.addHijo(student);
        givenAuthenticatedTutorAndStudentCanBeRead();
    }

    private void givenTutorWithoutChild() {
        givenAuthenticatedUser();
        givenTutor();
        givenStudent();
        givenAuthenticatedTutorAndStudentCanBeRead();
    }

    private void givenAuthenticatedUser() {
        user = user();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
    }

    private void givenMissingAuthenticatedUser() {
        student = student();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenTutor() {
        tutor = tutor(user.getId());
    }

    private void givenStudent() {
        student = student();
    }

    private void givenAuthenticatedTutorAndStudentCanBeRead() {
        when(tutorRepository.findByUsuarioId(user.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(student.getId())).thenReturn(Optional.of(student));
    }

    private void givenActiveProduct() {
        product = product(true);
        productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    }

    private void givenInactiveProduct() {
        product = product(false);
        productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    }

    private void givenMissingProduct() {
        productId = randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
    }

    private void givenExistingCartOwnedByTutor() {
        existingCart = existingCart(tutor);
        when(carritoFavoritoRepository.findById(existingCart.getId()))
                .thenReturn(Optional.of(existingCart));
    }

    private void givenExistingCartOwnedByAnotherTutor() {
        anotherTutor = tutor(randomUUID());
        existingCart = existingCart(anotherTutor);
        when(carritoFavoritoRepository.findById(existingCart.getId()))
                .thenReturn(Optional.of(existingCart));
    }

    private void givenNewCartRequestWithoutProducts() {
        request = new SaveFavoriteCartRequest(
                COGNITO_SUBJECT,
                null,
                CART_NAME,
                student.getId(),
                Map.of()
        );
    }

    private void givenNewCartRequestWithProductQuantity(Integer quantity) {
        if (productId == null) {
            productId = randomUUID();
        }
        request = new SaveFavoriteCartRequest(
                COGNITO_SUBJECT,
                null,
                CART_NAME,
                student.getId(),
                Map.of(productId, quantity)
        );
    }

    private void givenNewCartRequestWithNullProductQuantity() {
        productId = randomUUID();
        Map<UUID, Integer> products = new HashMap<>();
        products.put(productId, null);
        request = new SaveFavoriteCartRequest(
                COGNITO_SUBJECT,
                null,
                CART_NAME,
                student.getId(),
                products
        );
    }

    private void givenExistingCartRequest() {
        request = new SaveFavoriteCartRequest(
                COGNITO_SUBJECT,
                existingCart.getId(),
                "Almuerzo",
                student.getId(),
                null
        );
    }

    private void givenCartCanBeSaved() {
        when(carritoFavoritoRepository.save(any(CarritoFavorito.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenExistingCartCanBeSaved() {
        when(carritoFavoritoRepository.save(existingCart)).thenReturn(existingCart);
    }

    private void givenTutorAlreadyHasFavoriteCarts(int count) {
        when(carritoFavoritoRepository.findByTutorIdAndFilters(tutor.getId(), null, null))
                .thenReturn(existingCarts(count));
    }

    private void whenSavingFavoriteCart() {
        response = useCase.execute(request);
    }

    private <T extends Throwable> void whenSavingFavoriteCartFails(
            Class<T> expectedException
    ) {
        Executable execution = () -> useCase.execute(request);
        exception = assertThrows(expectedException, execution);
    }

    private void thenCreatedCartIsReturned(int expectedQuantity) {
        assertEquals(CART_NAME, response.nombre());
        assertEquals(tutor.getId(), response.tutorId());
        assertEquals(student.getId(), response.alumnoId());
        assertEquals(expectedQuantity, response.items().get(0).quantity());
    }

    private void thenUpdatedCartIsReturned() {
        assertEquals("Almuerzo", response.nombre());
        assertEquals(student.getId(), response.alumnoId());
        assertEquals(0, response.items().size());
    }

    private void thenNewCartWasSaved() {
        verify(carritoFavoritoRepository, never()).findById(any());
        verify(carritoFavoritoRepository).save(any(CarritoFavorito.class));
    }

    private void thenExistingCartWasSaved() {
        verify(carritoFavoritoRepository).save(existingCart);
    }

    private void thenCartWasNotSaved() {
        verify(carritoFavoritoRepository, never()).save(any());
    }

    private void thenProductWasNotRead() {
        verify(productRepository, never()).findById(any());
    }

    private void thenTutorWasNotRead() {
        verify(tutorRepository, never()).findByUsuarioId(any());
    }

    private void thenCartWasSavedWithResolvedProductQuantity(int expectedQuantity) {
        ArgumentCaptor<CarritoFavorito> captor = ArgumentCaptor.forClass(CarritoFavorito.class);
        verify(carritoFavoritoRepository).save(captor.capture());
        assertEquals(expectedQuantity, captor.getValue().getItems().get(product));
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private Usuario user() {
        Usuario user = new Usuario();
        user.setId(randomUUID());
        return user;
    }

    private Tutor tutor(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        Tutor tutor = new Tutor();
        tutor.setId(randomUUID());
        tutor.setUsuario(user);
        return tutor;
    }

    private Alumno student() {
        Alumno student = new Alumno();
        student.setId(randomUUID());
        return student;
    }

    private Producto product(boolean active) {
        Producto product = new Producto();
        product.setId(randomUUID());
        product.setNombre("Alfajor");
        product.setActivo(active);
        return product;
    }

    private CarritoFavorito existingCart(Tutor owner) {
        CarritoFavorito cart = new CarritoFavorito();
        cart.setId(randomUUID());
        cart.setTutor(owner);
        return cart;
    }

    private List<CarritoFavorito> existingCarts(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> new CarritoFavorito())
                .toList();
    }
}
