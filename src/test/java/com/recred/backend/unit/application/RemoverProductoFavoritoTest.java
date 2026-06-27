package com.recred.backend.unit.application;

import com.recred.backend.application.RemoverProductoFavorito;
import com.recred.backend.application.RemoverProductoFavoritoRequest;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoverProductoFavoritoTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private RemoverProductoFavorito removerProductoFavorito;

    @Test
    void dadoDatosValidos_cuandoExecute_entoncesRemueveFavoritoYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Producto producto = new Producto();
        producto.setId(productoId);
        alumno.agregarProductoFavorito(producto);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        removerProductoFavorito.execute(new RemoverProductoFavoritoRequest(alumnoId, productoId));

        assertFalse(alumno.getProductosFavoritos().contains(producto));
        verify(alumnoRepository, times(1)).save(alumno);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                removerProductoFavorito.execute(new RemoverProductoFavoritoRequest(alumnoId, productoId))
        );

        verify(alumnoRepository, never()).save(any());
    }
}
