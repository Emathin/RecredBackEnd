package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PurchaseSuggestIA {

    private final UsuarioService usuarioService;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final FranjaHorariaRepository franjaHorariaRepository;
    private final Clock clock;

    public PurchaseSuggestIA(UsuarioService usuarioService,
                             SugerenciaIARepository sugerenciaIARepository,
                             FranjaHorariaRepository franjaHorariaRepository,
                             Clock clock) {
        this.usuarioService = usuarioService;
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.franjaHorariaRepository = franjaHorariaRepository;
        this.clock = clock;
    }

    public Optional<SugerenciaIA> execute(String cognitoId) {
        Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoId);
        LocalDateTime now = LocalDateTime.now(clock);

        if (!cumpleReglasVisibilidad(usuario, now)) {
            return Optional.empty();
        }

        List<UUID> alumnosIds = getAlumnosIds(usuario);
        if (alumnosIds.isEmpty()) {
            return Optional.empty();
        }

        List<SugerenciaIAEntity> suggestions = new ArrayList<>();
        for (UUID alumnoId : alumnosIds) {
            suggestions.addAll(sugerenciaIARepository.findByAlumnoIdAndTipo(
                    alumnoId, TipoSugerenciaIA.RECOMENDACION_SALUDABLE));
        }

        return suggestions.stream()
                .filter(s -> s.getAceptada() == null)
                .max(Comparator.comparing(SugerenciaIAEntity::getCreadoEn))
                .map(SugerenciaIAEntity::toDomain);
    }

    private boolean cumpleReglasVisibilidad(Usuario usuario, LocalDateTime now) {
        return switch (usuario.getRol()) {
            case PADRE -> isVisibleParaPadre(now);
            // TODO: Configurar restricciones horarias reales para ALUMNO antes de producción.
            case ALUMNO -> true;
            default -> false;
        };
    }

    private boolean isVisibleParaPadre(LocalDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        boolean esDiaLaboral = day.getValue() >= DayOfWeek.MONDAY.getValue() &&
                               day.getValue() <= DayOfWeek.FRIDAY.getValue();
        return esDiaLaboral && now.getHour() >= 20;
    }

    private boolean isVisibleParaAlumno(Alumno alumno, LocalDateTime now) {
        if (alumno == null || alumno.getGrado() == null || alumno.getGrado().getColegio() == null) {
            return false;
        }
        return franjaHorariaRepository.findAllByColegioId(alumno.getGrado().getColegio().getId())
                .stream()
                .anyMatch(f -> f.isActivo() && f.estaDentroDeLaFranja(now));
    }

    private List<UUID> getAlumnosIds(Usuario usuario) {
        List<UUID> ids = new ArrayList<>();
        if (RolUsuario.ALUMNO.equals(usuario.getRol()) && usuario.getAlumno() != null) {
            ids.add(usuario.getAlumno().getId());
        } else if (RolUsuario.PADRE.equals(usuario.getRol()) && usuario.getTutor() != null) {
            usuario.getTutor().getHijos().forEach(h -> ids.add(h.getId()));
        }
        return ids;
    }
}
