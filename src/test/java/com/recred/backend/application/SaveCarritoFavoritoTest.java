package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveCarritoFavoritoTest {

    private static final String COGNITO_SUBJECT = "cognito-subject";

    @Mock
    private CarritoFavoritoRepository carritoFavoritoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ProductRepository productRepository;

    private SaveCarritoFavorito saveCarritoFavorito;

    @BeforeEach
    void setUp() {
        saveCarritoFavorito = new SaveCarritoFavorito(
                carritoFavoritoRepository,
                usuarioRepository,
                tutorRepository,
                alumnoRepository,
                productRepository
        );
    }

    @Test
    void executeShouldCreateCartForTutorChild() {
        Usuario usuario = usuario();
        Tutor tutor = tutor(usuario.getId());
        Alumno alumno = alumno();
        Producto producto = product(true);
        tutor.addHijo(alumno);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));
        when(productRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(carritoFavoritoRepository.save(any(CarritoFavorito.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CarritoFavorito result = saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                null,
                "Merienda",
                alumno.getId(),
                Map.of(producto.getId(), 2)
        );

        assertEquals("Merienda", result.getNombre());
        assertSame(tutor, result.getTutor());
        assertSame(alumno, result.getAlumno());
        assertEquals(2, result.getItems().get(producto));
        verify(carritoFavoritoRepository, never()).findById(any());
    }

    @Test
    void executeShouldUpdateExistingCartWhenItBelongsToTutor() {
        Usuario usuario = usuario();
        Tutor tutor = tutor(usuario.getId());
        Alumno alumno = alumno();
        UUID cartId = UUID.randomUUID();
        CarritoFavorito existing = new CarritoFavorito();
        existing.setId(cartId);
        existing.setTutor(tutor);
        tutor.addHijo(alumno);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));
        when(carritoFavoritoRepository.findById(cartId)).thenReturn(Optional.of(existing));
        when(carritoFavoritoRepository.save(existing)).thenReturn(existing);

        CarritoFavorito result = saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                cartId,
                "Almuerzo",
                alumno.getId(),
                null
        );

        assertSame(existing, result);
        assertEquals("Almuerzo", result.getNombre());
        assertSame(alumno, result.getAlumno());
        assertEquals(0, result.getItems().size());
        verify(carritoFavoritoRepository).save(existing);
    }

    @Test
    void executeShouldRejectStudentThatDoesNotBelongToTutor() {
        Usuario usuario = usuario();
        Tutor tutor = tutor(usuario.getId());
        Alumno alumno = alumno();

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));

        assertThrows(SecurityException.class, () -> saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                null,
                "Merienda",
                alumno.getId(),
                Map.of()
        ));
        verify(carritoFavoritoRepository, never()).save(any());
    }

    @Test
    void executeShouldRejectCartOwnedByAnotherTutor() {
        Usuario usuario = usuario();
        Tutor tutor = tutor(usuario.getId());
        Tutor anotherTutor = tutor(UUID.randomUUID());
        Alumno alumno = alumno();
        CarritoFavorito existing = new CarritoFavorito();
        existing.setId(UUID.randomUUID());
        existing.setTutor(anotherTutor);
        tutor.addHijo(alumno);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));
        when(carritoFavoritoRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class, () -> saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                existing.getId(),
                "Merienda",
                alumno.getId(),
                Map.of()
        ));
        verify(carritoFavoritoRepository, never()).save(any());
    }

    @Test
    void executeShouldRejectInvalidQuantity() {
        Usuario usuario = usuario();
        Tutor tutor = tutor(usuario.getId());
        Alumno alumno = alumno();
        UUID productId = UUID.randomUUID();
        tutor.addHijo(alumno);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));

        assertThrows(IllegalArgumentException.class, () -> saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                null,
                "Merienda",
                alumno.getId(),
                Map.of(productId, 0)
        ));
        verify(productRepository, never()).findById(any());
        verify(carritoFavoritoRepository, never()).save(any());
    }

    @Test
    void executeShouldRejectNullQuantity() {
        Usuario usuario = usuario();
        Tutor tutor = tutor(usuario.getId());
        Alumno alumno = alumno();
        UUID productId = UUID.randomUUID();
        Map<UUID, Integer> products = new HashMap<>();
        products.put(productId, null);
        tutor.addHijo(alumno);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));

        assertThrows(IllegalArgumentException.class, () -> saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                null,
                "Merienda",
                alumno.getId(),
                products
        ));
        verify(productRepository, never()).findById(any());
    }

    @Test
    void executeShouldRejectInactiveProduct() {
        Usuario usuario = usuario();
        Tutor tutor = tutor(usuario.getId());
        Alumno alumno = alumno();
        Producto product = product(false);
        tutor.addHijo(alumno);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                null,
                "Merienda",
                alumno.getId(),
                Map.of(product.getId(), 1)
        ));
        verify(carritoFavoritoRepository, never()).save(any());
    }

    @Test
    void executeShouldRejectMissingProduct() {
        Usuario usuario = usuario();
        Tutor tutor = tutor(usuario.getId());
        Alumno alumno = alumno();
        UUID productId = UUID.randomUUID();
        tutor.addHijo(alumno);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                null,
                "Merienda",
                alumno.getId(),
                Map.of(productId, 1)
        ));
        verify(carritoFavoritoRepository, never()).save(any());
    }

    @Test
    void executeShouldRejectMissingAuthenticatedUser() {
        UUID alumnoId = UUID.randomUUID();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                null,
                "Merienda",
                alumnoId,
                Map.of()
        ));
        verify(tutorRepository, never()).findByUsuarioId(any());
    }

    @Test
    void executeShouldSaveCartWithResolvedProducts() {
        Usuario usuario = usuario();
        Tutor tutor = tutor(usuario.getId());
        Alumno alumno = alumno();
        Producto producto = product(true);
        tutor.addHijo(alumno);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));
        when(productRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(carritoFavoritoRepository.save(any(CarritoFavorito.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                null,
                "Merienda",
                alumno.getId(),
                Map.of(producto.getId(), 3)
        );

        ArgumentCaptor<CarritoFavorito> captor = ArgumentCaptor.forClass(CarritoFavorito.class);
        verify(carritoFavoritoRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getItems().get(producto));
    }

    @Test
    void executeShouldRejectNewCartWhenFreePlanLimitExceeded() {
        Usuario usuario = usuario();
        usuario.setPlan(TipoPlan.GRATUITO);
        Tutor tutor = tutor(usuario.getId());
        Alumno alumno = alumno();
        tutor.addHijo(alumno);

        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));

        CarritoFavorito cart1 = new CarritoFavorito();
        CarritoFavorito cart2 = new CarritoFavorito();
        CarritoFavorito cart3 = new CarritoFavorito();
        when(carritoFavoritoRepository.findByTutorIdAndFilters(tutor.getId(), null, null))
                .thenReturn(java.util.List.of(cart1, cart2, cart3));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> saveCarritoFavorito.execute(
                COGNITO_SUBJECT,
                null,
                "Cuarto Carrito",
                alumno.getId(),
                Map.of()
        ));
        assertEquals("Límite de carritos favoritos alcanzado para cuenta gratuita (máximo 3)", exception.getMessage());
        verify(carritoFavoritoRepository, never()).save(any());
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        return usuario;
    }

    private Tutor tutor(UUID usuarioId) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setUsuario(usuario);
        return tutor;
    }

    private Alumno alumno() {
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        return alumno;
    }

    private Producto product(boolean active) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Alfajor");
        product.setActivo(active);
        return product;
    }
}
