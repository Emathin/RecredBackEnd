package com.recred.backend.unit.application;

import com.recred.backend.application.ManualProductLock;
import com.recred.backend.application.ManualProductLockRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualProductLockTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ManualProductLock manualProductLock;

    @Test
    void dadoDatosValidos_cuandoExecute_entoncesBloqueaYGuarda() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        Producto producto = new Producto();
        producto.setId(productoId);
        ManualProductLockRequest request = new ManualProductLockRequest(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.of(producto));

        manualProductLock.execute(request);

        verify(alumnoRepository).save(alumno);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        ManualProductLockRequest request = new ManualProductLockRequest(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Executable execution = () -> manualProductLock.execute(request);

        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Alumno no encontrado", exception.getMessage());

        verify(productRepository, never()).findById(any());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void dadoProductoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        ManualProductLockRequest request = new ManualProductLockRequest(alumnoId, productoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findById(productoId)).thenReturn(Optional.empty());

        Executable execution = () -> manualProductLock.execute(request);

        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Producto no encontrado", exception.getMessage());

        verify(alumnoRepository, never()).save(any());
    }
}
