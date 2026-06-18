package com.recred.backend.application;

import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;
import java.time.LocalDate;
import java.util.Optional;

public class SaveKiosqueroPayoutConfig {
    private final KiosqueroPayoutConfigRepository repository;

    public SaveKiosqueroPayoutConfig(KiosqueroPayoutConfigRepository repository) {
        this.repository = repository;
    }

    public SaveKiosqueroPayoutConfigResponse execute(SaveKiosqueroPayoutConfigCommand command) {
        Optional<KiosqueroPayoutConfig> existingOpt = repository
                .findByKiosqueroId(command.kiosqueroId());
        
        KiosqueroPayoutConfig config;
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
        } else {
            config = new KiosqueroPayoutConfig();
            config.setKiosqueroId(command.kiosqueroId());
        }
        
        config.setDestinationCvu(command.destinationCvu());
        config.setDestinationCuit(command.destinationCuit());
        config.setAccountHolderName(command.accountHolderName());
        config.setCantidadIntervalo(command.cantidadIntervalo());
        config.setUnidadIntervalo(command.unidadIntervalo());
        config.setEstado(command.estado());
        
        if (config.getProximaEjecucion() == null) {
            config.calculateNextPayout(LocalDate.now());
        }
        
        KiosqueroPayoutConfig saved = repository.save(config);
        
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
