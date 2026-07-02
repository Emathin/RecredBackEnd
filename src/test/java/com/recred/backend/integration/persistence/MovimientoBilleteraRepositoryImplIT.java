package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BilleteraEntity;
import com.recred.backend.infrastructure.entities.MovimientoBilleteraEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.MovimientoBilleteraRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataMovimientoBilleteraRepository;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class MovimientoBilleteraRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private MovimientoBilleteraRepositoryImpl repository;

    @Autowired
    private SpringDataMovimientoBilleteraRepository springDataRepository;

    @Autowired
    private SpringDataUsuarioRepository springDataUsuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity cachedStudentUser;
    private AlumnoEntity cachedStudent;
    private BilleteraEntity cachedWallet;
    private UsuarioEntity cachedAdminUser;

    @Test
    void registerMovementShouldCreateAndSaveEntityCorrectly() {
        BilleteraEntity wallet = generateWalletEntity();
        UsuarioEntity admin = generateAdminUserEntity();
        BigDecimal amount = new BigDecimal("100.50");
        String description = "Test movement";
        TipoMovimientoBilletera type = TipoMovimientoBilletera.CARGA;

        repository.registrarMovimiento(wallet.getId(), type, amount, description, admin.getId());
        entityManager.flush();
        entityManager.clear();

        List<MovimientoBilleteraEntity> saved = springDataRepository.findAll();
        assertTrue(saved.stream().anyMatch(m -> 
            m.getBilleteraEntity().getId().equals(wallet.getId()) &&
            m.getTipo() == type &&
            m.getMonto().compareTo(amount) == 0 &&
            m.getDescripcion().equals(description) &&
            m.getUsuarioEntity().getId().equals(admin.getId())
        ));
    }

    @Test
    void findByStudentIdAndDateRangeShouldMapAmountAndDirection() {
        BilleteraEntity wallet = generateWalletEntity();
        UsuarioEntity admin = generateAdminUserEntity();
        
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        
        generateMovementEntity(wallet, admin, TipoMovimientoBilletera.CARGA, new BigDecimal("100.00"), from.plusDays(1));
        generateMovementEntity(wallet, admin, TipoMovimientoBilletera.COMPRA, new BigDecimal("25.00"), from.plusDays(2));
        generateMovementEntity(wallet, admin, TipoMovimientoBilletera.AJUSTE, new BigDecimal("-10.00"), from.plusDays(3));

        List<WalletMovement> result = repository.findByStudentIdAndDateRange(
                wallet.getAlumnoEntity().getId(),
                from,
                to
        );

        assertTrue(result.size() >= 3);
        
        assertMovement(result, "100.00", WalletMovementDirection.ENTRADA);
        assertMovement(result, "25.00", WalletMovementDirection.SALIDA);
        assertMovement(result, "10.00", WalletMovementDirection.SALIDA);
    }

    private void assertMovement(List<WalletMovement> result, String expectedAmount, WalletMovementDirection expectedDirection) {
        Optional<WalletMovement> match = result.stream().filter(r -> r.amount().compareTo(new BigDecimal(expectedAmount)) == 0).findFirst();
        assertTrue(match.isPresent());
        assertEquals(expectedDirection, match.get().direction());
    }

    @Test
    void findByStudentIdAndDateRangeWhenAmountIsZeroShouldMapZeroAmount() {
        BilleteraEntity wallet = generateWalletEntity();
        UsuarioEntity admin = generateAdminUserEntity();
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        
        generateMovementEntity(wallet, admin, TipoMovimientoBilletera.AJUSTE, BigDecimal.ZERO, from.plusDays(1));

        List<WalletMovement> result = repository.findByStudentIdAndDateRange(
                wallet.getAlumnoEntity().getId(),
                from,
                to
        );

        Optional<WalletMovement> match = result.stream().filter(r -> r.amount().compareTo(BigDecimal.ZERO) == 0).findFirst();
        assertTrue(match.isPresent());
        assertEquals(BigDecimal.ZERO, match.get().amount());
        assertEquals(WalletMovementDirection.ENTRADA, match.get().direction());
    }

    @Test
    void registerSystemMovementWhenSystemUserExistsShouldUseIt() {
        BilleteraEntity wallet = generateWalletEntity();
        UsuarioEntity systemUser = generateSystemUserEntity();

        repository.registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.CARGA,
                new BigDecimal("50.00"),
                "System load"
        );
        entityManager.flush();
        entityManager.clear();

        List<MovimientoBilleteraEntity> saved = springDataRepository.findAll();
        assertTrue(saved.stream().anyMatch(m -> 
            m.getBilleteraEntity().getId().equals(wallet.getId()) &&
            m.getUsuarioEntity().getId().equals(systemUser.getId()) &&
            m.getDescripcion().equals("System load")
        ));
    }

    @Test
    void registerSystemMovementWhenSystemUserDoesNotExistShouldCreateIt() {
        BilleteraEntity wallet = generateWalletEntity();
        
        springDataUsuarioRepository.findByIdCognito("system").ifPresent(user -> {
            springDataRepository.deleteAll();
            springDataUsuarioRepository.delete(user);
            entityManager.flush();
        });

        repository.registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.CARGA,
                new BigDecimal("50.00"),
                "Autogenerated system load"
        );
        entityManager.flush();
        entityManager.clear();

        Optional<UsuarioEntity> createdSysUser = springDataUsuarioRepository.findByIdCognito("system");
        assertTrue(createdSysUser.isPresent());
        assertEquals("system@recred.local", createdSysUser.get().getEmail());
        assertEquals(RolUsuario.ADMIN, createdSysUser.get().getRol());
        
        List<MovimientoBilleteraEntity> saved = springDataRepository.findAll();
        assertTrue(saved.stream().anyMatch(m -> 
            m.getUsuarioEntity().getId().equals(createdSysUser.get().getId()) &&
            m.getDescripcion().equals("Autogenerated system load")
        ));
    }

    private UsuarioEntity generateStudentUserEntity() {
        if (this.cachedStudentUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-wallet-student-" + UUID.randomUUID().toString().substring(0, 8));
            user.setNombre("Student");
            user.setApellido("Wallet");
            user.setEmail("student.wallet@test.com");
            user.setRol(RolUsuario.ALUMNO);
            user.setActivo(true);
            entityManager.persist(user);
            this.cachedStudentUser = user;
        }
        return this.cachedStudentUser;
    }

    private AlumnoEntity generateStudentEntity() {
        if (this.cachedStudent == null) {
            AlumnoEntity student = new AlumnoEntity();
            student.setUsuario(generateStudentUserEntity());
            student.setActivo(true);
            entityManager.persist(student);
            this.cachedStudent = student;
        }
        return this.cachedStudent;
    }

    private BilleteraEntity generateWalletEntity() {
        if (this.cachedWallet == null) {
            BilleteraEntity wallet = new BilleteraEntity();
            wallet.setAlumnoEntity(generateStudentEntity());
            wallet.setSaldoCreditos(BigDecimal.ZERO);
            wallet.setCreditoHabilitado(false);
            entityManager.persist(wallet);
            this.cachedWallet = wallet;
        }
        return this.cachedWallet;
    }

    private UsuarioEntity generateAdminUserEntity() {
        if (this.cachedAdminUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-admin-wallet-" + UUID.randomUUID().toString().substring(0, 8));
            user.setNombre("Admin");
            user.setApellido("User");
            user.setEmail("admin.wallet@test.com");
            user.setRol(RolUsuario.ADMIN);
            user.setActivo(true);
            entityManager.persist(user);
            this.cachedAdminUser = user;
        }
        return this.cachedAdminUser;
    }
    
    private UsuarioEntity generateSystemUserEntity() {
        Optional<UsuarioEntity> existing = springDataUsuarioRepository.findByIdCognito("system");
        if (existing.isPresent()) {
            return existing.get();
        }
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("system");
        user.setNombre("System");
        user.setApellido("User");
        user.setEmail("system@recred.local");
        user.setRol(RolUsuario.ADMIN);
        user.setActivo(true);
        return springDataUsuarioRepository.saveAndFlush(user);
    }

    private MovimientoBilleteraEntity generateMovementEntity(
            BilleteraEntity wallet,
            UsuarioEntity user,
            TipoMovimientoBilletera type,
            BigDecimal amount,
            LocalDateTime date
    ) {
        MovimientoBilleteraEntity entity = new MovimientoBilleteraEntity();
        entity.setBilleteraEntity(wallet);
        entity.setUsuarioEntity(user);
        entity.setTipo(type);
        entity.setMonto(amount);
        entity.setDescripcion("Mock movement");
        entity.setCreadoEn(date);
        
        MovimientoBilleteraEntity saved = springDataRepository.save(entity);
        entityManager.flush();
        
        return saved;
    }
}
