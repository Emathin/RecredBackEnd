package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileControllerTest {

    private UsuarioService usuarioService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserProfileController(usuarioService))
                .build();
    }

    @Test
    void getMyUserReturnsMinimalSessionData() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario usuario = usuario(userId);
        when(usuarioService.obtenerUsuarioActual("cognito-sub")).thenReturn(usuario);

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("mateo@example.com"))
                .andExpect(jsonPath("$.firstName").value("Mateo"))
                .andExpect(jsonPath("$.lastName").value("Fortuna"))
                .andExpect(jsonPath("$.role").value("PADRE"))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.documentNumber").doesNotExist());

        verify(usuarioService).obtenerUsuarioActual("cognito-sub");
    }

    @Test
    void getMyProfileReturnsFullProfileData() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario usuario = usuario(userId);
        when(usuarioService.obtenerUsuarioActual("cognito-sub")).thenReturn(usuario);

        mockMvc.perform(get("/api/v1/users/me/profile")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("mateo@example.com"))
                .andExpect(jsonPath("$.firstName").value("Mateo"))
                .andExpect(jsonPath("$.lastName").value("Fortuna"))
                .andExpect(jsonPath("$.phone").value("112233"))
                .andExpect(jsonPath("$.documentNumber").value("45238796"))
                .andExpect(jsonPath("$.role").value("PADRE"));

        verify(usuarioService).obtenerUsuarioActual("cognito-sub");
    }

    @Test
    void updateMyProfileDelegatesToServiceAndReturnsUpdatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario usuario = usuario(userId);
        when(usuarioService.updateProfile(
                eq("cognito-sub"),
                eq("Mateo"),
                eq("Fortuna"),
                eq("112233"),
                eq("45238796")
        )).thenReturn(usuario);

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Mateo",
                                  "lastName": "Fortuna",
                                  "phone": "112233",
                                  "documentNumber": "45238796"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Mateo"))
                .andExpect(jsonPath("$.lastName").value("Fortuna"))
                .andExpect(jsonPath("$.phone").value("112233"))
                .andExpect(jsonPath("$.documentNumber").value("45238796"));

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(usuarioService).updateProfile(
                subjectCaptor.capture(),
                eq("Mateo"),
                eq("Fortuna"),
                eq("112233"),
                eq("45238796")
        );
        assertEquals("cognito-sub", subjectCaptor.getValue());
    }

    private Usuario usuario(UUID userId) {
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setEmail("mateo@example.com");
        usuario.setNombre("Mateo");
        usuario.setApellido("Fortuna");
        usuario.setTelefono("112233");
        usuario.setDni("45238796");
        usuario.setRol(RolUsuario.PADRE);
        return usuario;
    }

    private Authentication authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("cognito-sub")
                .issuedAt(Instant.parse("2026-05-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-28T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
