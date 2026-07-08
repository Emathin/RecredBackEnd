package com.recred.backend.unit.application;

import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeRestrictionsByStudentRequest;
import com.recred.backend.application.GetTimeRestrictionsByStudentResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTimeRestrictionsByStudentTest {

    @Mock
    private RestriccionHorariaRepository repository;

    private GetTimeRestrictionsByStudent useCase;

    private UUID studentId;
    private UUID creatorId;
    private RestriccionHoraria restriction;
    private List<GetTimeRestrictionsByStudentResponse> response;

    @BeforeEach
    void setUp() {
        useCase = new GetTimeRestrictionsByStudent(repository);
    }

    @Test
    void givenStudentWithTimeRestrictions_whenGettingRestrictions_thenReturnsRestrictions() {
        givenTimeRestrictionsScenario();
        givenStudentHasSimpleRestriction();

        whenGettingTimeRestrictions();

        thenRestrictionIsReturned();
        thenRestrictionsWereRequestedForStudent();
    }

    @Test
    void givenStudentWithoutTimeRestrictions_whenGettingRestrictions_thenReturnsEmptyList() {
        givenTimeRestrictionsScenario();
        givenStudentHasNoRestrictions();

        whenGettingTimeRestrictions();

        thenNoRestrictionsAreReturned();
        thenRestrictionsWereRequestedForStudent();
    }

    @Test
    void givenRestrictionWithNestedModels_whenGettingRestrictions_thenMapsCompleteResponse() {
        givenTimeRestrictionsScenario();
        givenStudentHasCompleteRestriction();

        whenGettingTimeRestrictions();

        thenRestrictionStateIsReturned();
        thenTimeSlotIsReturned();
        thenCategoryIsReturned();
        thenHealthClassificationIsReturned();
    }

    private void givenTimeRestrictionsScenario() {
        studentId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
    }

    private void givenStudentHasSimpleRestriction() {
        restriction = new RestriccionHoraria();
        restriction.setId(UUID.randomUUID());
        restriction.setAlumnoId(studentId);
        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(List.of(restriction));
    }

    private void givenStudentHasNoRestrictions() {
        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(List.of());
    }

    private void givenStudentHasCompleteRestriction() {
        restriction = restriction(studentId, creatorId);
        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(List.of(restriction));
    }

    private void whenGettingTimeRestrictions() {
        response = useCase.execute(new GetTimeRestrictionsByStudentRequest(studentId));
    }

    private void thenRestrictionIsReturned() {
        assertEquals(1, response.size());
        assertEquals(restriction.getId(), response.get(0).id());
        assertEquals(studentId, response.get(0).alumnoId());
    }

    private void thenNoRestrictionsAreReturned() {
        assertEquals(0, response.size());
    }

    private void thenRestrictionsWereRequestedForStudent() {
        verify(repository).findAllByAlumnoEntityId(studentId);
    }

    private void thenRestrictionStateIsReturned() {
        GetTimeRestrictionsByStudentResponse restrictionResponse = response.get(0);
        assertFalse(restrictionResponse.activa());
        assertEquals(creatorId, restrictionResponse.creadoPorUsuarioId());
    }

    private void thenTimeSlotIsReturned() {
        GetTimeRestrictionsByStudentResponse.FranjaHorariaResponse timeSlot =
                response.get(0).franjaHoraria();
        assertEquals(restriction.getFranjaHoraria().getId(), timeSlot.id());
        assertEquals("Primer recreo", timeSlot.descripcion());
        assertEquals(30, timeSlot.cupoMaximo());
        assertEquals(10, timeSlot.minutosCorte());
    }

    private void thenCategoryIsReturned() {
        GetTimeRestrictionsByStudentResponse.CategoriaProductoResponse category =
                response.get(0).categoria();
        assertEquals(restriction.getCategoria().getId(), category.id());
        assertEquals("Snacks", category.descripcion());
    }

    private void thenHealthClassificationIsReturned() {
        GetTimeRestrictionsByStudentResponse.ClasificacionSaludResponse classification =
                response.get(0).clasificacionSalud();
        assertEquals(restriction.getClasificacionSalud().getId(), classification.id());
        assertEquals("Sin TACC", classification.descripcion());
    }

    private RestriccionHoraria restriction(UUID studentId, UUID creatorId) {
        RestriccionHoraria restriction = new RestriccionHoraria();
        restriction.setId(UUID.randomUUID());
        restriction.setAlumnoId(studentId);
        restriction.setFranjaHoraria(timeSlot());
        restriction.setCategoria(category());
        restriction.setClasificacionSalud(healthClassification());
        restriction.setActiva(false);
        restriction.setCreadoPorUsuarioId(creatorId);
        return restriction;
    }

    private FranjaHoraria timeSlot() {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(UUID.randomUUID());
        timeSlot.setColegioId(UUID.randomUUID());
        timeSlot.setDescripcion("Primer recreo");
        timeSlot.setHoraInicio(LocalTime.of(9, 0));
        timeSlot.setHoraFin(LocalTime.of(9, 30));
        timeSlot.setActivo(true);
        timeSlot.setCupoMaximo(30);
        timeSlot.setMinutosCorte(10);
        return timeSlot;
    }

    private CategoriaProducto category() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Snacks");
        category.setActivo(true);
        return category;
    }

    private ClasificacionSalud healthClassification() {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(UUID.randomUUID());
        classification.setDescripcion("Sin TACC");
        classification.setActivo(true);
        return classification;
    }
}
