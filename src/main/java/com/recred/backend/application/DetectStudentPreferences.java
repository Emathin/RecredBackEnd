package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.CompraRepository;
import com.recred.backend.domain.repositories.InventarioRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class DetectStudentPreferences {

    private final AlumnoRepository alumnoRepository;
    private final PreferenceAIRepository preferenceAIRepository;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final CompraRepository compraRepository;
    private final InventarioRepository inventarioRepository;

    public DetectStudentPreferences(
            AlumnoRepository alumnoRepository,
            PreferenceAIRepository preferenceAIRepository,
            SugerenciaIARepository sugerenciaIARepository,
            CompraRepository compraRepository,
            InventarioRepository inventarioRepository) {
        this.alumnoRepository = alumnoRepository;
        this.preferenceAIRepository = preferenceAIRepository;
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.compraRepository = compraRepository;
        this.inventarioRepository = inventarioRepository;
    }

    @Transactional
    public Optional<PreferenciaDetectada> execute(UUID alumnoId, UUID buffetId) {
        try {
            Alumno alumno = alumnoRepository.findById(alumnoId)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + alumnoId));

            List<PreferenceAIRepository.CompraResumen> historial =
                    compraRepository.findLastPurchasesByAlumno(alumnoId, 20);

            List<Producto> stockDisponible =
                    inventarioRepository.findAvailableProductsByBuffet(buffetId);

            if (stockDisponible.isEmpty()) {
                return Optional.empty();
            }

            Optional<PreferenciaDetectada> resultado = 
                    preferenceAIRepository.analizarPreferencias(alumno, historial, stockDisponible);

            resultado.ifPresent(preferencia -> {
                SugerenciaIAEntity sugerencia = new SugerenciaIAEntity();
                sugerencia.setTipo(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
                sugerencia.setTitulo(preferencia.titulo());
                sugerencia.setMensaje(preferencia.mensaje());
                sugerencia.setAceptada(null);

                sugerencia.setMetadata(Map.of(
                        "productoId", preferencia.productoId(),
                        "razonIA", preferencia.razonIA()
                ));

                AlumnoEntity alumnoEntity = new AlumnoEntity();
                alumnoEntity.setId(alumnoId);
                sugerencia.setAlumnoEntity(alumnoEntity);

                BuffetEntity buffetEntity = new BuffetEntity();
                buffetEntity.setId(buffetId);
                sugerencia.setBuffetEntity(buffetEntity);

                sugerenciaIARepository.guardar(sugerencia);
            });

            return resultado;

        } catch (Exception e) {
            throw e;
        }
    }
}
