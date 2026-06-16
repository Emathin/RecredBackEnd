package com.recred.backend.unit.config;

import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
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
}
