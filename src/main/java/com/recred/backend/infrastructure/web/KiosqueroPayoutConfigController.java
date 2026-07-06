package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetKiosqueroPayoutConfig;
import com.recred.backend.application.GetKiosqueroPayoutConfigRequest;
import com.recred.backend.application.GetKiosqueroPayoutConfigResponse;
import com.recred.backend.application.SaveKiosqueroPayoutConfig;
import com.recred.backend.application.SaveKiosqueroPayoutConfigRequest;
import com.recred.backend.application.SaveKiosqueroPayoutConfigResponse;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kiosqueros/{usuarioId}/payout-config")
public class KiosqueroPayoutConfigController {

    private final SaveKiosqueroPayoutConfig saveKiosqueroPayoutConfig;
    private final GetKiosqueroPayoutConfig getKiosqueroPayoutConfig;
    private final KiosqueroRepository kiosqueroRepository;

    public KiosqueroPayoutConfigController(
            SaveKiosqueroPayoutConfig saveKiosqueroPayoutConfig,
            GetKiosqueroPayoutConfig getKiosqueroPayoutConfig,
            KiosqueroRepository kiosqueroRepository) {
        this.saveKiosqueroPayoutConfig = saveKiosqueroPayoutConfig;
        this.getKiosqueroPayoutConfig = getKiosqueroPayoutConfig;
        this.kiosqueroRepository = kiosqueroRepository;
    }

    @PostMapping
    public ResponseEntity<SaveKiosqueroPayoutConfigResponse> saveConfig(
            @PathVariable UUID usuarioId,
            @RequestBody SaveKiosqueroPayoutConfigRequest request) {
        UUID kiosqueroId = findKiosqueroId(usuarioId);

        SaveKiosqueroPayoutConfigRequest safeRequest = new SaveKiosqueroPayoutConfigRequest(
                kiosqueroId,
                request.destinationCvu(),
                request.destinationCuit(),
                request.accountHolderName(),
                request.cantidadIntervalo(),
                request.unidadIntervalo(),
                request.estado()
        );
        SaveKiosqueroPayoutConfigResponse response = saveKiosqueroPayoutConfig
                .execute(safeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<GetKiosqueroPayoutConfigResponse> getConfig(
            @PathVariable UUID usuarioId) {
        UUID kiosqueroId = findKiosqueroId(usuarioId);
        GetKiosqueroPayoutConfigRequest request = new GetKiosqueroPayoutConfigRequest(kiosqueroId);
        GetKiosqueroPayoutConfigResponse response = getKiosqueroPayoutConfig.execute(request);
        return ResponseEntity.ok(response);
    }

    private UUID findKiosqueroId(UUID usuarioId) {
        return kiosqueroRepository.findKiosqueroIdByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kiosquero no encontrado para el usuario: " + usuarioId));
    }
}
