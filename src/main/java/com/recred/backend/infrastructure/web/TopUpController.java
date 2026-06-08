package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateTopUpLink;
import com.recred.backend.application.TopUpCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class TopUpController {

    private final GenerateTopUpLink generateTopUpLink;

    public TopUpController(GenerateTopUpLink generateTopUpLink) {
        this.generateTopUpLink = generateTopUpLink;
    }

    @PostMapping("/topup")
    public ResponseEntity<Map<String, String>> requestTopUpLink(@RequestBody TopUpRequest request) {
        // Mapea el DTO de infraestructura al comando de aplicacion
        TopUpCommand command = new TopUpCommand(request.studentId(), request.amount());

        // Ejecuta el caso de uso para obtener la URL de Mercado Pago
        String paymentUrl = generateTopUpLink.execute(command);

        // Retorna la URL (init_point) envuelta en un mapa para el frontend
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }
}
