package com.recred.backend.unit.application;

import com.recred.backend.application.BloquearProductoManual;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BloquearProductoManualTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BloquearProductoManual bloquearProductoManual;

    @Test
    void dadoDatosValidos_cuandoExecute_entoncesBloqueaYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        Producto producto = new Producto();
        producto.setId(productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.of(producto));

        bloquearProductoManual.execute(alumnoId, productoId);

        verify(alumnoRepository, times(1)).save(alumno);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                bloquearProductoManual.execute(alumnoId, productoId)
        );

        verify(productRepository, never()).findById(any());
        verify(alumnoRepository, never()).save(any());
    }
}
