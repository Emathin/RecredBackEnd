package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.repositories.BudgetRepository;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.ProductCategoryEntity;
import com.recred.backend.infrastructure.entities.StudentBudgetEntity;
import com.recred.backend.infrastructure.entities.BudgetCategoryRuleEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BudgetRepositoryImpl implements BudgetRepository {

    private final SpringDataPresupuestoRepository springDataRepository;

    public BudgetRepositoryImpl(SpringDataPresupuestoRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public StudentBudget save(StudentBudget presupuesto) {
        StudentBudgetEntity entityExistente = null;

        if (presupuesto.getId() != null) {
            entityExistente = springDataRepository.findById(presupuesto.getId())
                    .orElse(new StudentBudgetEntity());
        } else {
            entityExistente = new StudentBudgetEntity();
        }

        StudentBudgetEntity entityLista = toEntity(presupuesto, entityExistente);
        StudentBudgetEntity guardada = springDataRepository.save(entityLista);

        return guardada.toDomain();
    }

    @Override
    public Optional<StudentBudget> findByActiveStudentID(UUID alumnoId) {
        return springDataRepository.findByAlumnoEntityIdAndActivoTrue(alumnoId)
                .map(StudentBudgetEntity::toDomain);
    }

    @Override
    public Optional<StudentBudget> findById(UUID budgetId) {
        return springDataRepository.findById(budgetId)
                .map(StudentBudgetEntity::toDomain);
    }

    private StudentBudgetEntity toEntity(StudentBudget modelo,
                                         StudentBudgetEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        if (modelo.getStudentId() != null) {
            StudentEntity alumno = new StudentEntity();
            alumno.setId(modelo.getStudentId());
            entity.setStudentEntity(alumno);
        }

        entity.setLimitAmount(modelo.getLimitAmount());
        entity.setStartDate(modelo.getStartDate());
        entity.setActive(modelo.isActive());

        if (modelo.getPeriod() != null) {
            entity.setPeriod(BudgetPeriod.valueOf(modelo.getPeriod().name()));
        }

        if (modelo.getCategoryRules() != null && !modelo.getCategoryRules().isEmpty()) {
            List<BudgetCategoryRuleEntity> subEntities =
                    modelo.getCategoryRules().stream()
                    .map(reglaDominio -> {
                        BudgetCategoryRuleEntity subEntity = new
                                BudgetCategoryRuleEntity();
                        subEntity.setId(reglaDominio.getId());
                        subEntity.setPercentageLimit(reglaDominio.getLimitAverage());
                        subEntity.setCalculatedLimitAmount(reglaDominio.getCalculatedLimitAmount());
                        subEntity.setActive(reglaDominio.isActive());
                        subEntity.setBudget(entity);

                        if (reglaDominio.getCategory() != null) {
                            ProductCategoryEntity catEntity = new ProductCategoryEntity();
                            catEntity.setId(reglaDominio.getCategory().getId());
                            subEntity.setCategory(catEntity);
                        }
                        return subEntity;
                    }).collect(Collectors.toList());

            if (entity.getCategoryRules() != null) {
                entity.getCategoryRules().clear();
                entity.getCategoryRules().addAll(subEntities);
            } else {
                entity.setCategoryRules(subEntities);
            }
        } else {
            if (entity.getCategoryRules() != null) {
                entity.getCategoryRules().clear();
            }
        }

        return entity;
    }
}