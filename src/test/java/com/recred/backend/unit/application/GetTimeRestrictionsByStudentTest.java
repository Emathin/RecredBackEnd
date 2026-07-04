package com.recred.backend.unit.application;

import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeRestrictionsByStudentRequest;
import com.recred.backend.application.GetTimeRestrictionsByStudentResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTimeRestrictionsByStudentTest {

    @Mock
    private RestriccionHorariaRepository repository;

    @InjectMocks
    private GetTimeRestrictionsByStudent getTimeRestrictionsByStudent;

    @Test
    void execute_DevuelveListaDeRestriccionesDelAlumno() {
        UUID studentId = UUID.randomUUID();
        RestriccionHoraria restriccion = new RestriccionHoraria();
        restriccion.setId(UUID.randomUUID());
        restriccion.setAlumnoId(studentId);
        List<RestriccionHoraria> restriccionesEsperadas = List.of(restriccion);

        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(restriccionesEsperadas);

        List<GetTimeRestrictionsByStudentResponse> resultado =
                getTimeRestrictionsByStudent.execute(new GetTimeRestrictionsByStudentRequest(studentId));

        assertEquals(1, resultado.size());
        assertEquals(restriccion.getId(), resultado.get(0).id());
        assertEquals(studentId, resultado.get(0).alumnoId());
        verify(repository).findAllByAlumnoEntityId(studentId);
    }

    @Test
    void execute_CuandoNoHayRestricciones_RetornaListaVacia() {
        UUID studentId = UUID.randomUUID();
        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(List.of());

        List<GetTimeRestrictionsByStudentResponse> resultado =
                getTimeRestrictionsByStudent.execute(new GetTimeRestrictionsByStudentRequest(studentId));

        assertEquals(0, resultado.size());
        verify(repository).findAllByAlumnoEntityId(studentId);
    }

    @Test
    void execute_WhenRestrictionHasNestedModels_ShouldMapCompleteResponse() {
        UUID studentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        RestriccionHoraria restriction = restriction(studentId, creatorId);
        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(List.of(restriction));

        List<GetTimeRestrictionsByStudentResponse> result =
                getTimeRestrictionsByStudent.execute(
                        new GetTimeRestrictionsByStudentRequest(studentId));

        GetTimeRestrictionsByStudentResponse response = result.get(0);
        assertEquals(false, response.activa());
        assertEquals(creatorId, response.creadoPorUsuarioId());
        assertEquals(restriction.getFranjaHoraria().getId(), response.franjaHoraria().id());
        assertEquals("Primer recreo", response.franjaHoraria().descripcion());
        assertEquals(30, response.franjaHoraria().cupoMaximo());
        assertEquals(10, response.franjaHoraria().minutosCorte());
        assertEquals(restriction.getCategoria().getId(), response.categoria().id());
        assertEquals("Snacks", response.categoria().descripcion());
        assertEquals(restriction.getClasificacionSalud().getId(),
                response.clasificacionSalud().id());
        assertEquals("Sin TACC", response.clasificacionSalud().descripcion());
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
