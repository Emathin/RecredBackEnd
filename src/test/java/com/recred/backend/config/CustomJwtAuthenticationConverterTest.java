package com.recred.backend.config;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomJwtAuthenticationConverterTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    void convert_ShouldCacheUserAuthoritiesByCognitoSubject() {
        Usuario usuario = new Usuario();
        usuario.setRol(RolUsuario.VENDEDOR);
        when(usuarioRepository.findByIdCognito("cognito-sub"))
                .thenReturn(Optional.of(usuario));
        CustomJwtAuthenticationConverter converter = converter();
        Jwt jwt = jwt("cognito-sub");

        AbstractAuthenticationToken firstAuthentication = converter.convert(jwt);
        AbstractAuthenticationToken secondAuthentication = converter.convert(jwt);

        assertHasAuthority(firstAuthentication, "ROLE_VENDEDOR");
        assertHasAuthority(secondAuthentication, "ROLE_VENDEDOR");
        verify(usuarioRepository, times(1)).findByIdCognito("cognito-sub");
    }

    @Test
    void convert_WhenJwtHasNoSubject_ShouldNotQueryRepository() {
        CustomJwtAuthenticationConverter converter = converter();

        AbstractAuthenticationToken authentication = converter.convert(jwtWithoutSubject());

        assertTrue(authentication.getAuthorities().isEmpty());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void convert_WhenUserDoesNotExist_ShouldReturnEmptyAuthoritiesAndNotCache() {
        when(usuarioRepository.findByIdCognito("missing-sub")).thenReturn(Optional.empty());
        CustomJwtAuthenticationConverter converter = converter();

        AbstractAuthenticationToken firstAuthentication = converter.convert(jwt("missing-sub"));
        AbstractAuthenticationToken secondAuthentication = converter.convert(jwt("missing-sub"));

        assertTrue(firstAuthentication.getAuthorities().isEmpty());
        assertTrue(secondAuthentication.getAuthorities().isEmpty());
        verify(usuarioRepository, times(2)).findByIdCognito("missing-sub");
    }

    @Test
    void convert_WhenUserHasNoRole_ShouldReturnEmptyAuthorities() {
        Usuario usuario = new Usuario();
        when(usuarioRepository.findByIdCognito("cognito-sub"))
                .thenReturn(Optional.of(usuario));
        CustomJwtAuthenticationConverter converter = converter();

        AbstractAuthenticationToken authentication = converter.convert(jwt("cognito-sub"));

        assertTrue(authentication.getAuthorities().isEmpty());
    }

    @Test
    void convert_WhenCacheExpires_ShouldReloadAuthorities() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-12T10:00:00Z"));
        Usuario vendedor = new Usuario();
        vendedor.setRol(RolUsuario.VENDEDOR);
        Usuario padre = new Usuario();
        padre.setRol(RolUsuario.PADRE);
        when(usuarioRepository.findByIdCognito("cognito-sub"))
                .thenReturn(Optional.of(vendedor), Optional.of(padre));
        CustomJwtAuthenticationConverter converter =
                new CustomJwtAuthenticationConverter(usuarioRepository, clock);

        AbstractAuthenticationToken firstAuthentication = converter.convert(jwt("cognito-sub"));
        clock.advance(Duration.ofMinutes(5));
        AbstractAuthenticationToken secondAuthentication = converter.convert(jwt("cognito-sub"));

        assertHasAuthority(firstAuthentication, "ROLE_VENDEDOR");
        assertHasAuthority(secondAuthentication, "ROLE_PADRE");
        verify(usuarioRepository, times(2)).findByIdCognito("cognito-sub");
    }

    private CustomJwtAuthenticationConverter converter() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-12T10:00:00Z"), ZoneOffset.UTC);
        return new CustomJwtAuthenticationConverter(usuarioRepository, clock);
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }

    private Jwt jwtWithoutSubject() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "read")
                .build();
    }

    private void assertHasAuthority(
            AbstractAuthenticationToken authentication,
            String expectedAuthority
    ) {
        boolean hasAuthority = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(expectedAuthority));
        assertTrue(hasAuthority);
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
