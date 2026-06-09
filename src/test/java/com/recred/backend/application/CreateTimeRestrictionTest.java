package com.recred.backend.application;

import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import java.util.Optional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTimeRestrictionTest {

        @Mock
        private RestriccionHorariaRepository repository;
        @Mock
        private UsuarioRepository usuarioRepository;
        @Mock
        private AlumnoRepository alumnoRepository;

        @InjectMocks
        private CreateTimeRestriction createTimeRestriction;

        @Test
        void execute_CreaRestriccionGeneral_CuandoNoHayFiltros() {
                UUID studentId = UUID.randomUUID();
                UUID timeSlotId = UUID.randomUUID();

                TimeRestrictionCommand command = new TimeRestrictionCommand(studentId, timeSlotId, null, null);

                Usuario mockUsuario = new Usuario();
                mockUsuario.setId(UUID.randomUUID());
                mockUsuario.setNombre("testUser");
                mockUsuario.setEmail("test@test.com");
                mockUsuario.setRol(com.recred.backend.domain.models.RolUsuario.PADRE);
                when(usuarioRepository.findByIdCognito("test-sub")).thenReturn(Optional.of(mockUsuario));

                Alumno mockAlumno = new Alumno();
                when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(mockAlumno));

                when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

                RestriccionHoraria result = createTimeRestriction.execute(command, "test-sub");

                ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
                verify(repository).save(captor.capture());
                RestriccionHoraria guardada = captor.getValue();

                assertEquals(studentId, guardada.getAlumnoId());
                assertEquals(timeSlotId, guardada.getFranjaHoraria().getId());
                assertNull(guardada.getCategoria());
                assertNull(guardada.getClasificacionSalud());
                assertNotNull(result);
        }

        @Test
        void execute_CreaRestriccionEspecifica_CuandoSeEnvianFiltros() {
                UUID studentId = UUID.randomUUID();
                UUID timeSlotId = UUID.randomUUID();
                UUID categoryId = UUID.randomUUID();
                UUID classificationId = UUID.randomUUID();

                TimeRestrictionCommand command = new TimeRestrictionCommand(studentId, timeSlotId, categoryId,
                                classificationId);

                Usuario mockUsuario = new Usuario();
                mockUsuario.setId(UUID.randomUUID());
                mockUsuario.setNombre("testUser");
                mockUsuario.setRol(com.recred.backend.domain.models.RolUsuario.PADRE);
                when(usuarioRepository.findByIdCognito("test-sub")).thenReturn(Optional.of(mockUsuario));

                Alumno mockAlumno = new Alumno();
                when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(mockAlumno));

                when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

                createTimeRestriction.execute(command, "test-sub");

                ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
                verify(repository).save(captor.capture());
                RestriccionHoraria guardada = captor.getValue();

                assertEquals(categoryId, guardada.getCategoria().getId());
                assertEquals(classificationId, guardada.getClasificacionSalud().getId());
        }
}