package com.recred.backend.integration.persistence;

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

import static com.recred.backend.domain.models.EstadoPagoExterno.*;
import static com.recred.backend.domain.models.RolUsuario.*;
import static java.math.BigDecimal.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class SchoolAssistantPaymentJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private SchoolAssistantPaymentJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    private AlumnoEntity cachedStudent;

    @Test
    void findRecentByStudentId_ShouldUsePageRequestLimit() {
        AlumnoEntity student = givenStudentWithMultiplePayments();
        
        List<SchoolAssistantPayment> result = whenFindingRecentPayments(student.getId(), 1);
        
        thenResultSizeIsExactly(result, 1);
    }

    @Test
    void findByStudentIdAndDateRange_ShouldUsePageRequestLimit() {
        AlumnoEntity student = givenStudentWithMultiplePayments();
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusMinutes(5);
        
        List<SchoolAssistantPayment> result = whenFindingPaymentsInDateRange(student.getId(), start, end, 10);
        
        thenResultSizeIsExactly(result, 2);
    }

    private AlumnoEntity givenStudentWithMultiplePayments() {
        AlumnoEntity student = generateStudent();
        generatePayment(student, "100.00", "pref1", "pay1");
        generatePayment(student, "200.00", "pref2", "pay2");
        entityManager.flush();
        entityManager.clear();
        return student;
    }

    private List<SchoolAssistantPayment> whenFindingRecentPayments(java.util.UUID studentId, int limit) {
        return repository.findRecentByStudentId(studentId, limit);
    }

    private List<SchoolAssistantPayment> whenFindingPaymentsInDateRange(java.util.UUID studentId, LocalDateTime start, LocalDateTime end, int limit) {
        return repository.findByStudentIdAndDateRange(studentId, start, end, limit);
    }

    private void thenResultSizeIsExactly(List<SchoolAssistantPayment> result, int expectedSize) {
        assertEquals(expectedSize, result.size());
    }

    private PagoMercadoPagoEntity generatePayment(AlumnoEntity student, String amount, String prefId, String payId) {
        PagoMercadoPagoEntity payment = new PagoMercadoPagoEntity();
        payment.setBilleteraEntity(student.getBilleteraEntity());
        payment.setUsuarioEntity(student.getUsuario());
        payment.setMonto(new BigDecimal(amount));
        payment.setMoneda("ARS");
        payment.setEstado(APROBADO);
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
            user.setRol(ALUMNO);
            user.setActivo(true);
            entityManager.persist(user);

            AlumnoEntity student = new AlumnoEntity();
            student.setUsuario(user);
            student.setActivo(true);
            entityManager.persist(student);

            BilleteraEntity wallet = new BilleteraEntity();
            wallet.setSaldoCreditos(ZERO);
            wallet.setLimiteCredito(ZERO);
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
