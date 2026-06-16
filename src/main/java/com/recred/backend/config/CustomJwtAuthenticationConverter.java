package com.recred.backend.config;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CustomJwtAuthenticationConverter implements
        Converter<Jwt, AbstractAuthenticationToken> {

    private static final Duration AUTH_CACHE_TTL = Duration.ofMinutes(5);

    private final UsuarioRepository usuarioRepository;
    private final Clock clock;
    private final Map<String, CachedAuthorities> authoritiesCache = new ConcurrentHashMap<>();

    @Autowired
    public CustomJwtAuthenticationConverter(UsuarioRepository usuarioRepository) {
        this(usuarioRepository, Clock.systemUTC());
    }

    public CustomJwtAuthenticationConverter(UsuarioRepository usuarioRepository, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String cognitoId = jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities(cognitoId));
    }

    private List<GrantedAuthority> authorities(String cognitoId) {
        if (cognitoId == null) {
            return List.of();
        }

        CachedAuthorities cachedAuthorities = authoritiesCache.get(cognitoId);
        if (cachedAuthorities != null && !cachedAuthorities.isExpired(now())) {
            return cachedAuthorities.authorities();
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByIdCognito(cognitoId);
        List<GrantedAuthority> authorities = userAuthorities(usuarioOpt);
        updateCache(cognitoId, usuarioOpt, authorities);
        return authorities;
    }

    private List<GrantedAuthority> userAuthorities(Optional<Usuario> usuarioOpt) {
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getRol() == null) {
            return List.of();
        }

        String authority = "ROLE_" + usuarioOpt.get().getRol().name();
        return List.of(new SimpleGrantedAuthority(authority));
    }

    private void updateCache(
            String cognitoId,
            Optional<Usuario> usuarioOpt,
            List<GrantedAuthority> authorities
    ) {
        if (usuarioOpt.isEmpty()) {
            authoritiesCache.remove(cognitoId);
            return;
        }

        authoritiesCache.put(cognitoId, new CachedAuthorities(
                authorities,
                now().plus(AUTH_CACHE_TTL)
        ));
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private record CachedAuthorities(
            List<GrantedAuthority> authorities,
            Instant expiresAt
    ) {
        boolean isExpired(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }
}
