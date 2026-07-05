package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetSchoolOverview {

    private final UsuarioRepository usuarioRepository;
    private final ColegioRepository colegioRepository;
    private final BuffetRepository buffetRepository;

    public GetSchoolOverview(UsuarioRepository usuarioRepository, 
                             ColegioRepository colegioRepository, 
                             BuffetRepository buffetRepository) {
        this.usuarioRepository = usuarioRepository;
        this.colegioRepository = colegioRepository;
        this.buffetRepository = buffetRepository;
    }

    public GetSchoolOverviewResponse execute(GetSchoolOverviewRequest request) {
        Usuario usuario = usuarioRepository.findByIdCognito(request.cognitoSubject())
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(
                        request.cognitoSubject()));
        
        if (usuario.getColegio() == null) {
            throw new IllegalArgumentException("El usuario no tiene un colegio asociado.");
        }
        
        UUID schoolId = usuario.getColegio().getId();

        Colegio colegio = colegioRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        List<Buffet> buffets = buffetRepository.findByColegioId(schoolId);

        List<GetSchoolOverviewResponse.BuffetDto> buffetDtos = buffets.stream()
                .map(this::mapToBuffetDto)
                .collect(Collectors.toList());

        return new GetSchoolOverviewResponse(
                colegio.getId(),
                colegio.getNombre(),
                colegio.getCue(),
                buffetDtos
        );
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
