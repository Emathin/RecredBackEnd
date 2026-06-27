package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import com.recred.backend.infrastructure.entities.CompraPresencialEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataPurchaseRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class SpringDataPurchaseRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private SpringDataPurchaseRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findRecentByStudentIdWithLimitDelegatesToPageableQuery() {
        AlumnoEntity student = generateStudent();
        
        CompraPresencialEntity purchase = generatePurchase(student, "150.00", EstadoCompra.ENTREGADO, LocalDateTime.now().minusDays(1));
        CompraPresencialEntity recentPurchase = generatePurchase(student, "200.00", EstadoCompra.PENDIENTE, LocalDateTime.now());
        
        entityManager.flush();
        entityManager.clear();

        List<SchoolAssistantPurchase> result = repository.findRecentByStudentId(student.getId(), 1);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("200.00"), result.get(0).totalAmount());
        assertEquals(EstadoCompra.PENDIENTE, result.get(0).status());
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
        entityManager.persist(student);
        return student;
    }

    private BuffetEntity cachedBuffet;

    private BuffetEntity getCachedBuffet() {
        if (this.cachedBuffet == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("School Test");
            school.setCue("CUE-TEST-123");
            school.setActivo(true);
            entityManager.persist(school);
            
            UsuarioEntity vendorUser = new UsuarioEntity();
            vendorUser.setIdCognito("sub-vendor-test");
            vendorUser.setNombre("Vendor");
            vendorUser.setApellido("User");
            vendorUser.setEmail("vendor@test.com");
            vendorUser.setRol(RolUsuario.VENDEDOR);
            vendorUser.setActivo(true);
            entityManager.persist(vendorUser);
            
            BuffetEntity buffet = new BuffetEntity();
            buffet.setNombre("Test Buffet");
            buffet.setColegioEntity(school);
            buffet.setActivo(true);
            entityManager.persist(buffet);
            
            KiosqueroEntity vendor = new KiosqueroEntity();
            vendor.setUsuario(vendorUser);
            vendor.setBuffetEntity(buffet);
            vendor.setCuit("123456789");
            entityManager.persist(vendor);

            buffet.setVendedor(vendor);
            entityManager.merge(buffet);
            
            this.cachedBuffet = buffet;
        }
        return this.cachedBuffet;
    }

    private CompraPresencialEntity generatePurchase(AlumnoEntity student, String amount, EstadoCompra status, LocalDateTime date) {
        CompraPresencialEntity purchase = new CompraPresencialEntity();
        purchase.setAlumnoEntity(student);
        purchase.setBuffetEntity(getCachedBuffet());
        purchase.setMontoTotal(new BigDecimal(amount));
        purchase.setEstado(status);
        purchase.setMedioPago(MedioPago.CREDITOS);
        purchase.setFechaHora(date);
        entityManager.persist(purchase);
        return purchase;
    }
}
