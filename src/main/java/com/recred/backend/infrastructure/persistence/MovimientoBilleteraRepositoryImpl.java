package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.infrastructure.entities.BilleteraEntity;
import com.recred.backend.infrastructure.entities.MovimientoBilleteraEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class MovimientoBilleteraRepositoryImpl implements MovimientoBilleteraRepository {

    private static final String SYSTEM_COGNITO_SUB = "system";
    private static final String SYSTEM_EMAIL = "system@recred.local";

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
    public List<WalletMovement> findByStudentIdAndDateRange(
            UUID studentId,
            LocalDateTime from,
            LocalDateTime to) {
        return springDataMovimientoBilleteraRepository
                .findByStudentIdAndDateRange(studentId, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
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

    @Override
    public BigDecimal obtenerUltimoMontoCarga(UUID billeteraId) {
        return springDataMovimientoBilleteraRepository
                .findFirstByBilleteraEntityIdAndTipoOrderByCreadoEnDesc(
                        billeteraId, TipoMovimientoBilletera.CARGA)
                .map(MovimientoBilleteraEntity::getMonto)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getCollaboratorNetBalance(UUID billeteraId, UUID usuarioId) {
        return springDataMovimientoBilleteraRepository
                .getCollaboratorNetBalance(billeteraId, usuarioId);
    }

    @Override
    public void registrarMovimientoSistema(
            UUID billeteraId,
            TipoMovimientoBilletera tipo,
            BigDecimal monto,
            String descripcion) {
        registrarMovimiento(billeteraId, tipo, monto, descripcion, systemUserId());
    }

    private UUID systemUserId() {
        return springDataUsuarioRepository.findByIdCognito(SYSTEM_COGNITO_SUB)
                .orElseGet(this::createSystemUser)
                .getId();
    }

    private UsuarioEntity createSystemUser() {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito(SYSTEM_COGNITO_SUB);
        user.setEmail(SYSTEM_EMAIL);
        user.setNombre("Sistema");
        user.setApellido("Recred");
        user.setRol(RolUsuario.ADMIN);
        user.setActivo(true);
        user.setAutenticadoConCognito(false);
        return springDataUsuarioRepository.save(user);
    }

    private WalletMovement toDomain(MovimientoBilleteraEntity entity) {
        BigDecimal amount = entity.getMonto() == null
                ? BigDecimal.ZERO
                : entity.getMonto();
        return new WalletMovement(
                entity.getId(),
                entity.getCreadoEn(),
                entity.getTipo(),
                entity.getDescripcion(),
                amount.abs(),
                direction(entity.getTipo(), amount));
    }

    private WalletMovementDirection direction(
            TipoMovimientoBilletera type,
            BigDecimal amount) {
        if (amount.signum() < 0) {
            return WalletMovementDirection.SALIDA;
        }
        if (type == TipoMovimientoBilletera.COMPRA
                || type == TipoMovimientoBilletera.CREDITO_UTILIZADO) {
            return WalletMovementDirection.SALIDA;
        }
        return WalletMovementDirection.ENTRADA;
    }
}
