package com.recred.backend.unit.application;

import com.recred.backend.application.GetFavoritesByStudent;
import com.recred.backend.application.GetFavoritesByStudentRequest;
import com.recred.backend.application.GetFavoritesByStudentResponse;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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
        alumno.agregarProductoFavorito(producto);
        GetFavoritesByStudentRequest request = new GetFavoritesByStudentRequest(alumnoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        Set<GetFavoritesByStudentResponse> resultado = getFavoritesByStudent.execute(request);

        assertEquals(1, resultado.size());
        GetFavoritesByStudentResponse response = resultado.iterator().next();
        assertEquals(producto.getId(), response.id());
        verify(alumnoRepository, times(1)).findById(alumnoId);
    }

    @Test
    void dadoAlumnoInexistente_cuandoExecute_entoncesLanzaExcepcion() {
        UUID alumnoId = UUID.randomUUID();
        GetFavoritesByStudentRequest request = new GetFavoritesByStudentRequest(alumnoId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Executable execution = () -> getFavoritesByStudent.execute(request);

        assertThrows(RuntimeException.class, execution);
        verify(alumnoRepository, times(1)).findById(alumnoId);
    }
}
