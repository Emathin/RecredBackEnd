package com.recred.backend.integration.persistence;

import com.recred.backend.infrastructure.persistence.*;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CarritoFavoritoEntity;
import com.recred.backend.infrastructure.entities.CarritoFavoritoItemEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarritoFavoritoRepositoryImplTest {

    @Mock
    private SpringDataCarritoFavoritoRepository springDataRepository;

    @Mock
    private SpringDataTutorRepository springDataTutorRepository;

    @Mock
    private SpringDataAlumnoRepository springDataAlumnoRepository;

    @Mock
    private SpringDataProductRepository springDataProductRepository;

    private CarritoFavoritoRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CarritoFavoritoRepositoryImpl(
                springDataRepository,
                springDataTutorRepository,
                springDataAlumnoRepository,
                springDataProductRepository
        );
    }

    @Test
    void saveShouldCreateEntityWithReferencesAndItems() {
        UUID tutorId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CarritoFavorito cart = cart(null, tutorId, alumnoId, productId, 2);
        TutorEntity tutorEntity = tutorEntity(tutorId);
        AlumnoEntity alumnoEntity = alumnoEntity(alumnoId);
        ProductoEntity productEntity = productEntity(productId);

        when(springDataTutorRepository.getReferenceById(tutorId)).thenReturn(tutorEntity);
        when(springDataAlumnoRepository.getReferenceById(alumnoId)).thenReturn(alumnoEntity);
        when(springDataProductRepository.getReferenceById(productId)).thenReturn(productEntity);
        when(springDataRepository.save(any(CarritoFavoritoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CarritoFavorito result = repository.save(cart);

        ArgumentCaptor<CarritoFavoritoEntity> captor = ArgumentCaptor.forClass(
                CarritoFavoritoEntity.class
        );
        verify(springDataRepository).save(captor.capture());
        CarritoFavoritoEntity saved = captor.getValue();
        assertEquals("Merienda", saved.getNombre());
        assertEquals(tutorId, saved.getTutor().getId());
        assertEquals(alumnoId, saved.getAlumno().getId());
        assertEquals(1, saved.getItems().size());
        assertEquals(productId, saved.getItems().get(0).getProductoEntity().getId());
        assertEquals(2, saved.getItems().get(0).getCantidad());
        assertEquals("Merienda", result.getNombre());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void saveShouldUpdateExistingEntityAndClearOldItems() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CarritoFavoritoEntity existing = new CarritoFavoritoEntity();
        existing.setId(cartId);
        existing.setNombre("Viejo");
        existing.addItem(new CarritoFavoritoItemEntity());
        CarritoFavorito cart = cart(cartId, null, null, productId, 3);
        ProductoEntity productEntity = productEntity(productId);

        when(springDataRepository.findById(cartId)).thenReturn(Optional.of(existing));
        when(springDataProductRepository.getReferenceById(productId)).thenReturn(productEntity);
        when(springDataRepository.save(existing)).thenReturn(existing);

        CarritoFavorito result = repository.save(cart);

        assertEquals("Merienda", result.getNombre());
        assertEquals(1, existing.getItems().size());
        assertEquals(productId, existing.getItems().get(0).getProductoEntity().getId());
        verify(springDataRepository).save(existing);
    }

    @Test
    void saveShouldIgnoreItemsWithoutProductId() {
        CarritoFavorito cart = new CarritoFavorito();
        cart.setNombre("Merienda");
        cart.setItems(Map.of(new Producto(), 1));
        when(springDataRepository.save(any(CarritoFavoritoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CarritoFavorito result = repository.save(cart);

        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void findByIdShouldMapEntityWhenPresent() {
        UUID cartId = UUID.randomUUID();
        CarritoFavoritoEntity entity = new CarritoFavoritoEntity();
        entity.setId(cartId);
        entity.setNombre("Merienda");
        when(springDataRepository.findById(cartId)).thenReturn(Optional.of(entity));

        Optional<CarritoFavorito> result = repository.findById(cartId);

        assertTrue(result.isPresent());
        assertEquals(cartId, result.orElseThrow().getId());
    }

    @Test
    void findByTutorIdAndFiltersShouldMapEntities() {
        UUID tutorId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CarritoFavoritoEntity entity = new CarritoFavoritoEntity();
        entity.setId(UUID.randomUUID());
        entity.setNombre("Merienda");
        when(springDataRepository.findByTutorIdAndFilters(tutorId, alumnoId, productId))
                .thenReturn(List.of(entity));

        List<CarritoFavorito> result = repository.findByTutorIdAndFilters(
                tutorId,
                alumnoId,
                productId
        );

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
    }

    @Test
    void deleteShouldDelegateToSpringDataRepository() {
        UUID id = UUID.randomUUID();

        repository.delete(id);

        verify(springDataRepository).deleteById(id);
    }

    private CarritoFavorito cart(
            UUID cartId,
            UUID tutorId,
            UUID alumnoId,
            UUID productId,
            int quantity
    ) {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Producto product = new Producto();
        product.setId(productId);

        CarritoFavorito cart = new CarritoFavorito();
        cart.setId(cartId);
        cart.setNombre("Merienda");
        cart.setTutor(tutor);
        cart.setAlumno(alumno);
        cart.setItems(Map.of(product, quantity));
        return cart;
    }

    private TutorEntity tutorEntity(UUID id) {
        TutorEntity entity = new TutorEntity();
        entity.setId(id);
        return entity;
    }

    private AlumnoEntity alumnoEntity(UUID id) {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(id);
        return entity;
    }

    private ProductoEntity productEntity(UUID id) {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        entity.setNombre("Alfajor");
        entity.setDescripcion("Alfajor simple");
        entity.setPrecio(new BigDecimal("40.00"));
        entity.setActivo(true);
        entity.setRequierePreparacion(false);
        return entity;
    }
}
