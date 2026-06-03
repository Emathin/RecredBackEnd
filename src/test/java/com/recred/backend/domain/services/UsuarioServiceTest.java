package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void updateProfileRejectsRequestWithoutFields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.updateProfile(COGNITO_SUBJECT, null, null, null, null)
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
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
    }

    @Test
    void obtenerUsuarioActualRechazaUsuarioInexistente() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());

        assertThrows(
                UsuarioAutenticadoNoEncontradoException.class,
                () -> usuarioService.obtenerUsuarioActual(COGNITO_SUBJECT)
        );
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
}
