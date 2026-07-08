package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.EstadoLicenciaColegio;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class GetSchoolOverview {

    private final UsuarioRepository usuarioRepository;
    private final ColegioRepository colegioRepository;
    private final BuffetRepository buffetRepository;
    private final SuscripcionColegioRepository subscriptionRepository;
    private final Clock clock;

    public GetSchoolOverview(UsuarioRepository usuarioRepository, 
                             ColegioRepository colegioRepository, 
                             BuffetRepository buffetRepository,
                             SuscripcionColegioRepository subscriptionRepository,
                             Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.colegioRepository = colegioRepository;
        this.buffetRepository = buffetRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.clock = clock;
    }

    public GetSchoolOverviewResponse execute(GetSchoolOverviewRequest request) {
        UUID schoolId = schoolId(authenticatedUser(request.cognitoSubject()));
        return response(school(schoolId), buffets(schoolId), licencia(schoolId));
    }

    private Usuario authenticatedUser(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(
                        cognitoSubject));
    }

    private UUID schoolId(Usuario usuario) {
        if (usuario.getColegio() == null) {
            throw new IllegalArgumentException("El usuario no tiene un colegio asociado.");
        }

        return usuario.getColegio().getId();
    }

    private Colegio school(UUID schoolId) {
        return colegioRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));
    }

    private List<GetSchoolOverviewResponse.BuffetDto> buffets(UUID schoolId) {
        return buffetRepository.findByColegioId(schoolId).stream()
                .map(this::mapToBuffetDto)
                .toList();
    }

    private GetSchoolOverviewResponse response(
            Colegio colegio,
            List<GetSchoolOverviewResponse.BuffetDto> buffets,
            GetSchoolOverviewResponse.LicenciaDto licencia
    ) {
        return new GetSchoolOverviewResponse(
                colegio.getId(),
                colegio.getNombre(),
                colegio.getCue(),
                licencia,
                buffets
        );
    }

    private GetSchoolOverviewResponse.LicenciaDto licencia(UUID schoolId) {
        return subscriptionRepository.findActiveByColegioId(schoolId)
                .map(this::licencia)
                .orElseGet(this::sinLicencia);
    }

    private GetSchoolOverviewResponse.LicenciaDto licencia(SuscripcionColegio subscription) {
        return new GetSchoolOverviewResponse.LicenciaDto(
                subscription.estadoLicenciaAt(now()),
                subscription.getFechaInicio(),
                subscription.getFechaVencimiento(),
                subscription.getFechaGraciaHasta(),
                subscription.getPrecio(),
                subscription.getMoneda()
        );
    }

    private GetSchoolOverviewResponse.LicenciaDto sinLicencia() {
        return new GetSchoolOverviewResponse.LicenciaDto(
                EstadoLicenciaColegio.SIN_LICENCIA,
                null,
                null,
                null,
                null,
                "USD"
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private GetSchoolOverviewResponse.BuffetDto mapToBuffetDto(Buffet buffet) {
        GetSchoolOverviewResponse.VendedorDto vendedorDto = null;
        if (buffet.getVendedor() != null && buffet.getVendedor().getUsuario() != null) {
            Kiosquero kiosquero = buffet.getVendedor();
            Usuario vendedorUsuario = kiosquero.getUsuario();
            vendedorDto = new GetSchoolOverviewResponse.VendedorDto(
                    kiosquero.getId(),
                    vendedorUsuario.getNombre(),
                    vendedorUsuario.getApellido(),
                    vendedorUsuario.getEmail(),
                    kiosquero.getCuit()
            );
        }

        return new GetSchoolOverviewResponse.BuffetDto(
                buffet.getId(),
                buffet.getNombre(),
                buffet.getActivo(),
                vendedorDto
        );
    }
}
