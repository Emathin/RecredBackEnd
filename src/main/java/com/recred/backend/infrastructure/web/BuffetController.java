package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetProductsByBuffetUser;
import com.recred.backend.application.GetScheduledPickups;
import com.recred.backend.application.GetScheduledPickupsRequest;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buffet")
public class BuffetController {

    private final GetProductsByBuffetUser getProductsByBuffetUser;
    private final UsuarioService usuarioService;
    private final GetScheduledPickups getScheduledPickups;

    public BuffetController(GetProductsByBuffetUser getProductsByBuffetUser,
                            UsuarioService usuarioService,
                            GetScheduledPickups getScheduledPickups) {
        this.getProductsByBuffetUser = getProductsByBuffetUser;
        this.usuarioService = usuarioService;
        this.getScheduledPickups = getScheduledPickups;
    }

    @GetMapping("/user/{userId}/products")
    public ResponseEntity<List<ProductResponseDTO>> findAllByUserId(
            @PathVariable UUID userId) {
        List<ProductResponseDTO> products =
                getProductsByBuffetUser.execute(userId)
                .stream()
                .map(ProductResponseDTO::fromAppResponse)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/scheduled-pickups")
    public ResponseEntity<List<ScheduledPickupResponseDTO>> getScheduledPickups(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(required = false) EstadoRetiro estadoRetiro,
            @RequestParam(required = false) EstadoCompra status,
            @RequestParam(required = false) UUID franjaId,
            @RequestParam(required = false) String search
    ) {
        String sub = jwt.getSubject();
        Usuario usuario = usuarioService.obtenerUsuarioActual(sub);
        if (usuario.getKiosquero() == null || usuario.getKiosquero().getBuffet() == null) {
            throw new RuntimeException("El usuario no está asociado a ningún buffet.");
        }
        UUID buffetId = usuario.getKiosquero().getBuffet().getId();

        GetScheduledPickupsRequest request = new GetScheduledPickupsRequest(
                buffetId, fecha, estadoRetiro, status, franjaId, search);

        List<ScheduledPickupResponseDTO> pickups = getScheduledPickups.execute(request)
                .stream()
                .map(ScheduledPickupResponseDTO::new)
                .toList();

        return ResponseEntity.ok(pickups);
    }
}
