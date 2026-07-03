package com.recred.backend.application;

import com.recred.backend.domain.exceptions.KiosqueroPayoutConfigNotFoundException;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;

public class GetKiosqueroPayoutConfig {
    private final KiosqueroPayoutConfigRepository repository;

    public GetKiosqueroPayoutConfig(KiosqueroPayoutConfigRepository repository) {
        this.repository = repository;
    }

    public GetKiosqueroPayoutConfigResponse execute(GetKiosqueroPayoutConfigRequest request) {
        KiosqueroPayoutConfig config = repository
                .findByKiosqueroId(request.kiosqueroId())
                .orElseThrow(() -> new KiosqueroPayoutConfigNotFoundException(
                        request.kiosqueroId()
                ));

        return toResponse(config);
    }

    private GetKiosqueroPayoutConfigResponse toResponse(KiosqueroPayoutConfig config) {
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
