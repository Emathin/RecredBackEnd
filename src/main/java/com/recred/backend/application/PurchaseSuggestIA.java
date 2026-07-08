package com.recred.backend.application;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.services.UsuarioService;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PurchaseSuggestIA {

    private final UsuarioService usuarioService;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final Clock clock;

    public PurchaseSuggestIA(
            UsuarioService usuarioService,
            SugerenciaIARepository sugerenciaIARepository,
            Clock clock
    ) {
        this.usuarioService = usuarioService;
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.clock = clock;
    }

    public Optional<PurchaseSuggestionResponse> execute(
            PurchaseSuggestIARequest suggestionRequest
    ) {
        Usuario user = findCurrentUser(suggestionRequest.cognitoId());

        return findVisiblePurchaseSuggestionFor(user);
    }

    private Usuario findCurrentUser(String cognitoId) {
        return usuarioService.obtenerUsuarioActual(cognitoId);
    }

    private boolean canSeePurchaseSuggestions(Usuario user) {
        return switch (user.getRol()) {
            case PADRE -> isParentVisibilityWindow();
            case ALUMNO -> true;
            default -> false;
        };
    }

    private Optional<PurchaseSuggestionResponse> findVisiblePurchaseSuggestionFor(Usuario user) {
        if (!canSeePurchaseSuggestions(user)) {
            return Optional.empty();
        }

        List<UUID> studentIds = studentIdsFor(user);
        if (studentIds.isEmpty()) {
            return Optional.empty();
        }

        return findLatestPendingHealthySuggestion(studentIds);
    }

    private boolean isParentVisibilityWindow() {
        LocalDateTime now = LocalDateTime.now(clock);
        return isBusinessDay(now) && now.getHour() >= 20;
    }

    private boolean isBusinessDay(LocalDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        return day.getValue() >= DayOfWeek.MONDAY.getValue()
                && day.getValue() <= DayOfWeek.FRIDAY.getValue();
    }

    private List<UUID> studentIdsFor(Usuario user) {
        List<UUID> ids = new ArrayList<>();
        if (RolUsuario.ALUMNO.equals(user.getRol()) && user.getAlumno() != null) {
            ids.add(user.getAlumno().getId());
        } else if (RolUsuario.PADRE.equals(user.getRol()) && user.getTutor() != null) {
            user.getTutor().getHijos().forEach(student -> ids.add(student.getId()));
        }
        return ids;
    }

    private Optional<PurchaseSuggestionResponse> findLatestPendingHealthySuggestion(
            List<UUID> studentIds
    ) {
        return findLatestPendingHealthySuggestionFor(studentIds)
                .map(PurchaseSuggestionResponse::fromDomain);
    }

    private Optional<SugerenciaIA> findLatestPendingHealthySuggestionFor(List<UUID> studentIds) {
        return sugerenciaIARepository.findLatestPendingByAlumnoIdsAndTipo(
                studentIds,
                TipoSugerenciaIA.RECOMENDACION_SALUDABLE
        );
    }
}
