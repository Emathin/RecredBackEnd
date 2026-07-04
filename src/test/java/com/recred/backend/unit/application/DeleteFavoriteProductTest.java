package com.recred.backend.unit.application;

import com.recred.backend.application.DeleteFavoriteProduct;
import com.recred.backend.application.DeleteFavoriteProductRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteFavoriteProductTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private DeleteFavoriteProduct deleteFavoriteProduct;

    @Test
    void dadoDatosValidos_cuandoExecute_entoncesRemueveFavoritoYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Producto producto = new Producto();
        producto.setId(productoId);
        alumno.agregarProductoFavorito(producto);
        DeleteFavoriteProductRequest request = new DeleteFavoriteProductRequest(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        deleteFavoriteProduct.execute(request);

        assertFalse(alumno.getProductosFavoritos().contains(producto));
        verify(alumnoRepository).save(alumno);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        DeleteFavoriteProductRequest request = new DeleteFavoriteProductRequest(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Executable execution = () -> deleteFavoriteProduct.execute(request);

        assertThrows(RuntimeException.class, execution);
        verify(alumnoRepository, never()).save(any());
    }
}
