package com.recred.backend.web;

import com.recred.backend.application.GetProductsByBuffetUser;
import com.recred.backend.application.GetScheduledPickups;
import com.recred.backend.application.GetScheduledPickupsRequest;
import com.recred.backend.application.GetScheduledPickupsResponse;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.web.BuffetController;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import com.recred.backend.infrastructure.web.ScheduledPickupResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuffetControllerTest {

    @Mock
    private GetProductsByBuffetUser getProductsByBuffetUser;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private GetScheduledPickups getScheduledPickups;

    private BuffetController buffetController;

    @BeforeEach
    void setUp() {
        buffetController = new BuffetController(
                getProductsByBuffetUser,
                usuarioService,
                getScheduledPickups
        );
    }

    @Test
    void findAllByUserId_deberiaRetornarListaDeProductosY200Ok() {
        UUID userId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Producto de Prueba");

        when(getProductsByBuffetUser.execute(userId)).thenReturn(List.of(producto));

        ResponseEntity<List<ProductResponseDTO>> response = buffetController.findAllByUserId(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(producto.getId(), response.getBody().get(0).id());
        verify(getProductsByBuffetUser, times(1)).execute(userId);
    }

    @Test
    void getScheduledPickups_cuandoUsuarioEsKiosqueroAsociadoABuffet_deberiaRetornarListadePickupsY200Ok() {
        String sub = "sub-123";
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(sub);

        UUID buffetId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        Kiosquero kiosquero = new Kiosquero();
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        kiosquero.setBuffet(buffet);
        usuario.setKiosquero(kiosquero);

        LocalDate fecha = LocalDate.now();
        EstadoRetiro estado = EstadoRetiro.PROGRAMADO;
        UUID franjaId = UUID.randomUUID();
        String search = "test";

        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());

        when(usuarioService.obtenerUsuarioActual(sub)).thenReturn(usuario);
        when(getScheduledPickups.execute(new GetScheduledPickupsRequest(buffetId, fecha, estado, franjaId, search)))
                .thenReturn(List.of(new GetScheduledPickupsResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "John Doe",
                        new java.math.BigDecimal("10.00"),
                        "PENDIENTE",
                        "Pendiente",
                        "PROGRAMADO",
                        "CREDITOS",
                        java.time.LocalDateTime.now(),
                        "W123",
                        UUID.randomUUID(),
                        "Recreo 1",
                        fecha,
                        List.of()
                )));

        ResponseEntity<List<ScheduledPickupResponseDTO>> response = buffetController.getScheduledPickups(
                jwt, fecha, estado, franjaId, search
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(usuarioService, times(1)).obtenerUsuarioActual(sub);
        verify(getScheduledPickups, times(1)).execute(new GetScheduledPickupsRequest(buffetId, fecha, estado, franjaId, search));
    }

    @Test
    void getScheduledPickups_cuandoUsuarioNoTieneKiosquero_deberiaLanzarExcepcion() {
        String sub = "sub-123";
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(sub);

        Usuario usuario = new Usuario();

        when(usuarioService.obtenerUsuarioActual(sub)).thenReturn(usuario);

        assertThrows(RuntimeException.class, () ->
                buffetController.getScheduledPickups(jwt, null, null, null, null)
        );

        verify(usuarioService, times(1)).obtenerUsuarioActual(sub);
        verifyNoInteractions(getScheduledPickups);
    }
}
