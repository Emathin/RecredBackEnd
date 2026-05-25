package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionTipoEntity;
import com.recred.backend.infrastructure.entities.FranjaHorariaEntity;
import com.recred.backend.infrastructure.entities.RestriccionHorariaEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RestriccionHorariaRepositoryImpl implements
        RestriccionHorariaRepository {

    private final SpringDataRestriccionHorariaRepository springDataRepository;

    public RestriccionHorariaRepositoryImpl
            (SpringDataRestriccionHorariaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public RestriccionHoraria save(RestriccionHoraria restriccion) {
        RestriccionHorariaEntity entity = restriccion.getId() != null
                ? springDataRepository.findById(restriccion.getId())
                .orElse(new RestriccionHorariaEntity())
                : new RestriccionHorariaEntity();

        toEntity(restriccion, entity);
        RestriccionHorariaEntity entidadGuardada = springDataRepository.save(entity);

        return entidadGuardada.toDomain();
    }

    @Override
    public Optional<RestriccionHoraria> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(RestriccionHorariaEntity::toDomain);
    }

    @Override
    public List<RestriccionHoraria> findAllByAlumnoIdAndActiva
            (UUID alumnoId, boolean activa) {
        return springDataRepository.findAllByAlumnoEntityIdAndActiva(alumnoId, activa)
                .stream()
                .map(RestriccionHorariaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RestriccionHoraria> findAllByAlumnoId(UUID alumnoId) {
        return springDataRepository.findAllByAlumnoEntityId(alumnoId).stream()
                .map(RestriccionHorariaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }

    private void toEntity(RestriccionHoraria domain,
                          RestriccionHorariaEntity entity) {
        if (entity.getId() == null) {
            entity.setId(domain.getId());
        }
        entity.setActiva(domain.isActiva());

        if (domain.getAlumnoId() != null) {
            AlumnoEntity alumnoEntity = new AlumnoEntity();
            alumnoEntity.setId(domain.getAlumnoId());
            entity.setAlumnoEntity(alumnoEntity);
        }

        if (domain.getFranjaHoraria() != null) {
            FranjaHorariaEntity franjaEntity = new FranjaHorariaEntity();
            franjaEntity.setId(domain.getFranjaHoraria().getId());
            entity.setFranjaHorariaEntity(franjaEntity);
        }

        if (domain.getCategoria() != null) {
            CategoriaProductoEntity catEntity = new CategoriaProductoEntity();
            catEntity.setId(domain.getCategoria().getId());
            entity.setCategoria(catEntity);
        } else {
            entity.setCategoria(null);
        }

        if (domain.getClasificacion() != null) {
            ClasificacionTipoEntity tipoEntity = new ClasificacionTipoEntity();
            tipoEntity.setId(domain.getClasificacion().getId());
            entity.setClasificacionTipoEntity(tipoEntity);
        } else {
            entity.setClasificacionTipoEntity(null);
        }

        if (entity.getCreadoPorUsuarioEntity() == null) {
            UsuarioEntity usuarioDummy = new UsuarioEntity();
            usuarioDummy.setId(UUID.randomUUID());
            entity.setCreadoPorUsuarioEntity(usuarioDummy);
        }
    }
}