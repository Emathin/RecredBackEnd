package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.RegisterBuffet;
import com.recred.backend.application.RegisterBuffetRequest;
import com.recred.backend.application.RegisterBuffetResponse;
import com.recred.backend.application.RegisterSeller;
import com.recred.backend.application.RegisterSellerRequest;
import com.recred.backend.application.RegisterSellerResponse;
import com.recred.backend.infrastructure.web.SchoolAccessGuard;
import com.recred.backend.infrastructure.web.SchoolBuffetController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SchoolBuffetControllerTest {

    @Mock
    private RegisterBuffet registerBuffetUseCase;
    
    @Mock
    private RegisterSeller registerSellerUseCase;
    
    @Mock
    private SchoolAccessGuard accessGuard;
    
    @InjectMocks
    private SchoolBuffetController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void registerBuffet_shouldReturn200AndExecuteUseCase() throws Exception {
        UUID schoolId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        RegisterBuffetRequest req = new RegisterBuffetRequest(
                null, "Buffet Test", LocalDate.now().plusYears(1));

        doNothing().when(accessGuard).requireSchoolDirectorAccess(any(), eq(schoolId));
        
        when(registerBuffetUseCase.execute(any(RegisterBuffetRequest.class)))
                .thenReturn(new RegisterBuffetResponse(buffetId));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user");

        mockMvc.perform(post("/api/v1/colegios/{schoolId}/buffets", schoolId)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buffetId").value(buffetId.toString()));


        verify(accessGuard).requireSchoolDirectorAccess(any(), eq(schoolId));
        verify(registerBuffetUseCase).execute(any(RegisterBuffetRequest.class));
    }

    @Test
    void registerSeller_shouldReturn200AndExecuteUseCase() throws Exception {
        UUID buffetId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        RegisterSellerRequest req = new RegisterSellerRequest(
                null, "testuser", "test@test.com", "John", "Doe",
                "12345678", "5551234", "20123456789"
        );

        doNothing().when(accessGuard).requireBuffetDirectorAccess(any(), eq(buffetId));
        
        when(registerSellerUseCase.execute(any(RegisterSellerRequest.class)))
                .thenReturn(new RegisterSellerResponse(kiosqueroId, usuarioId));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user");

        mockMvc.perform(post("/api/v1/buffets/{buffetId}/sellers", buffetId)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kiosqueroId").value(kiosqueroId.toString()))
                .andExpect(jsonPath("$.usuarioId").value(usuarioId.toString()));


        verify(accessGuard).requireBuffetDirectorAccess(any(), eq(buffetId));
        verify(registerSellerUseCase).execute(any(RegisterSellerRequest.class));
    }
}
