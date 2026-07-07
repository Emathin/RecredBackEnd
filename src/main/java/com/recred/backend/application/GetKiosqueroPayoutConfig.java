package com.recred.backend.application;

import com.recred.backend.domain.exceptions.KiosqueroPayoutConfigNotFoundException;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;

public class GetKiosqueroPayoutConfig {
    private final KiosqueroPayoutConfigRepository payoutConfigRepository;

    public GetKiosqueroPayoutConfig(
            KiosqueroPayoutConfigRepository payoutConfigRepository) {
        this.payoutConfigRepository = payoutConfigRepository;
    }

    public GetKiosqueroPayoutConfigResponse execute(
            GetKiosqueroPayoutConfigRequest configRequest) {
        KiosqueroPayoutConfig config = findPayoutConfig(configRequest);

        return responseFrom(config);
    }

    private KiosqueroPayoutConfig findPayoutConfig(
            GetKiosqueroPayoutConfigRequest configRequest) {
        return payoutConfigRepository.findByKiosqueroId(configRequest.kiosqueroId())
                .orElseThrow(() -> new KiosqueroPayoutConfigNotFoundException(
                        configRequest.kiosqueroId()));
    }

    private GetKiosqueroPayoutConfigResponse responseFrom(KiosqueroPayoutConfig config) {
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
