package com.recred.backend.application;

import com.recred.backend.domain.exceptions.KiosqueroPayoutConfigNotFoundException;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;

public class GetKiosqueroPayoutConfig {
    private final KiosqueroPayoutConfigRepository repository;

    public GetKiosqueroPayoutConfig(KiosqueroPayoutConfigRepository repository) {
        this.repository = repository;
    }

    public GetKiosqueroPayoutConfigResponse execute(GetKiosqueroPayoutConfigCommand command) {
        KiosqueroPayoutConfig config = repository
                .findByKiosqueroId(command.kiosqueroId())
                .orElseThrow(() -> new KiosqueroPayoutConfigNotFoundException(
                        command.kiosqueroId()
                ));
        
        return new GetKiosqueroPayoutConfigResponse(
            config.getKiosqueroId(),
            config.getDestinationCvu(),
            config.getDestinationCuit(),
            config.getAccountHolderName(),
            config.getCantidadIntervalo(),
            config.getUnidadIntervalo(),
            config.getEstado(),
            config.getProximaEjecucion(),
            config.getUltimaEjecucion()
        );
    }
}
