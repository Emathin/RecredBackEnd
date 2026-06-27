package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.*;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.PresupuestoAlumnoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.PresupuestoRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataPresupuestoRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class PresupuestoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private PresupuestoRepositoryImpl repository;

    @Autowired
    private SpringDataPresupuestoRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity cachedStudentUser;
    private AlumnoEntity cachedStudent;
    private UsuarioEntity cachedParent;

    @Test
    void findActivoByAlumnoId_SiExiste_RetornaDominioMapeado() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity parent = generateParentUserEntity();
        PresupuestoAlumnoEntity entity = generateBudgetEntity(student, parent, true);

        Optional<PresupuestoAlumno> result = repository.findActivoByAlumnoId(student.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
    }

    @Test
    void findActivoByAlumnoId_SiNoExiste_RetornaEmpty() {
        AlumnoEntity student = generateStudentEntity();

        Optional<PresupuestoAlumno> result = repository.findActivoByAlumnoId(student.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void save_WhenBudgetHasReferencesAndRules_ShouldMapThemToEntity() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity parent = generateParentUserEntity();
        CategoriaProductoEntity category = generateCategoryEntity();

        PresupuestoAlumno domain = createDomainBudget(null, student.getId(), parent.getId(), new BigDecimal("10000.00"), PeriodoPresupuesto.MENSUAL, category.getId());

        PresupuestoAlumno saved = repository.save(domain);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(saved.getId());

        assertBudgetSavedCorrectly(saved, student, parent, category);
    }

    @Test
    void save_WhenExistingBudgetHasRules_ShouldClearAndFlushBeforeReplacing() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity parent = generateParentUserEntity();
        CategoriaProductoEntity cat1 = generateCategoryEntity();
        
        PresupuestoAlumnoEntity existingEntity = generateBudgetEntity(student, parent, true);
        
        PresupuestoAlumno domain = createDomainBudget(existingEntity.getId(), student.getId(), parent.getId(), new BigDecimal("5000.00"), PeriodoPresupuesto.MENSUAL, cat1.getId());

        repository.save(domain);
        entityManager.flush();
        entityManager.clear();

        PresupuestoAlumnoEntity updatedEntity = springDataRepository.findById(existingEntity.getId()).get();
        assertEquals(1, updatedEntity.getReglasCategoria().size());
        assertEquals(cat1.getId(), updatedEntity.getReglasCategoria().get(0).getCategoria().getId());
    }

    @Test
    void findById_WhenBudgetExists_ShouldReturnDomain() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity parent = generateParentUserEntity();
        PresupuestoAlumnoEntity entity = generateBudgetEntity(student, parent, true);

        Optional<PresupuestoAlumno> result = repository.findById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
    }

    private UsuarioEntity generateStudentUserEntity() {
        if (this.cachedStudentUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-budget-stu-" + UUID.randomUUID().toString().substring(0, 8));
            user.setNombre("Student");
            user.setApellido("Budget");
            user.setEmail("student.budget@test.com");
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

    private UsuarioEntity generateParentUserEntity() {
        if (this.cachedParent == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-parent-bud-" + UUID.randomUUID().toString().substring(0, 8));
            user.setNombre("Parent");
            user.setApellido("Budget");
            user.setEmail("parent.budget@test.com");
            user.setRol(RolUsuario.PADRE);
            user.setActivo(true);
            entityManager.persist(user);
            this.cachedParent = user;
        }
        return this.cachedParent;
    }

    private CategoriaProductoEntity generateCategoryEntity() {
        CategoriaProductoEntity cat = new CategoriaProductoEntity();
        cat.setDescripcion("Snacks");
        cat.setActivo(true);
        entityManager.persist(cat);
        return cat;
    }

    private PresupuestoAlumnoEntity generateBudgetEntity(AlumnoEntity student, UsuarioEntity parent, boolean active) {
        PresupuestoAlumnoEntity entity = new PresupuestoAlumnoEntity();
        entity.setAlumnoEntity(student);
        entity.setCreadoPorUsuarioEntity(parent);
        entity.setActivo(active);
        entity.setMontoLimite(new BigDecimal("500.00"));
        entity.setPeriodo(BudgetPeriod.SEMANAL);
        entity.setFechaInicio(LocalDate.now());
        return springDataRepository.save(entity);
    }

    private PresupuestoAlumno createDomainBudget(UUID id, UUID studentId, UUID parentId, BigDecimal limit, PeriodoPresupuesto period, UUID categoryId) {
        PresupuestoAlumno domain = new PresupuestoAlumno();
        domain.setId(id);
        domain.setAlumnoId(studentId);
        domain.setCreadoPorUsuarioId(parentId);
        domain.setMontoLimiteGeneral(limit);
        domain.setPeriodo(period);
        domain.setFechaInicio(LocalDate.now());
        if (categoryId != null) {
            domain.setReglasCategoria(List.of(rule(categoryId)));
        }
        return domain;
    }

    private ReglaPresupuestoCategoria rule(UUID categoryId) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        return new ReglaPresupuestoCategoria(
                null,
                category,
                new BigDecimal("30.00"),
                new BigDecimal("3000.00"),
                true
        );
    }

    private void assertBudgetSavedCorrectly(PresupuestoAlumno saved, AlumnoEntity student, UsuarioEntity parent, CategoriaProductoEntity category) {
        Optional<PresupuestoAlumnoEntity> entityOpt = springDataRepository.findById(saved.getId());
        assertTrue(entityOpt.isPresent());
        PresupuestoAlumnoEntity entity = entityOpt.get();

        assertEquals(student.getId(), entity.getAlumnoEntity().getId());
        assertEquals(parent.getId(), entity.getCreadoPorUsuarioEntity().getId());
        assertEquals(new BigDecimal("10000.00"), entity.getMontoLimite());
        assertEquals(1, entity.getReglasCategoria().size());
        assertEquals(category.getId(), entity.getReglasCategoria().get(0).getCategoria().getId());
        assertEquals(entity, entity.getReglasCategoria().get(0).getPresupuesto());
    }
}
