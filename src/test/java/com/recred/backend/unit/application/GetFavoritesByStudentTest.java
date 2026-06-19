package com.recred.backend.unit.application;

import com.recred.backend.application.GetFavoritesByStudent;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFavoritesByStudentTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private GetFavoritesByStudent getFavoritesByStudent;

    @Test
    void dadoAlumnoExistente_cuandoExecute_entoncesRetornaFavoritos() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        alumno.addFavoriteProduct(producto);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        Set<Producto> resultado = getFavoritesByStudent.execute(alumnoId);

        assertEquals(1, resultado.size());
        assertTrue(resultado.contains(producto));
        verify(alumnoRepository, times(1)).findById(alumnoId);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                getFavoritesByStudent.execute(alumnoId)
        );

        verify(alumnoRepository, times(1)).findById(alumnoId);
    }
}
