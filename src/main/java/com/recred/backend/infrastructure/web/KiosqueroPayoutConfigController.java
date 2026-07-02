package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetKiosqueroPayoutConfig;
import com.recred.backend.application.GetKiosqueroPayoutConfigCommand;
import com.recred.backend.application.GetKiosqueroPayoutConfigResponse;
import com.recred.backend.application.SaveKiosqueroPayoutConfig;
import com.recred.backend.application.SaveKiosqueroPayoutConfigCommand;
import com.recred.backend.application.SaveKiosqueroPayoutConfigResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kiosqueros/{kiosqueroId}/payout-config")
public class KiosqueroPayoutConfigController {

    private final SaveKiosqueroPayoutConfig saveKiosqueroPayoutConfig;
    private final GetKiosqueroPayoutConfig getKiosqueroPayoutConfig;

    public KiosqueroPayoutConfigController(
            SaveKiosqueroPayoutConfig saveKiosqueroPayoutConfig,
            GetKiosqueroPayoutConfig getKiosqueroPayoutConfig) {
        this.saveKiosqueroPayoutConfig = saveKiosqueroPayoutConfig;
        this.getKiosqueroPayoutConfig = getKiosqueroPayoutConfig;
    }

    @PostMapping
    public ResponseEntity<SaveKiosqueroPayoutConfigResponse> saveConfig(
            @PathVariable UUID kiosqueroId,
            @RequestBody SaveKiosqueroPayoutConfigCommand command) {
        SaveKiosqueroPayoutConfigCommand safeCommand = new SaveKiosqueroPayoutConfigCommand(
                kiosqueroId,
                command.destinationCvu(),
                command.destinationCuit(),
                command.accountHolderName(),
                command.cantidadIntervalo(),
                command.unidadIntervalo(),
                command.estado()
        );
        SaveKiosqueroPayoutConfigResponse response = saveKiosqueroPayoutConfig
                .execute(safeCommand);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<GetKiosqueroPayoutConfigResponse> getConfig(
            @PathVariable UUID kiosqueroId) {
        GetKiosqueroPayoutConfigCommand command = new GetKiosqueroPayoutConfigCommand(kiosqueroId);
        GetKiosqueroPayoutConfigResponse response = getKiosqueroPayoutConfig.execute(command);
        return ResponseEntity.ok(response);
    }
}
