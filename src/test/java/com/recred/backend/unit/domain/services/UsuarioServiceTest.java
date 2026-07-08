package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoUserProfile;
import com.recred.backend.domain.services.UsuarioService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String COGNITO_SUBJECT = "cognito-sub";

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository);
    }

    @Test
    void updateProfileNormalizesFieldsAndKeepsNotProvidedFields() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));

        Usuario actualizado = usuarioService.updateProfile(
                COGNITO_SUBJECT,
                " Mateo ",
                null,
                " 112233 ",
                " 45238796 "
        );

        assertEquals("Mateo", actualizado.getNombre());
        assertEquals("Perez", actualizado.getApellido());
        assertEquals("112233", actualizado.getTelefono());
        assertEquals("45238796", actualizado.getDni());
        assertEquals(RolUsuario.PADRE, actualizado.getRol());
        assertNotNull(actualizado.getActualizadoEn());
        verify(usuarioRepository).updateProfile(
                eq(COGNITO_SUBJECT),
                eq("Mateo"),
                eq("Perez"),
                eq("112233"),
                eq("45238796"),
                any(LocalDateTime.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void updateProfileUpdatesLastNameWhenProvided() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));

        Usuario actualizado = usuarioService.updateProfile(
                COGNITO_SUBJECT,
                null,
                " Gomez ",
                null,
                null
        );

        assertEquals("Juan", actualizado.getNombre());
        assertEquals("Gomez", actualizado.getApellido());
        verify(usuarioRepository).updateProfile(
                eq(COGNITO_SUBJECT),
                eq("Juan"),
                eq("Gomez"),
                eq("115566"),
                eq("12345678"),
                any(LocalDateTime.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void updateProfileRejectsRequestWithoutFields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.updateProfile(COGNITO_SUBJECT, null, null, null, null)
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(usuarioRepository, never()).updateProfile(
                any(String.class),
                any(String.class),
                any(String.class),
                any(String.class),
                any(String.class),
                any(LocalDateTime.class));
    }

    @Test
    void updateProfileRejectsBlankFields() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));

        assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.updateProfile(COGNITO_SUBJECT, " ", null, null, null)
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(usuarioRepository, never()).updateProfile(
                any(String.class),
                any(String.class),
                any(String.class),
                any(String.class),
                any(String.class),
                any(LocalDateTime.class));
    }

    @Test
    void obtenerUsuarioActualRechazaUsuarioInexistente() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());

        assertThrows(
                UsuarioAutenticadoNoEncontradoException.class,
                () -> usuarioService.obtenerUsuarioActual(COGNITO_SUBJECT)
        );
    }

    @Test
    void sincronizarUsuarioCognitoReturnsExistingUser() {
        Jwt jwt = jwt(COGNITO_SUBJECT);
        Usuario usuario = usuario();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));

        Usuario result = usuarioService.sincronizarUsuarioCognito(jwt);

        assertEquals(usuario, result);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void sincronizarUsuarioCognitoCreatesUserUsingClaimsAndProfileFallbacks() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(COGNITO_SUBJECT)
                .claim("username", " usuario ")
                .claim("given_name", " ")
                .build();
        CognitoUserProfile profile = new CognitoUserProfile(
                "profile@example.com",
                "Nombre Perfil",
                "Apellido Perfil"
        );
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario result = usuarioService.sincronizarUsuarioCognito(jwt, profile);

        assertEquals(COGNITO_SUBJECT, result.getIdCognito());
        assertEquals("profile@example.com", result.getEmail());
        assertEquals("Nombre Perfil", result.getNombre());
        assertEquals("Apellido Perfil", result.getApellido());
        assertEquals(RolUsuario.PADRE, result.getRol());
        assertEquals(true, result.getAutenticadoConCognito());
        assertEquals(true, result.getActivo());
        assertNotNull(result.getCreadoEn());
        assertNotNull(result.getActualizadoEn());
    }

    @Test
    void sincronizarUsuarioCognitoUsesSubjectEmailFallback() {
        Jwt jwt = jwt("external-user");
        when(usuarioRepository.findByIdCognito("external-user")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario result = usuarioService.sincronizarUsuarioCognito(jwt, null);

        assertEquals("external-user@cognito.local", result.getEmail());
        assertEquals("Sin Nombre", result.getNombre());
        assertEquals("Sin Apellido", result.getApellido());
    }

    @Test
    void sincronizarUsuarioCognitoUsesUsernameAsEmailWhenItContainsAt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(COGNITO_SUBJECT)
                .claim("preferred_username", "persona@example.com")
                .build();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario result = usuarioService.sincronizarUsuarioCognito(jwt);

        assertEquals("persona@example.com", result.getEmail());
        assertEquals("persona@example.com", result.getNombre());
    }

    @Test
    void sincronizarUsuarioCognitoUsesGenericEmailFallbackWhenSubjectIsMissing() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(null);
        when(usuarioRepository.findByIdCognito(null)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario result = usuarioService.sincronizarUsuarioCognito(jwt);

        assertEquals("usuario-cognito@recred.local", result.getEmail());
        assertEquals("Sin Nombre", result.getNombre());
        assertEquals("Sin Apellido", result.getApellido());
    }

    @Test
    void actualizarRolNormalizesAndSavesRole() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario result = usuarioService.actualizarRol(COGNITO_SUBJECT, "vendedor");

        assertEquals(RolUsuario.VENDEDOR, result.getRol());
        assertNotNull(result.getActualizadoEn());
        verify(usuarioRepository).save(usuario);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setIdCognito(COGNITO_SUBJECT);
        usuario.setNombre("Juan");
        usuario.setApellido("Perez");
        usuario.setEmail("juan@example.com");
        usuario.setTelefono("115566");
        usuario.setDni("12345678");
        usuario.setRol(RolUsuario.PADRE);
        return usuario;
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }
}
