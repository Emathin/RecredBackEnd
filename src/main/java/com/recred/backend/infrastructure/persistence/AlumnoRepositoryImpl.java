package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AlumnoRepositoryImpl implements AlumnoRepository {

    private final SpringDataAlumnoRepository springDataRepository;

    public AlumnoRepositoryImpl(SpringDataAlumnoRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Alumno> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(AlumnoEntity::toDomain);
    }

    @Override
    public List<Alumno> findAll() {
        return springDataRepository.findAll().stream()
                .map(AlumnoEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID alumnoId) {
        return springDataRepository.existsById(alumnoId);
    }

    @Override
    public Optional<UUID> findStudentIdByUserId(UUID userId) {
        return springDataRepository.findStudentIdByUserId(userId);
    }

    @Override
    public UUID findUserID(UUID alumnoId) {
        return springDataRepository.findUserId(alumnoId);
    }

    @Override
    public void save(Alumno alumno) {
        AlumnoEntity entityExistente = springDataRepository.findById(alumno.getId())
                .orElseGet(AlumnoEntity::new);

        AlumnoEntity entityLista = toEntity(alumno, entityExistente);

        springDataRepository.save(entityLista);
    }

    private AlumnoEntity toEntity(Alumno modelo, AlumnoEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        entity.setActivo(modelo.getActivo());

        if (modelo.getRestriccionesSalud() != null && !modelo.getRestriccionesSalud().isEmpty()) {
            Set<ClasificacionSaludEntity> restriccionesEntities = modelo
                    .getRestriccionesSalud().stream()
                    .map(restriccionDominio -> {
                        ClasificacionSaludEntity clasificacionEntity = new
                                ClasificacionSaludEntity();
                        clasificacionEntity.setId(restriccionDominio.getId());
                        clasificacionEntity.setDescripcion(restriccionDominio.getDescripcion());
                        clasificacionEntity.setActivo(restriccionDominio.getActivo());
                        return clasificacionEntity;
                    })
                    .collect(Collectors.toSet());

            entity.setRestriccionesSalud(restriccionesEntities);
        } else {
            if (entity.getRestriccionesSalud() != null) {
                entity.getRestriccionesSalud().clear();
            }
        }

        if (modelo.getProductosFavoritos() != null && !modelo.getProductosFavoritos().isEmpty()) {
            Set<ProductoEntity> productosProxies = modelo.getProductosFavoritos().stream()
                    .filter(p -> p.getId() != null)
                    .map(p -> {
                        ProductoEntity proxy = new ProductoEntity();
                        proxy.setId(p.getId());
                        return proxy;
                    })
                    .collect(Collectors.toSet());
            entity.setProductosFavoritos(productosProxies);
        } else {
            if (entity.getProductosFavoritos() != null) {
                entity.getProductosFavoritos().clear();
            }
        }

        if (modelo.getProductosBloqueados() != null && !modelo.getProductosBloqueados().isEmpty()) {
            Set<ProductoEntity> bloqueadosProxies = modelo.getProductosBloqueados().stream()
                    .filter(p -> p.getId() != null)
                    .map(p -> {
                        ProductoEntity proxy = new ProductoEntity();
                        proxy.setId(p.getId());
                        return proxy;
                    })
                    .collect(Collectors.toSet());
            entity.setProductosBloqueados(bloqueadosProxies);
        } else {
            if (entity.getProductosBloqueados() != null) {
                entity.getProductosBloqueados().clear();
            }
        }

        return entity;
    }
}
