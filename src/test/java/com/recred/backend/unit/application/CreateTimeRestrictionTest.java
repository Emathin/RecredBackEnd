package com.recred.backend.unit.application;

import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.CreateTimeRestrictionRequest;
import com.recred.backend.application.CreateTimeRestrictionResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    private CreateTimeRestriction useCase;

    private UUID studentId;
    private UUID timeSlotId;
    private UUID categoryId;
    private UUID classificationId;
    private String cognitoSub;
    private Usuario usuario;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        timeSlotId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        classificationId = UUID.randomUUID();
        cognitoSub = "user-123";

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());

        alumno = new Alumno();
        alumno.setId(studentId);
    }

    @Test
    void execute_WithValidDataAndCategory_ShouldCreateRestriction() {
        CreateTimeRestrictionRequest request = new CreateTimeRestrictionRequest(
                studentId,
                timeSlotId,
                categoryId,
                null,
                cognitoSub
        );
        RestriccionHoraria saved = new RestriccionHoraria();
        saved.setId(UUID.randomUUID());
        saved.setAlumnoId(studentId);
        FranjaHoraria fh = new FranjaHoraria();
        fh.setId(timeSlotId);
        saved.setFranjaHoraria(fh);
        CategoriaProducto cp = new CategoriaProducto();
        cp.setId(categoryId);
        saved.setCategoria(cp);
        saved.setCreadoPorUsuarioId(usuario.getId());

        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(usuario));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(new ArrayList<>());
        when(repository.save(any(RestriccionHoraria.class))).thenReturn(saved);

        CreateTimeRestrictionResponse response = useCase.execute(request);

        assertNotNull(response);
        assertEquals(saved.getId(), response.id());
        assertEquals(timeSlotId, response.timeSlotId());
        assertEquals(categoryId, response.categoryId());
        verify(repository).save(any(RestriccionHoraria.class));
    }

    @Test
    void execute_WhenClassificationAlreadyExistsGlobally_ShouldThrowException() {
        CreateTimeRestrictionRequest request = new CreateTimeRestrictionRequest(
                studentId,
                timeSlotId,
                null,
                classificationId,
                cognitoSub
        );
        ClasificacionSalud globalRestriction = new ClasificacionSalud();
        globalRestriction.setId(classificationId);
        alumno.getRestriccionesSalud().add(globalRestriction);

        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(usuario));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("Student already has this health restriction assigned globally.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void execute_WhenRestrictionAlreadyExistsForTimeSlot_ShouldThrowException() {
        CreateTimeRestrictionRequest request = new CreateTimeRestrictionRequest(
                studentId,
                timeSlotId,
                categoryId,
                null,
                cognitoSub
        );
        RestriccionHoraria existing = new RestriccionHoraria();
        existing.setActiva(true);
        FranjaHoraria fh = new FranjaHoraria();
        fh.setId(timeSlotId);
        existing.setFranjaHoraria(fh);
        CategoriaProducto cp = new CategoriaProducto();
        cp.setId(categoryId);
        existing.setCategoria(cp);

        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(usuario));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(List.of(existing));

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("Student already has an active restriction for this category or type in the selected time slot.", ex.getMessage());
        verify(repository, never()).save(any());
    }
}
