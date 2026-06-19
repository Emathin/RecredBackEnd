package com.recred.backend.unit.application;

import com.recred.backend.application.RemoveFavoriteProduct;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveFavoriteProductTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private RemoveFavoriteProduct removeFavoriteProduct;

    @Test
    void dadoDatosValidos_cuandoExecute_entoncesRemueveFavoritoYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Producto producto = new Producto();
        producto.setId(productoId);
        alumno.addFavoriteProduct(producto);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        removeFavoriteProduct.execute(alumnoId, productoId);

        assertFalse(alumno.getProductosFavoritos().contains(producto));
        verify(alumnoRepository, times(1)).save(alumno);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                removeFavoriteProduct.execute(alumnoId, productoId)
        );

        verify(alumnoRepository, never()).save(any());
    }
}
