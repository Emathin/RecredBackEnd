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
import org.springframework.test.web.servlet.ResultActions;

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

    private final UUID USUARIO_ID = UUID.randomUUID();
    private final UUID KIOSQUERO_ID = UUID.randomUUID();
    
    private final String JSON_REQUEST = """
            {
                "destinationCvu": "1234567890123456789012",
                "destinationCuit": "20123456789",
                "accountHolderName": "John Doe",
                "cantidadIntervalo": 1,
                "unidadIntervalo": "DAYS",
                "estado": "ACTIVE"
            }
            """;

    @Test
    void saveConfigShouldReturnCreatedWhenValidRequest() throws Exception {
        givenKiosqueroExists();
        
        ResultActions response = whenSavingConfig();
        
        thenStatusIsCreated(response);
    }

    @Test
    void saveConfigShouldReturnBadRequestWhenKiosqueroNotFound() throws Exception {
        givenKiosqueroDoesNotExist();
        
        ResultActions response = whenSavingConfig();
        
        thenStatusIsBadRequest(response);
    }

    @Test
    void getConfigShouldReturnConfigWhenExists() throws Exception {
        givenKiosqueroExists();
        givenConfigExists();
        
        ResultActions response = whenGettingConfig();
        
        thenConfigIsReturned(response);
    }

    @Test
    void getConfigShouldReturnBadRequestWhenKiosqueroNotFound() throws Exception {
        givenKiosqueroDoesNotExist();
        
        ResultActions response = whenGettingConfig();
        
        thenStatusIsBadRequest(response);
    }

    private void givenKiosqueroExists() {
        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(USUARIO_ID))
                .thenReturn(Optional.of(KIOSQUERO_ID));
    }

    private void givenKiosqueroDoesNotExist() {
        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(USUARIO_ID))
                .thenReturn(Optional.empty());
    }

    private void givenConfigExists() {
        GetKiosqueroPayoutConfigResponse configResponse = new GetKiosqueroPayoutConfigResponse(
                KIOSQUERO_ID,
                "1234567890123456789012",
                "20123456789",
                "John Doe",
                1,
                IntervalUnit.DAYS,
                PayoutScheduleStatus.ACTIVE,
                LocalDate.of(2026, 7, 2),
                null
        );
        when(getUseCase.execute(any(GetKiosqueroPayoutConfigRequest.class)))
                .thenReturn(configResponse);
    }

    private ResultActions whenSavingConfig() throws Exception {
        return mockMvc.perform(post("/api/v1/kiosqueros/{usuarioId}/payout-config", USUARIO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_REQUEST));
    }

    private ResultActions whenGettingConfig() throws Exception {
        return mockMvc.perform(get("/api/v1/kiosqueros/{usuarioId}/payout-config", USUARIO_ID));
    }

    private void thenStatusIsCreated(ResultActions response) throws Exception {
        response.andExpect(status().isCreated());
    }

    private void thenStatusIsBadRequest(ResultActions response) throws Exception {
        response.andExpect(status().isBadRequest());
    }

    private void thenConfigIsReturned(ResultActions response) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.destinationCvu").value("1234567890123456789012"))
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"));
    }
}
