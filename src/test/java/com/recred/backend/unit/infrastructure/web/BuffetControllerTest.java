package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetProductsByBuffetUser;
import com.recred.backend.application.GetProductsByBuffetUserRequest;
import com.recred.backend.application.GetProductsByBuffetUserResponse;
import com.recred.backend.application.GetScheduledPickups;
import com.recred.backend.application.GetScheduledPickupsRequest;
import com.recred.backend.application.GetScheduledPickupsResponse;
import com.recred.backend.application.ProductResponse;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.EstadoCompra;
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

    @Mock private GetProductsByBuffetUser getProductsByBuffetUser;
    @Mock private UsuarioService usuarioService;
    @Mock private GetScheduledPickups getScheduledPickups;

    private BuffetController buffetController;

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final UUID BUFFET_ID = UUID.randomUUID();
    private final UUID FRANJA_ID = UUID.randomUUID();
    private final String PRODUCT_NAME = "Producto de Prueba";
    private final String SUB = "sub-123";
    private final String SEARCH = "test";
    private final LocalDate FECHA = LocalDate.now();
    private final EstadoRetiro ESTADO = EstadoRetiro.PROGRAMADO;
    private final EstadoCompra STATUS = EstadoCompra.PENDIENTE;

    @BeforeEach
    void setUp() {
        buffetController = new BuffetController(
                getProductsByBuffetUser,
                usuarioService,
                getScheduledPickups
        );
    }

    @Test
    void findAllByUserIdShouldReturnProductListAnd200Ok() {
        givenProductsExistForBuffetUser();
        
        ResponseEntity<List<ProductResponseDTO>> response = whenFindingAllByUserId();
        
        thenProductListResponseIsOk(response);
        thenDelegatesToGetProductsByBuffetUser();
    }

    @Test
    void getScheduledPickupsShouldReturnPickupsAnd200OkWhenUserIsKiosquero() {
        Jwt jwt = givenJwt();
        givenUserIsKiosqueroAssociatedToBuffet();
        givenScheduledPickupsExist();
        
        ResponseEntity<List<ScheduledPickupResponseDTO>> response = 
                whenGettingScheduledPickups(jwt);
                
        thenScheduledPickupsResponseIsOk(response);
        thenDelegatesToGetScheduledPickups();
    }

    @Test
    void getScheduledPickupsShouldThrowExceptionWhenUserIsNotKiosquero() {
        Jwt jwt = givenJwt();
        givenUserIsNotKiosquero();
        
        assertThrows(RuntimeException.class, () -> 
                buffetController.getScheduledPickups(jwt, null, null, null, null, null)
        );
        
        thenDoesNotDelegateToGetScheduledPickups();
    }

    private void givenProductsExistForBuffetUser() {
        Producto producto = new Producto();
        producto.setId(PRODUCT_ID);
        producto.setNombre(PRODUCT_NAME);
        when(getProductsByBuffetUser.execute(new GetProductsByBuffetUserRequest(USER_ID)))
                .thenReturn(new GetProductsByBuffetUserResponse(
                        List.of(ProductResponse.fromDomain(producto))));
    }

    private Jwt givenJwt() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(SUB);
        return jwt;
    }

    private void givenUserIsKiosqueroAssociatedToBuffet() {
        Usuario usuario = new Usuario();
        Kiosquero kiosquero = new Kiosquero();
        Buffet buffet = new Buffet();
        buffet.setId(BUFFET_ID);
        kiosquero.setBuffet(buffet);
        usuario.setKiosquero(kiosquero);
        when(usuarioService.obtenerUsuarioActual(SUB)).thenReturn(usuario);
    }

    private void givenUserIsNotKiosquero() {
        Usuario usuario = new Usuario();
        when(usuarioService.obtenerUsuarioActual(SUB)).thenReturn(usuario);
    }

    private void givenScheduledPickupsExist() {
        when(getScheduledPickups.execute(new GetScheduledPickupsRequest(
                BUFFET_ID, FECHA, ESTADO, STATUS, FRANJA_ID, SEARCH)))
                .thenReturn(List.of(new GetScheduledPickupsResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "John Doe",
                        new java.math.BigDecimal("10.00"), "PENDIENTE", "Pendiente",
                        "PROGRAMADO", "CREDITOS", java.time.LocalDateTime.now(),
                        "W123", UUID.randomUUID(), "Recreo 1", FECHA, List.of()
                )));
    }

    private ResponseEntity<List<ProductResponseDTO>> whenFindingAllByUserId() {
        return buffetController.findAllByUserId(USER_ID);
    }

    private ResponseEntity<List<ScheduledPickupResponseDTO>> whenGettingScheduledPickups(Jwt jwt) {
        return buffetController.getScheduledPickups(jwt, FECHA, ESTADO, STATUS, FRANJA_ID, SEARCH);
    }

    private void thenProductListResponseIsOk(ResponseEntity<List<ProductResponseDTO>> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(PRODUCT_ID, response.getBody().get(0).id());
    }

    private void thenDelegatesToGetProductsByBuffetUser() {
        verify(getProductsByBuffetUser, times(1))
                .execute(new GetProductsByBuffetUserRequest(USER_ID));
    }

    private void thenScheduledPickupsResponseIsOk(
            ResponseEntity<List<ScheduledPickupResponseDTO>> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    private void thenDelegatesToGetScheduledPickups() {
        verify(usuarioService, times(1)).obtenerUsuarioActual(SUB);
        verify(getScheduledPickups, times(1)).execute(new GetScheduledPickupsRequest(
                BUFFET_ID, FECHA, ESTADO, STATUS, FRANJA_ID, SEARCH));
    }

    private void thenDoesNotDelegateToGetScheduledPickups() {
        verify(usuarioService, times(1)).obtenerUsuarioActual(SUB);
        verifyNoInteractions(getScheduledPickups);
    }
}
