package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.PresupuestoAlumnoEntity;
import com.recred.backend.infrastructure.entities.ReglaPresupuestoCategoriaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PresupuestoRepositoryImpl implements PresupuestoRepository {

    private final SpringDataPresupuestoRepository springDataRepository;

    public PresupuestoRepositoryImpl(SpringDataPresupuestoRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public PresupuestoAlumno save(PresupuestoAlumno presupuesto) {
        PresupuestoAlumnoEntity entityExistente = null;

        if (presupuesto.getId() != null) {
            entityExistente = springDataRepository.findById(presupuesto.getId())
                    .orElse(new PresupuestoAlumnoEntity());
        } else {
            entityExistente = new PresupuestoAlumnoEntity();
        }

        PresupuestoAlumnoEntity entityLista = toEntity(presupuesto, entityExistente);
        PresupuestoAlumnoEntity guardada = springDataRepository.save(entityLista);

        return guardada.toDomain();
    }

    @Override
    public Optional<PresupuestoAlumno> findActivoByAlumnoId(UUID alumnoId) {
        return springDataRepository.findByAlumnoEntityIdAndActivoTrue(alumnoId)
                .map(PresupuestoAlumnoEntity::toDomain);
    }

    private PresupuestoAlumnoEntity toEntity(PresupuestoAlumno modelo, PresupuestoAlumnoEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        if (modelo.getAlumnoId() != null) {
            AlumnoEntity alumno = new AlumnoEntity();
            alumno.setId(modelo.getAlumnoId());
            entity.setAlumnoEntity(alumno);
        }

        entity.setMontoLimite(modelo.getMontoLimiteGeneral());
        entity.setFechaInicio(modelo.getFechaInicio());
        entity.setActivo(modelo.isActivo());

        if (modelo.getPeriodo() != null) {
            entity.setPeriodo(BudgetPeriod.valueOf(modelo.getPeriodo().name()));
        }

        if (modelo.getReglasCategoria() != null && !modelo.getReglasCategoria().isEmpty()) {
            List<ReglaPresupuestoCategoriaEntity> subEntities = modelo.getReglasCategoria().stream()
                    .map(reglaDominio -> {
                        ReglaPresupuestoCategoriaEntity subEntity = new ReglaPresupuestoCategoriaEntity();
                        subEntity.setId(reglaDominio.getId());
                        subEntity.setPorcentajeLimite(reglaDominio.getPorcentajeLimite());
                        subEntity.setMontoLimiteCalculado(reglaDominio.getMontoLimiteCalculado());
                        subEntity.setActivo(reglaDominio.isActivo());
                        subEntity.setPresupuesto(entity);

                        if (reglaDominio.getCategoria() != null) {
                            CategoriaProductoEntity catEntity = new CategoriaProductoEntity();
                            catEntity.setId(reglaDominio.getCategoria().getId());
                            subEntity.setCategoria(catEntity);
                        }
                        return subEntity;
                    }).collect(Collectors.toList());

            if (entity.getReglasCategoria() != null) {
                entity.getReglasCategoria().clear();
                entity.getReglasCategoria().addAll(subEntities);
            } else {
                entity.setReglasCategoria(subEntities);
            }
        } else {
            if (entity.getReglasCategoria() != null) {
                entity.getReglasCategoria().clear();
            }
        }

        return entity;
    }
}