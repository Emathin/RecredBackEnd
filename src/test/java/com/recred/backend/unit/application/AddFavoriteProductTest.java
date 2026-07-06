package com.recred.backend.unit.application;

import com.recred.backend.application.AddFavoriteProduct;
import com.recred.backend.application.AddFavoriteProductRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddFavoriteProductTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ProductRepository productRepository;
 
    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private AddFavoriteProduct addFavoriteProduct;

    @Test
    void dadoDatosValidos_cuandoExecute_entoncesAgregaFavoritoYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = alumno(alumnoId);
        Producto producto = producto(productoId);
        AddFavoriteProductRequest request = request(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.of(producto));

        addFavoriteProduct.execute(request);

        assertTrue(alumno.getProductosFavoritos().contains(producto));
        verify(alumnoRepository).save(alumno);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        AddFavoriteProductRequest request = request(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Executable execution = () -> addFavoriteProduct.execute(request);

        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Alumno no encontrado", exception.getMessage());
        verify(productRepository, never()).findById(any());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void dadoProductoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = alumno(alumnoId);
        AddFavoriteProductRequest request = request(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.empty());

        Executable execution = () -> addFavoriteProduct.execute(request);

        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Producto no encontrado", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void dadoPlanGratuitoYMasDe5Favoritos_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = alumnoConFavoritos(alumnoId, 5);
        Producto nuevoProducto = producto(productoId);
        Tutor tutor = tutorConPlan(TipoPlan.GRATUITO);
        tutor.addHijo(alumno);
        AddFavoriteProductRequest request = request(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.of(nuevoProducto));
        when(tutorRepository.findByAlumnoId(alumnoId)).thenReturn(List.of(tutor));

        Executable execution = () -> addFavoriteProduct.execute(request);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("Límite de productos favoritos alcanzado para cuenta gratuita (máximo 5)", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void dadoPlanGratuitoY5FavoritosEntreHijos_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = alumnoConFavoritos(alumnoId, 2);
        Alumno otroHijo = alumnoConFavoritos(UUID.randomUUID(), 3);
        Producto nuevoProducto = producto(productoId);
        Tutor tutor = tutorConPlan(TipoPlan.GRATUITO);
        tutor.addHijo(alumno);
        tutor.addHijo(otroHijo);
        AddFavoriteProductRequest request = request(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.of(nuevoProducto));
        when(tutorRepository.findByAlumnoId(alumnoId)).thenReturn(List.of(tutor));

        Executable execution = () -> addFavoriteProduct.execute(request);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("Límite de productos favoritos alcanzado para cuenta gratuita (máximo 5)", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void dadoPlanAvanzadoYMasDe5Favoritos_cuandoExecute_entoncesAgregaYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = alumnoConFavoritos(alumnoId, 5);
        Producto nuevoProducto = producto(productoId);
        Tutor tutor = tutorConPlan(TipoPlan.AVANZADO);
        AddFavoriteProductRequest request = request(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.of(nuevoProducto));
        when(tutorRepository.findByAlumnoId(alumnoId)).thenReturn(List.of(tutor));

        addFavoriteProduct.execute(request);

        assertTrue(alumno.getProductosFavoritos().contains(nuevoProducto));
        assertEquals(6, alumno.getProductosFavoritos().size());
        verify(alumnoRepository).save(alumno);
    }

    private Alumno alumno(UUID alumnoId) {
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        return alumno;
    }

    private Alumno alumnoConFavoritos(UUID alumnoId, int cantidad) {
        Alumno alumno = alumno(alumnoId);
        for (int i = 0; i < cantidad; i++)
            alumno.agregarProductoFavorito(producto(UUID.randomUUID()));
        return alumno;
    }

    private Producto producto(UUID productoId) {
        Producto producto = new Producto();
        producto.setId(productoId);
        return producto;
    }

    private Tutor tutorConPlan(TipoPlan plan) {
        Usuario usuario = new Usuario();
        usuario.setPlan(plan);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setUsuario(usuario);
        return tutor;
    }

    private AddFavoriteProductRequest request(UUID alumnoId, UUID productoId) {
        return new AddFavoriteProductRequest(alumnoId, productoId);
    }
}
