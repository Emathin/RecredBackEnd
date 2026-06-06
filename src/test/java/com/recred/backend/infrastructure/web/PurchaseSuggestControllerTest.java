package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.PurchaseSuggestIA;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PurchaseSuggestController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseSuggestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private PurchaseSuggestIA purchaseSuggestIA;

    @MockitoBean
    private RespondToAISuggestion respondToAISuggestion;

    @MockitoBean
    private CreateAdvancePurchase createAdvancePurchase;

    @MockitoBean
    private SugerenciaIARepository sugerenciaIARepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    private UUID sugerenciaId;
    private UUID alumnoId;
    private UUID productoId;
    private UUID usuarioId;
    private TestingAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        sugerenciaId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
        productoId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();
        
        auth = new TestingAuthenticationToken("user-123", null);
    }

    @Test
    void obtenerSugerenciaActiva_CuandoExiste_DeberiaRetornar200() throws Exception {
        ProductoEntity producto = new ProductoEntity();
        producto.setId(productoId);
        producto.setNombre("Manzana");
        producto.setPrecio(new BigDecimal("100.00"));

        SugerenciaIAEntity sugerencia = new SugerenciaIAEntity();
        sugerencia.setId(sugerenciaId);
        sugerencia.setTitulo("Comé sano");
        sugerencia.setMensaje("Una manzana al día...");
        sugerencia.setProductoEntity(producto);

        when(purchaseSuggestIA.execute(anyString())).thenReturn(Optional.of(sugerencia));

        mockMvc.perform(get("/api/v1/sugerencias-consumo/activa")
                .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sugerenciaId").value(sugerenciaId.toString()))
                .andExpect(jsonPath("$.productoNombre").value("Manzana"))
                .andExpect(jsonPath("$.productoPrecio").value(100.00));
    }

    @Test
    void obtenerSugerenciaActiva_CuandoNoExiste_DeberiaRetornar204() throws Exception {
        when(purchaseSuggestIA.execute(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/sugerencias-consumo/activa")
                .principal(auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void comprarSugerencia_DeberiaProcesarCompraAnticipada() throws Exception {
        ProductoEntity producto = new ProductoEntity();
        producto.setId(productoId);

        AlumnoEntity alumno = new AlumnoEntity();
        alumno.setId(alumnoId);

        SugerenciaIAEntity sugerencia = new SugerenciaIAEntity();
        sugerencia.setId(sugerenciaId);
        sugerencia.setAlumnoEntity(alumno);
        sugerencia.setProductoEntity(producto);

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setRol(RolUsuario.PADRE);

        AdvancePurchaseResponse response = new AdvancePurchaseResponse(
                UUID.randomUUID().toString(),
                new PurchaseStatus(UUID.randomUUID(), alumnoId, EstadoCompra.PENDIENTE, "Pendiente", java.time.LocalDateTime.now()),
                100.00,
                Collections.singletonMap(alumnoId, "CODE123")
        );

        when(sugerenciaIARepository.findById(sugerenciaId)).thenReturn(Optional.of(sugerencia));
        when(usuarioService.obtenerUsuarioActual(anyString())).thenReturn(usuario);
        when(createAdvancePurchase.execute(any(AdvancePurchaseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/sugerencias-consumo/{id}/comprar", sugerenciaId)
                .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.codes." + alumnoId).value("CODE123"));

        verify(respondToAISuggestion).execute(sugerenciaId, true);
        verify(createAdvancePurchase).execute(any(AdvancePurchaseRequest.class));
    }
}
