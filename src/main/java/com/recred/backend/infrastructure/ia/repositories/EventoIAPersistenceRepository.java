package com.recred.backend.infrastructure.ia.repositories;

import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.models.PrediccionGastoResultado;
import com.recred.backend.domain.models.TipoEventoIA;
import com.recred.backend.domain.repositories.EventoIARepository;
import com.recred.backend.infrastructure.ia.entities.EventoIAEntity;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EventoIAPersistenceRepository implements EventoIARepository {

    private static final String ORIGEN_ENTRADA = "BACKEND_PREDICCION_GASTO";

    private final EventoIAJpaRepository eventoIAJpaRepository;
    private final EntityManager entityManager;

    public EventoIAPersistenceRepository(
            EventoIAJpaRepository eventoIAJpaRepository,
            EntityManager entityManager
    ) {
        this.eventoIAJpaRepository = eventoIAJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void registrarPrediccion(PrediccionGastoResultado resultado) {
        PrediccionGasto prediccionGasto = resultado.prediccionGasto();
        EventoIAEntity eventoIAEntity = new EventoIAEntity();
        eventoIAEntity.setTipo(TipoEventoIA.PREDICCION_GASTO);
        eventoIAEntity.setAlumnoEntity(entityManager.getReference(
                AlumnoEntity.class,
                prediccionGasto.alumnoId()
        ));
        eventoIAEntity.setOrigenEntrada(ORIGEN_ENTRADA);
        eventoIAEntity.setDatosEntrada(datosEntrada(prediccionGasto));
        eventoIAEntity.setDatosResultado(datosResultado(resultado));
        eventoIAEntity.setConfianza(prediccionGasto.confianza());

        eventoIAJpaRepository.save(eventoIAEntity);
    }

    private Map<String, Object> datosEntrada(PrediccionGasto prediccionGasto) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("periodo", prediccionGasto.periodo().name());
        datos.put("fechaCalculo", prediccionGasto.fechaCalculo().toString());
        datos.put("fechaInicio", prediccionGasto.fechaInicio().toString());
        datos.put("fechaFin", prediccionGasto.fechaFin().toString());
        return datos;
    }

    private Map<String, Object> datosResultado(PrediccionGastoResultado resultado) {
        PrediccionGasto prediccionGasto = resultado.prediccionGasto();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("gastoActual", prediccionGasto.gastoActual());
        datos.put("gastoPredicho", prediccionGasto.gastoPredicho());
        datos.put("promedioGastoDiario", prediccionGasto.promedioGastoDiario());
        datos.put("montoLimite", prediccionGasto.montoLimite());
        datos.put("porcentajePresupuesto", prediccionGasto.porcentajePresupuesto());
        datos.put("diasHistoricosUsados", prediccionGasto.diasHistoricosUsados());
        datos.put("diasRestantes", prediccionGasto.diasRestantes());
        datos.put("categoriasMasConsumidas", mapearCategorias(
                prediccionGasto.categoriasMasConsumidas()));
        resultado.analisisIa().ifPresent(analisis -> datos.put("analisisIa", mapearAnalisis(
                analisis)));
        return datos;
    }

    private Map<String, Object> mapearAnalisis(AnalisisPrediccionGasto analisis) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("resumen", analisis.resumen());
        datos.put("alertas", analisis.alertas());
        datos.put("recomendaciones", analisis.recomendaciones());
        datos.put("modelo", analisis.modelo());
        return datos;
    }

    private List<Map<String, Object>> mapearCategorias(List<CategoriaConsumo> categorias) {
        return categorias.stream()
                .map(this::mapearCategoria)
                .toList();
    }

    private Map<String, Object> mapearCategoria(CategoriaConsumo categoriaConsumo) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("descripcion", categoriaConsumo.descripcion());
        datos.put("montoTotal", categoriaConsumo.montoTotal());
        return datos;
    }
}
