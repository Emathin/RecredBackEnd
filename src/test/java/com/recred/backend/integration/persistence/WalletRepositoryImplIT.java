package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BilleteraEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataAlumnoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataWalletRepository;
import com.recred.backend.infrastructure.persistence.WalletRepositoryImpl;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class WalletRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private WalletRepositoryImpl repository;

    @Autowired
    private SpringDataWalletRepository springDataRepository;

    @Autowired
    private SpringDataAlumnoRepository springDataAlumnoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByStudentId_ShouldMapWalletEntityToDomain() {
        AlumnoEntity student = generateStudent();
        BilleteraEntity entity = walletEntity(student);
        springDataRepository.save(entity);
        entityManager.flush();
        entityManager.clear();

        Optional<Billetera> result = repository.findByStudentId(student.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals(new BigDecimal("120.00"), result.get().getSaldoCreditos());
        assertEquals(new BigDecimal("50.00"), result.get().getLimiteCredito());
        assertEquals(new BigDecimal("10.00"), result.get().getSaldoDeudaInterno());
        assertTrue(result.get().getCreditoHabilitado());
    }

    @Test
    void findByStudentId_WhenWalletDoesNotExist_ShouldReturnEmptyOptional() {
        UUID studentId = UUID.randomUUID();

        Optional<Billetera> result = repository.findByStudentId(studentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void save_ShouldUpdateExistingEntity() {
        AlumnoEntity student = generateStudent();
        BilleteraEntity entity = generateSavedWallet(student);
        entityManager.flush();
        entityManager.clear();

        Billetera walletUpdateCommand = createWalletUpdateCommand(entity.getId());

        repository.save(walletUpdateCommand);
        entityManager.flush();
        entityManager.clear();

        assertWalletWasUpdated(entity.getId(), walletUpdateCommand);
    }

    @Test
    void save_WhenWalletDoesNotExist_ShouldThrowException() {
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> repository.save(wallet));
    }

    @Test
    void findAlumnoIdsWithLowBalance_ShouldDelegateToSpringDataRepository() {
        AlumnoEntity student1 = generateStudent();
        BilleteraEntity entity1 = walletEntity(student1);
        entity1.setSaldoCreditos(new BigDecimal("20.00"));
        springDataRepository.save(entity1);

        AlumnoEntity student2 = generateStudent();
        BilleteraEntity entity2 = walletEntity(student2);
        entity2.setSaldoCreditos(new BigDecimal("50.00"));
        springDataRepository.save(entity2);

        entityManager.flush();
        entityManager.clear();

        BigDecimal threshold = new BigDecimal("30.00");

        List<UUID> result = repository.findAlumnoIdsWithLowBalance(threshold);

        assertEquals(1, result.size());
        assertTrue(result.contains(student1.getId()));
    }

    private AlumnoEntity generateStudent() {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Test");
        user.setApellido("User");
        user.setEmail("user" + UUID.randomUUID().toString().substring(0, 5) + "@test.com");
        user.setRol(RolUsuario.ALUMNO);
        user.setActivo(true);
        entityManager.persist(user);
        
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(user);
        student.setActivo(true);
        springDataAlumnoRepository.save(student);
        return student;
    }

    private BilleteraEntity walletEntity(AlumnoEntity student) {
        BilleteraEntity entity = new BilleteraEntity();
        entity.setAlumnoEntity(student);
        entity.setSaldoCreditos(new BigDecimal("120.00"));
        entity.setLimiteCredito(new BigDecimal("50.00"));
        entity.setSaldoDeudaInterno(new BigDecimal("10.00"));
        entity.setCreditoHabilitado(true);
        return entity;
    }

    private BilleteraEntity generateSavedWallet(AlumnoEntity student) {
        BilleteraEntity entity = walletEntity(student);
        return springDataRepository.save(entity);
    }

    private Billetera createWalletUpdateCommand(UUID walletId) {
        Billetera wallet = new Billetera();
        wallet.setId(walletId);
        wallet.setSaldoCreditos(new BigDecimal("75.00"));
        wallet.setLimiteCredito(new BigDecimal("25.00"));
        wallet.setSaldoDeudaInterno(new BigDecimal("5.00"));
        wallet.setCreditoHabilitado(false);
        return wallet;
    }

    private void assertWalletWasUpdated(UUID walletId, Billetera expected) {
        BilleteraEntity saved = springDataRepository.findById(walletId).orElseThrow();
        assertEquals(expected.getSaldoCreditos(), saved.getSaldoCreditos());
        assertEquals(expected.getLimiteCredito(), saved.getLimiteCredito());
        assertEquals(expected.getSaldoDeudaInterno(), saved.getSaldoDeudaInterno());
        assertEquals(expected.getCreditoHabilitado(), saved.getCreditoHabilitado());
    }
}
