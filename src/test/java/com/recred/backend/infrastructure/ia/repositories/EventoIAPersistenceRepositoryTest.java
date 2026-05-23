package com.recred.backend.infrastructure.ia.repositories;

import com.recred.backend.control_parental.domain.enums.PeriodoPresupuesto;
import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.models.PrediccionGastoResultado;
import com.recred.backend.domain.models.TipoEventoIA;
import com.recred.backend.infrastructure.ia.entities.EventoIAEntity;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoIAPersistenceRepositoryTest {

    @Mock
    private EventoIAJpaRepository eventoIAJpaRepository;

    @Mock
    private EntityManager entityManager;

    @Test
    void registrarEventoDePrediccionConEntradaYResultado() {
        UUID alumnoId = UUID.randomUUID();
        AlumnoEntity alumnoEntity = new AlumnoEntity();
        when(entityManager.getReference(AlumnoEntity.class, alumnoId)).thenReturn(alumnoEntity);
        EventoIAPersistenceRepository repository = new EventoIAPersistenceRepository(
                eventoIAJpaRepository,
                entityManager
        );

        repository.registrarPrediccion(resultado(alumnoId));

        ArgumentCaptor<EventoIAEntity> captor = ArgumentCaptor.forClass(EventoIAEntity.class);
        verify(eventoIAJpaRepository).save(captor.capture());
        EventoIAEntity evento = captor.getValue();
        assertEquals(TipoEventoIA.PREDICCION_GASTO, evento.getTipo());
        assertSame(alumnoEntity, evento.getAlumnoEntity());
        assertEquals("BACKEND_PREDICCION_GASTO", evento.getOrigenEntrada());
        assertEquals(new BigDecimal("0.8000"), evento.getConfianza());
        assertEquals("MENSUAL", evento.getDatosEntrada().get("periodo"));
        assertEquals(new BigDecimal("120.00"), evento.getDatosResultado().get("gastoPredicho"));
        assertEquals(
                new BigDecimal("80.00"),
                evento.getDatosResultado().get("porcentajePresupuesto")
        );

        List<Map<String, Object>> categorias = categorias(evento);
        assertEquals("Bebidas", categorias.get(0).get("descripcion"));
        assertEquals(new BigDecimal("40.00"), categorias.get(0).get("montoTotal"));

        Map<String, Object> analisisIa = analisisIa(evento);
        assertEquals("Sube el gasto.", analisisIa.get("resumen"));
        assertEquals("gemini-test", analisisIa.get("modelo"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> categorias(EventoIAEntity evento) {
        return (List<Map<String, Object>>) evento.getDatosResultado()
                .get("categoriasMasConsumidas");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> analisisIa(EventoIAEntity evento) {
        return (Map<String, Object>) evento.getDatosResultado().get("analisisIa");
    }

    private PrediccionGastoResultado resultado(UUID alumnoId) {
        return new PrediccionGastoResultado(
                prediccion(alumnoId),
                Optional.of(new AnalisisPrediccionGasto(
                        "Sube el gasto.",
                        List.of("Alerta."),
                        List.of("Revisar presupuesto."),
                        "gemini-test"
                ))
        );
    }

    private PrediccionGasto prediccion(UUID alumnoId) {
        return new PrediccionGasto(
                alumnoId,
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("50.00"),
                new BigDecimal("120.00"),
                new BigDecimal("10.00"),
                new BigDecimal("150.00"),
                new BigDecimal("80.00"),
                new BigDecimal("0.8000"),
                60,
                7,
                List.of(new CategoriaConsumo("Bebidas", new BigDecimal("40.00")))
        );
    }
}
