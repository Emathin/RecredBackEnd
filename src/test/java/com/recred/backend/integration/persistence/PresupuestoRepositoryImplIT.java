package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.models.RolUsuario;
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
class PresupuestoRepositoryImplIT extends AbstractIntegrationTest {

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
        PresupuestoAlumnoEntity entity = givenActiveBudgetForStudent();

        Optional<PresupuestoAlumno> result = whenFindingActiveBudget(entity.getAlumnoEntity().getId());

        thenActiveBudgetIsReturned(result, entity.getId());
    }

    @Test
    void findActivoByAlumnoId_SiNoExiste_RetornaEmpty() {
        AlumnoEntity student = givenStudentWithoutBudget();

        Optional<PresupuestoAlumno> result = whenFindingActiveBudget(student.getId());

        thenBudgetIsNotFound(result);
    }

    @Test
    void save_WhenBudgetHasReferencesAndRules_ShouldMapThemToEntity() {
        PresupuestoAlumno domain = givenNewBudgetDomainWithRules();

        PresupuestoAlumno saved = whenSavingBudget(domain);

        thenBudgetIsSavedCorrectlyWithRules(saved, domain);
    }

    @Test
    void save_WhenExistingBudgetHasRules_ShouldClearAndFlushBeforeReplacing() {
        PresupuestoAlumno domain = givenExistingBudgetForUpdateWithRules();

        whenSavingBudget(domain);

        thenExistingBudgetRulesAreReplaced(domain);
    }

    @Test
    void findById_WhenBudgetExists_ShouldReturnDomain() {
        PresupuestoAlumnoEntity entity = givenActiveBudgetForStudent();

        Optional<PresupuestoAlumno> result = whenFindingBudgetById(entity.getId());

        thenActiveBudgetIsReturned(result, entity.getId());
    }

    private PresupuestoAlumnoEntity givenActiveBudgetForStudent() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity parent = generateParentUserEntity();
        return generateBudgetEntity(student, parent, true);
    }

    private AlumnoEntity givenStudentWithoutBudget() {
        return generateStudentEntity();
    }

    private PresupuestoAlumno givenNewBudgetDomainWithRules() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity parent = generateParentUserEntity();
        CategoriaProductoEntity category = generateCategoryEntity();
        return createDomainBudget(null, student.getId(), parent.getId(), new BigDecimal("10000.00"), category.getId());
    }

    private PresupuestoAlumno givenExistingBudgetForUpdateWithRules() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity parent = generateParentUserEntity();
        CategoriaProductoEntity newCategory = generateCategoryEntity();
        
        PresupuestoAlumnoEntity existingEntity = generateBudgetEntity(student, parent, true);
        
        return createDomainBudget(existingEntity.getId(), student.getId(), parent.getId(), new BigDecimal("5000.00"), newCategory.getId());
    }

    private Optional<PresupuestoAlumno> whenFindingActiveBudget(UUID studentId) {
        return repository.findActivoByAlumnoId(studentId);
    }

    private Optional<PresupuestoAlumno> whenFindingBudgetById(UUID budgetId) {
        return repository.findById(budgetId);
    }

    private PresupuestoAlumno whenSavingBudget(PresupuestoAlumno domain) {
        PresupuestoAlumno saved = repository.save(domain);
        entityManager.flush();
        entityManager.clear();
        return saved;
    }

    private void thenActiveBudgetIsReturned(Optional<PresupuestoAlumno> result, UUID expectedId) {
        assertTrue(result.isPresent());
        assertEquals(expectedId, result.get().getId());
    }

    private void thenBudgetIsNotFound(Optional<PresupuestoAlumno> result) {
        assertTrue(result.isEmpty());
    }

    private void thenBudgetIsSavedCorrectlyWithRules(PresupuestoAlumno saved, PresupuestoAlumno domain) {
        assertNotNull(saved.getId());
        Optional<PresupuestoAlumnoEntity> entityOpt = springDataRepository.findById(saved.getId());
        assertTrue(entityOpt.isPresent());
        PresupuestoAlumnoEntity entity = entityOpt.get();

        assertEquals(domain.getAlumnoId(), entity.getAlumnoEntity().getId());
        assertEquals(domain.getCreadoPorUsuarioId(), entity.getCreadoPorUsuarioEntity().getId());
        assertEquals(domain.getMontoLimiteGeneral(), entity.getMontoLimite());
        assertEquals(1, entity.getReglasCategoria().size());
        assertEquals(domain.getReglasCategoria().get(0).getCategoria().getId(), entity.getReglasCategoria().get(0).getCategoria().getId());
        assertEquals(entity, entity.getReglasCategoria().get(0).getPresupuesto());
    }

    private void thenExistingBudgetRulesAreReplaced(PresupuestoAlumno domain) {
        PresupuestoAlumnoEntity updatedEntity = springDataRepository.findById(domain.getId()).get();
        assertEquals(1, updatedEntity.getReglasCategoria().size());
        assertEquals(domain.getReglasCategoria().get(0).getCategoria().getId(), updatedEntity.getReglasCategoria().get(0).getCategoria().getId());
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

    private PresupuestoAlumno createDomainBudget(UUID id, UUID studentId, UUID parentId, BigDecimal limit, UUID categoryId) {
        PresupuestoAlumno domain = new PresupuestoAlumno();
        domain.setId(id);
        domain.setAlumnoId(studentId);
        domain.setCreadoPorUsuarioId(parentId);
        domain.setMontoLimiteGeneral(limit);
        domain.setPeriodo(PeriodoPresupuesto.MENSUAL);
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
}
