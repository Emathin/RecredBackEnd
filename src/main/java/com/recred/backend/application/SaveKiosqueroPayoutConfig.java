package com.recred.backend.application;

import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class SaveKiosqueroPayoutConfig {
    private final KiosqueroPayoutConfigRepository repository;
    private final Clock clock;

    public SaveKiosqueroPayoutConfig(KiosqueroPayoutConfigRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    public SaveKiosqueroPayoutConfig(KiosqueroPayoutConfigRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public SaveKiosqueroPayoutConfigResponse execute(SaveKiosqueroPayoutConfigRequest request) {
        KiosqueroPayoutConfig config = findOrCreateConfig(request.kiosqueroId());
        updateConfig(config, request);
        config.validateIntervalConfig();
        calculateNextPayoutIfMissing(config);
        return toResponse(repository.save(config));
    }

    private KiosqueroPayoutConfig findOrCreateConfig(UUID kiosqueroId) {
        Optional<KiosqueroPayoutConfig> existing = repository.findByKiosqueroId(kiosqueroId);
        return existing.orElseGet(() -> newConfig(kiosqueroId));
    }

    private KiosqueroPayoutConfig newConfig(UUID kiosqueroId) {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setKiosqueroId(kiosqueroId);
        return config;
    }

    private void updateConfig(
            KiosqueroPayoutConfig config,
            SaveKiosqueroPayoutConfigRequest request) {
        config.setDestinationCvu(request.destinationCvu());
        config.setDestinationCuit(request.destinationCuit());
        config.setAccountHolderName(request.accountHolderName());
        config.setCantidadIntervalo(request.cantidadIntervalo());
        config.setUnidadIntervalo(request.unidadIntervalo());
        config.setEstado(request.estado());
    }

    private void calculateNextPayoutIfMissing(KiosqueroPayoutConfig config) {
        if (config.getProximaEjecucion() == null) {
            config.calculateNextPayout(LocalDate.now(clock));
        }
    }

    private SaveKiosqueroPayoutConfigResponse toResponse(KiosqueroPayoutConfig saved) {
        return new SaveKiosqueroPayoutConfigResponse(
                saved.getKiosqueroId(),
                saved.getDestinationCvu(),
                saved.getDestinationCuit(),
                saved.getAccountHolderName(),
                saved.getCantidadIntervalo(),
                saved.getUnidadIntervalo(),
                saved.getEstado(),
                saved.getProximaEjecucion()
        );
    }
}
