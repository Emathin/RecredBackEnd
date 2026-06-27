package com.recred.backend.unit.application;

import com.recred.backend.application.AgregarProductoFavorito;
import com.recred.backend.application.AgregarProductoFavoritoRequest;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgregarProductoFavoritoTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ProductRepository productRepository;
 
    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private AgregarProductoFavorito agregarProductoFavorito;

    @Test
    void dadoDatosValidos_cuandoExecute_entoncesAgregaFavoritoYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Producto producto = new Producto();
        producto.setId(productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.of(producto));

        agregarProductoFavorito.execute(new AgregarProductoFavoritoRequest(alumnoId, productoId));

        assertTrue(alumno.getProductosFavoritos().contains(producto));
        verify(alumnoRepository, times(1)).save(alumno);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                agregarProductoFavorito.execute(new AgregarProductoFavoritoRequest(alumnoId, productoId))
        );

        verify(productRepository, never()).findById(any());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void dadoProductoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                agregarProductoFavorito.execute(new AgregarProductoFavoritoRequest(alumnoId, productoId))
        );

        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void dadoPlanGratuitoYMasDe5Favoritos_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);

        for (int i = 0; i < 5; i++) {
            Producto p = new Producto();
            p.setId(UUID.randomUUID());
            alumno.agregarProductoFavorito(p);
        }

        Producto nuevoProducto = new Producto();
        nuevoProducto.setId(productoId);

        Usuario usuario = new Usuario();
        usuario.setPlan(TipoPlan.GRATUITO);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.of(nuevoProducto));
        when(tutorRepository.findByAlumnoId(alumnoId)).thenReturn(java.util.List.of(tutor));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                agregarProductoFavorito.execute(new AgregarProductoFavoritoRequest(alumnoId, productoId))
        );
        assertEquals("Límite de productos favoritos alcanzado para cuenta gratuita (máximo 5)", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void dadoPlanPremiumYMasDe5Favoritos_cuandoExecute_entoncesAgregaYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);

        for (int i = 0; i < 5; i++) {
            Producto p = new Producto();
            p.setId(UUID.randomUUID());
            alumno.agregarProductoFavorito(p);
        }

        Producto nuevoProducto = new Producto();
        nuevoProducto.setId(productoId);

        Usuario usuario = new Usuario();
        usuario.setPlan(TipoPlan.PREMIUM);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.of(nuevoProducto));
        when(tutorRepository.findByAlumnoId(alumnoId)).thenReturn(java.util.List.of(tutor));

        agregarProductoFavorito.execute(new AgregarProductoFavoritoRequest(alumnoId, productoId));

        assertTrue(alumno.getProductosFavoritos().contains(nuevoProducto));
        assertEquals(6, alumno.getProductosFavoritos().size());
        verify(alumnoRepository, times(1)).save(alumno);
    }
}
