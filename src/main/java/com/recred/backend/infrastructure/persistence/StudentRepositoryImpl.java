package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.HealthClassificationEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StudentRepositoryImpl implements StudentRepository {

    private final SpringDataAlumnoRepository springDataRepository;

    public StudentRepositoryImpl(SpringDataAlumnoRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Student> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(StudentEntity::toDomain);
    }

    @Override
    public List<Student> findAll() {
        return springDataRepository.findAll().stream()
                .map(StudentEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID alumnoId) {
        return springDataRepository.existsById(alumnoId);
    }

    @Override
    public void save(Student student) {
        StudentEntity entityExistente = springDataRepository.findById(student.getId())
                .orElseGet(StudentEntity::new);

        StudentEntity entityLista = toEntity(student, entityExistente);

        springDataRepository.save(entityLista);
    }

    private StudentEntity toEntity(Student modelo, StudentEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        entity.setActive(modelo.getActive());

        if (modelo.getHealthRestrictions() != null && !modelo.getHealthRestrictions().isEmpty()) {
            Set<HealthClassificationEntity> restriccionesEntities = modelo
                    .getHealthRestrictions().stream()
                    .map(restriccionDominio -> {
                        HealthClassificationEntity clasificacionEntity = new
                                HealthClassificationEntity();
                        clasificacionEntity.setId(restriccionDominio.getId());
                        clasificacionEntity.setDescription(restriccionDominio.getDescription());
                        clasificacionEntity.setActive(restriccionDominio.getActive());
                        return clasificacionEntity;
                    })
                    .collect(Collectors.toSet());

            entity.setHealthRestrictions(restriccionesEntities);
        } else {
            if (entity.getHealthRestrictions() != null) {
                entity.getHealthRestrictions().clear();
            }
        }

        return entity;
    }
}
