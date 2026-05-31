package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Wallet;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.infrastructure.entities.WalletEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WalletRepositoryImpl implements WalletRepository {

    private final SpringDataWalletRepository springDataRepository;

    public WalletRepositoryImpl(SpringDataWalletRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Wallet> findByStudentId(UUID studentId) {
        return springDataRepository.findByAlumnoEntityId(studentId)
                .map(WalletEntity::toDomain);
    }

    @Override
    public void save(Wallet wallet) {
        WalletEntity entity = springDataRepository.findById(wallet.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        entity.setCreditBalance(wallet.getCreditBalance());
        entity.setCreditLimit(wallet.getCreditLimit());
        entity.setInnerDebtBalance(wallet.getInnerDebtBalance());
        entity.setEnabledCredit(wallet.getEnabledCredit());

        springDataRepository.save(entity);
    }
}
