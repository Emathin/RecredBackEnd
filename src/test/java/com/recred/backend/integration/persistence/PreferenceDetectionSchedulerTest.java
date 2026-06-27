package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.PreferenceDetectionScheduler;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Transactional
class PreferenceDetectionSchedulerTest extends AbstractIntegrationTest {

    @Autowired
    private PreferenceDetectionScheduler scheduler;

    @Autowired
    private EntityManager entityManager;

    private ColegioEntity cachedSchool;
    private BuffetEntity cachedBuffet;

    @Test
    void runPreferenceDetectionForAllActiveStudentsInAllBuffets() {
        BuffetEntity buffet = generateBuffetEntity();
        
        generateStudentEntity(true, "student.active.pref@test.com");
        
        generateStudentEntity(false, "student.inactive.pref@test.com");


        assertDoesNotThrow(() -> scheduler.runDetectionForAllStudents());
    }

    @Test
    void abortScheduledDetectionWhenNoBuffetsAreFound() {
        entityManager.createQuery("DELETE FROM BuffetEntity").executeUpdate();
        
        assertDoesNotThrow(() -> scheduler.runDetectionForAllStudents());
    }

    private ColegioEntity generateSchoolEntity() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("Test School Prefs");
            school.setCue("CUE-PREFS-123");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private BuffetEntity generateBuffetEntity() {
        if (this.cachedBuffet == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setColegioEntity(generateSchoolEntity());
            buffet.setNombre("Buffet Prefs");
            buffet.setActivo(true);
            entityManager.persist(buffet);
            this.cachedBuffet = buffet;
        }
        return this.cachedBuffet;
    }

    private UsuarioEntity generateUserEntity(String email) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-pref-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Student");
        user.setApellido("User");
        user.setEmail(email);
        user.setRol(RolUsuario.ALUMNO);
        user.setActivo(true);
        entityManager.persist(user);
        return user;
    }

    private AlumnoEntity generateStudentEntity(boolean active, String email) {
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(generateUserEntity(email));
        student.setActivo(active);
        entityManager.persist(student);
        return student;
    }
}
