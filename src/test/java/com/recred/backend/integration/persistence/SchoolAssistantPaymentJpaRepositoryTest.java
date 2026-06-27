package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BilleteraEntity;
import com.recred.backend.infrastructure.entities.PagoMercadoPagoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SchoolAssistantPaymentJpaRepository;
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
class SchoolAssistantPaymentJpaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private SchoolAssistantPaymentJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findRecentByStudentId_ShouldUsePageRequestLimit() {
        AlumnoEntity student = generateStudent();

        generatePayment(student, "100.00", "pref1", "pay1");
        generatePayment(student, "200.00", "pref2", "pay2");

        entityManager.flush();
        entityManager.clear();

        List<SchoolAssistantPayment> result = repository.findRecentByStudentId(student.getId(), 1);

        assertEquals(1, result.size());
    }

    @Test
    void findByStudentIdAndDateRange_ShouldUsePageRequestLimit() {
        AlumnoEntity student = generateStudent();

        LocalDateTime start = LocalDateTime.now().minusMinutes(5);

        generatePayment(student, "100.00", "pref3", "pay3");
        generatePayment(student, "200.00", "pref4", "pay4");

        entityManager.flush();
        entityManager.clear();
        
        LocalDateTime end = LocalDateTime.now().plusMinutes(5);

        List<SchoolAssistantPayment> result = repository.findByStudentIdAndDateRange(
                student.getId(),
                start,
                end,
                10
        );

        assertEquals(2, result.size());
    }

    private AlumnoEntity cachedStudent;

    private PagoMercadoPagoEntity generatePayment(AlumnoEntity student, String amount, String prefId, String payId) {
        PagoMercadoPagoEntity payment = new PagoMercadoPagoEntity();
        payment.setBilleteraEntity(student.getBilleteraEntity());
        payment.setUsuarioEntity(student.getUsuario());
        payment.setMonto(new BigDecimal(amount));
        payment.setMoneda("ARS");
        payment.setEstado(EstadoPagoExterno.APROBADO);
        payment.setMercadoPagoPreferenceId(prefId);
        payment.setMercadoPagoPaymentId(payId);
        entityManager.persist(payment);
        return payment;
    }

    private AlumnoEntity generateStudent() {
        if (this.cachedStudent == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-student-pay-test");
            user.setNombre("Test");
            user.setApellido("User");
            user.setEmail("user.pay@test.com");
            user.setRol(RolUsuario.ALUMNO);
            user.setActivo(true);
            entityManager.persist(user);

            AlumnoEntity student = new AlumnoEntity();
            student.setUsuario(user);
            student.setActivo(true);
            entityManager.persist(student);

            BilleteraEntity wallet = new BilleteraEntity();
            wallet.setSaldoCreditos(BigDecimal.ZERO);
            wallet.setLimiteCredito(BigDecimal.ZERO);
            wallet.setCreditoHabilitado(true);
            wallet.setAlumnoEntity(student);
            entityManager.persist(wallet);

            student.setBilleteraEntity(wallet);
            entityManager.merge(student);

            this.cachedStudent = student;
        }
        return this.cachedStudent;
    }
}
