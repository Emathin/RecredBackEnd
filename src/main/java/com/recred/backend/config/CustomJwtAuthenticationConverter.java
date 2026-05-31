package com.recred.backend.config;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class CustomJwtAuthenticationConverter implements
        Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    public CustomJwtAuthenticationConverter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String cognitoId = jwt.getSubject();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByIdCognito(cognitoId);

        List<GrantedAuthority> authorities = Collections.emptyList();

        if (usuarioOpt.isPresent() && usuarioOpt.get().getRol() != null) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_" +
                    usuarioOpt.get().getRol().name()));
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }
}