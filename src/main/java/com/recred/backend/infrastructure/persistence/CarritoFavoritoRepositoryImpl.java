package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.CarritoFavoritoRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CarritoFavoritoEntity;
import com.recred.backend.infrastructure.entities.CarritoFavoritoItemEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CarritoFavoritoRepositoryImpl implements CarritoFavoritoRepository {

    private final SpringDataCarritoFavoritoRepository springDataRepository;
    private final SpringDataTutorRepository springDataTutorRepository;
    private final SpringDataAlumnoRepository springDataAlumnoRepository;
    private final SpringDataProductRepository springDataProductRepository;

    public CarritoFavoritoRepositoryImpl(
            SpringDataCarritoFavoritoRepository springDataRepository,
            SpringDataTutorRepository springDataTutorRepository,
            SpringDataAlumnoRepository springDataAlumnoRepository,
            SpringDataProductRepository springDataProductRepository) {
        this.springDataRepository = springDataRepository;
        this.springDataTutorRepository = springDataTutorRepository;
        this.springDataAlumnoRepository = springDataAlumnoRepository;
        this.springDataProductRepository = springDataProductRepository;
    }

    @Override
    @Transactional
    public CarritoFavorito save(CarritoFavorito model) {
        CarritoFavoritoEntity entity;
        if (model.getId() != null) {
            entity = springDataRepository.findById(model.getId())
                    .orElseGet(CarritoFavoritoEntity::new);
        } else {
            entity = new CarritoFavoritoEntity();
        }

        entity.setNombre(model.getNombre());

        if (model.getTutor() != null && model.getTutor().getId() != null) {
            TutorEntity tutorEntity = springDataTutorRepository
                    .getReferenceById(model.getTutor().getId());
            entity.setTutor(tutorEntity);
        }

        if (model.getAlumno() != null && model.getAlumno().getId() != null) {
            AlumnoEntity alumnoEntity = springDataAlumnoRepository
                    .getReferenceById(model.getAlumno().getId());
            entity.setAlumno(alumnoEntity);
        }

        entity.getItems().clear();
        if (model.getItems() != null) {
            for (var entry : model.getItems().entrySet()) {
                Producto producto = entry.getKey();
                Integer cantidad = entry.getValue();
                if (producto != null && producto.getId() != null) {
                    ProductoEntity prodEntity = springDataProductRepository
                            .getReferenceById(producto.getId());
                    CarritoFavoritoItemEntity itemEntity = new CarritoFavoritoItemEntity();
                    itemEntity.setProductoEntity(prodEntity);
                    itemEntity.setCantidad(cantidad);
                    entity.addItem(itemEntity);
                }
            }
        }

        CarritoFavoritoEntity savedEntity = springDataRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<CarritoFavorito> findById(UUID id) {
        return springDataRepository.findById(id).map(CarritoFavoritoEntity::toDomain);
    }

    @Override
    public List<CarritoFavorito> findByTutorIdAndFilters(
            UUID tutorId, UUID alumnoId, UUID productoId) {
        return springDataRepository.findByTutorIdAndFilters(tutorId, alumnoId, productoId)
                .stream()
                .map(CarritoFavoritoEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        springDataRepository.deleteById(id);
    }
}
