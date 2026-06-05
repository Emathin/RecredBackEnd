package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Usuario;
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

    public Optional<SugerenciaIAEntity> execute(String cognitoId) {
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
                .max(Comparator.comparing(SugerenciaIAEntity::getCreadoEn));
    }


    //REFACTORIZA LOS IF

    private boolean cumpleReglasVisibilidad(Usuario usuario, LocalDateTime now) {
        RolUsuario rol = usuario.getRol();
        if (RolUsuario.PADRE.equals(rol)) {
            DayOfWeek day = now.getDayOfWeek();
            return day.getValue() >= DayOfWeek.MONDAY.getValue() &&
                   day.getValue() <= DayOfWeek.FRIDAY.getValue() &&
                   now.getHour() >= 20;
        } else if (RolUsuario.ALUMNO.equals(rol)) {
            Alumno alumno = usuario.getAlumno();
            if (alumno == null || alumno.getGrado() == null
                    || alumno.getGrado().getColegio() == null) {
                return false;
            }
            UUID colegioId = alumno.getGrado().getColegio().getId();
            List<FranjaHoraria> franjas = franjaHorariaRepository.findAllByColegioId(colegioId);
            return franjas.stream().anyMatch(f -> f.isActivo() && f.estaDentroDeLaFranja(now));
        }
        return false;
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
