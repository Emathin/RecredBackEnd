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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTimeRestrictionTest {

    private static final String GLOBAL_HEALTH_RESTRICTION_MESSAGE =
            "Student already has this health restriction assigned globally.";
    private static final String OVERLAPPING_TIME_RESTRICTION_MESSAGE =
            "Student already has an active restriction "
                    + "for this category or type in the selected time slot.";

    @Mock
    private RestriccionHorariaRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlumnoRepository alumnoRepository;

    private CreateTimeRestriction useCase;

    private UUID studentId;
    private UUID timeSlotId;
    private UUID categoryId;
    private UUID classificationId;
    private UUID creatorId;
    private UUID savedRestrictionId;
    private String cognitoSub;
    private Usuario creator;
    private Alumno student;
    private CreateTimeRestrictionResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new CreateTimeRestriction(repository, usuarioRepository, alumnoRepository);
    }

    @Test
    void givenStudentWithoutRestrictions_whenCreatingCategoryRestriction_thenSavesRestriction() {
        givenCategoryRestrictionScenario();
        givenAuthenticatedUserExists();
        givenStudentExists();
        givenStudentHasNoTimeRestrictions();
        givenRestrictionWillBeSaved();

        whenCreatingCategoryRestriction();

        thenCategoryRestrictionWasSaved();
        thenCreatedCategoryRestrictionIsReturned();
    }

    @Test
    void givenGlobalHealthRestriction_whenCreatingClassificationRestriction_thenFails() {
        givenClassificationRestrictionScenario();
        givenAuthenticatedUserExists();
        givenStudentAlreadyHasGlobalHealthRestriction();

        whenCreatingClassificationRestrictionFails();

        thenRestrictionCreationFailsWith(GLOBAL_HEALTH_RESTRICTION_MESSAGE);
        thenRestrictionWasNotSaved();
    }

    @Test
    void givenOverlappingRestriction_whenCreatingCategoryRestriction_thenFails() {
        givenCategoryRestrictionScenario();
        givenAuthenticatedUserExists();
        givenStudentExists();
        givenStudentHasOverlappingCategoryRestriction();

        whenCreatingCategoryRestrictionFails();

        thenRestrictionCreationFailsWith(OVERLAPPING_TIME_RESTRICTION_MESSAGE);
        thenRestrictionWasNotSaved();
    }

    private void givenCategoryRestrictionScenario() {
        givenRestrictionScenario();
        classificationId = null;
    }

    private void givenClassificationRestrictionScenario() {
        givenRestrictionScenario();
        categoryId = null;
    }

    private void givenRestrictionScenario() {
        studentId = UUID.randomUUID();
        timeSlotId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        classificationId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        savedRestrictionId = UUID.randomUUID();
        cognitoSub = "user-123";

        creator = new Usuario();
        creator.setId(creatorId);

        student = new Alumno();
        student.setId(studentId);
    }

    private void givenAuthenticatedUserExists() {
        when(usuarioRepository.findByIdCognito(cognitoSub)).thenReturn(Optional.of(creator));
    }

    private void givenStudentExists() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentAlreadyHasGlobalHealthRestriction() {
        ClasificacionSalud globalRestriction = new ClasificacionSalud();
        globalRestriction.setId(classificationId);
        student.getRestriccionesSalud().add(globalRestriction);
        givenStudentExists();
    }

    private void givenStudentHasNoTimeRestrictions() {
        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(List.of());
    }

    private void givenStudentHasOverlappingCategoryRestriction() {
        RestriccionHoraria existingRestriction = new RestriccionHoraria(
                null,
                studentId,
                timeSlot(),
                category(),
                null
        );
        when(repository.findAllByAlumnoEntityId(studentId))
                .thenReturn(List.of(existingRestriction));
    }

    private void givenRestrictionWillBeSaved() {
        when(repository.save(any(RestriccionHoraria.class))).thenAnswer(invocation -> {
            RestriccionHoraria restriction = invocation.getArgument(0);
            restriction.setId(savedRestrictionId);
            return restriction;
        });
    }

    private void whenCreatingCategoryRestriction() {
        response = useCase.execute(categoryRestrictionRequest());
    }

    private void whenCreatingCategoryRestrictionFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                () -> useCase.execute(categoryRestrictionRequest()));
    }

    private void whenCreatingClassificationRestrictionFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                () -> useCase.execute(classificationRestrictionRequest()));
    }

    private void thenCategoryRestrictionWasSaved() {
        RestriccionHoraria savedRestriction = savedRestriction();
        assertEquals(studentId, savedRestriction.getAlumnoId());
        assertEquals(timeSlotId, savedRestriction.getFranjaHoraria().getId());
        assertEquals(categoryId, savedRestriction.getCategoria().getId());
        assertNull(savedRestriction.getClasificacionSalud());
        assertEquals(creatorId, savedRestriction.getCreadoPorUsuarioId());
    }

    private void thenCreatedCategoryRestrictionIsReturned() {
        assertEquals(savedRestrictionId, response.id());
        assertEquals(studentId, response.studentId());
        assertEquals(timeSlotId, response.timeSlotId());
        assertEquals(categoryId, response.categoryId());
        assertNull(response.classificationId());
        assertEquals(creatorId, response.creadoPorUsuarioId());
    }

    private void thenRestrictionCreationFailsWith(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenRestrictionWasNotSaved() {
        verify(repository, never()).save(any());
    }

    private RestriccionHoraria savedRestriction() {
        ArgumentCaptor<RestriccionHoraria> captor =
                ArgumentCaptor.forClass(RestriccionHoraria.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private CreateTimeRestrictionRequest categoryRestrictionRequest() {
        return new CreateTimeRestrictionRequest(
                studentId,
                timeSlotId,
                categoryId,
                null,
                cognitoSub
        );
    }

    private CreateTimeRestrictionRequest classificationRestrictionRequest() {
        return new CreateTimeRestrictionRequest(
                studentId,
                timeSlotId,
                null,
                classificationId,
                cognitoSub
        );
    }

    private FranjaHoraria timeSlot() {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(timeSlotId);
        return timeSlot;
    }

    private CategoriaProducto category() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        return category;
    }
}
