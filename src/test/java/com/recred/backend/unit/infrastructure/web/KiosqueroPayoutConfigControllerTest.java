package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.application.GetKiosqueroPayoutConfig;
import com.recred.backend.application.GetKiosqueroPayoutConfigRequest;
import com.recred.backend.application.GetKiosqueroPayoutConfigResponse;
import com.recred.backend.application.SaveKiosqueroPayoutConfig;
import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.infrastructure.web.KiosqueroPayoutConfigController;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KiosqueroPayoutConfigController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class KiosqueroPayoutConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SaveKiosqueroPayoutConfig saveUseCase;

    @MockitoBean
    private GetKiosqueroPayoutConfig getUseCase;

    @MockitoBean
    private KiosqueroRepository kiosqueroRepository;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void saveConfig_WhenValidRequest_ShouldReturnOk() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();

        String jsonRequest = """
                {
                    "destinationCvu": "1234567890123456789012",
                    "destinationCuit": "20123456789",
                    "accountHolderName": "John Doe",
                    "cantidadIntervalo": 1,
                    "unidadIntervalo": "DAYS",
                    "estado": "ACTIVE"
                }
                """;

        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(usuarioId))
                .thenReturn(Optional.of(kiosqueroId));

        mockMvc.perform(post("/api/v1/kiosqueros/{usuarioId}/payout-config", usuarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated());
    }

    @Test
    void saveConfig_WhenKiosqueroNotFound_ShouldReturnBadRequest() throws Exception {
        UUID usuarioId = UUID.randomUUID();

        String jsonRequest = """
                {
                    "destinationCvu": "1234567890123456789012",
                    "destinationCuit": "20123456789",
                    "accountHolderName": "John Doe",
                    "cantidadIntervalo": 1,
                    "unidadIntervalo": "DAYS",
                    "estado": "ACTIVE"
                }
                """;

        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(usuarioId))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/kiosqueros/{usuarioId}/payout-config", usuarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getConfig_WhenExists_ShouldReturnConfig() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();

        GetKiosqueroPayoutConfigResponse response = new GetKiosqueroPayoutConfigResponse(
                kiosqueroId,
                "1234567890123456789012",
                "20123456789",
                "John Doe",
                1,
                IntervalUnit.DAYS,
                PayoutScheduleStatus.ACTIVE,
                LocalDate.of(2026, 7, 2),
                null
        );

        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(usuarioId))
                .thenReturn(Optional.of(kiosqueroId));

        when(getUseCase.execute(any(GetKiosqueroPayoutConfigRequest.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/kiosqueros/{usuarioId}/payout-config", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinationCvu").value("1234567890123456789012"))
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"));
    }

    @Test
    void getConfig_WhenKiosqueroNotFound_ShouldReturnBadRequest() throws Exception {
        UUID usuarioId = UUID.randomUUID();

        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(usuarioId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/kiosqueros/{usuarioId}/payout-config", usuarioId))
                .andExpect(status().isBadRequest());
    }
}
