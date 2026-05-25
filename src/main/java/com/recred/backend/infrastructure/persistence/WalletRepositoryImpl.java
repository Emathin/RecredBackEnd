package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BilleteraEntity;
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
    public Optional<Billetera> findByStudentId(UUID studentId) {
        return springDataRepository.findByAlumnoEntity_Id(studentId)
                .map(BilleteraEntity::toDomain);
    }

    @Override
    public void save(Billetera wallet) {
        BilleteraEntity entity = springDataRepository.findById(wallet.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        entity.setSaldoCreditos(wallet.getSaldoCreditos());
        entity.setLimiteCredito(wallet.getLimiteCredito());
        entity.setSaldoDeudaInterno(wallet.getSaldoDeudaInterno());
        entity.setCreditoHabilitado(wallet.getCreditoHabilitado());

        springDataRepository.save(entity);
    }
}
