package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.infrastructure.entities.BilleteraEntity;
import com.recred.backend.infrastructure.entities.MovimientoBilleteraEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MovimientoBilleteraRepositoryImpl implements MovimientoBilleteraRepository {

    private final SpringDataMovimientoBilleteraRepository springDataMovimientoBilleteraRepository;
    private final SpringDataWalletRepository springDataWalletRepository;
    private final SpringDataUsuarioRepository springDataUsuarioRepository;

    public MovimientoBilleteraRepositoryImpl(
            SpringDataMovimientoBilleteraRepository springDataMovimientoBilleteraRepository,
            SpringDataWalletRepository springDataWalletRepository,
            SpringDataUsuarioRepository springDataUsuarioRepository) {
        this.springDataMovimientoBilleteraRepository = springDataMovimientoBilleteraRepository;
        this.springDataWalletRepository = springDataWalletRepository;
        this.springDataUsuarioRepository = springDataUsuarioRepository;
    }

    @Override
    public void registrarMovimiento(
            UUID billeteraId,
            TipoMovimientoBilletera tipo,
            BigDecimal monto,
            String descripcion,
            UUID usuarioId) {

        BilleteraEntity billeteraEntity = springDataWalletRepository.getReferenceById(billeteraId);
        UsuarioEntity usuarioEntity = springDataUsuarioRepository.getReferenceById(usuarioId);

        MovimientoBilleteraEntity movimiento = new MovimientoBilleteraEntity();
        movimiento.setBilleteraEntity(billeteraEntity);
        movimiento.setTipo(tipo);
        movimiento.setMonto(monto);
        movimiento.setDescripcion(descripcion);
        movimiento.setUsuarioEntity(usuarioEntity);

        springDataMovimientoBilleteraRepository.save(movimiento);
    }
}
