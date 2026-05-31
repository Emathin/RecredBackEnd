package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.infrastructure.entities.ProductCategoryEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryRepositoryImplTest {

    @Mock
    private SpringDataCategoryRepository springDataRepository;

    private CategoryRepositoryImpl categoryRepository;

    @BeforeEach
    void setUp() {
        categoryRepository = new CategoryRepositoryImpl(springDataRepository);
    }

    @Test
    void findById_DeberiaLlamarAlSpringData() {
        UUID id = UUID.randomUUID();
        categoryRepository.findById(id);
        verify(springDataRepository).findById(id);
    }

    @Test
    void findByDescription_DeberiaLlamarAlSpringData() {
        String desc = "Test";
        categoryRepository.findByDescription(desc);
        verify(springDataRepository).findByDescripcion(desc);
    }

    @Test
    void save_DeberiaCrearNuevaEntidad_CuandoNoTieneId() {
        ProductCategory category = new ProductCategory();
        category.setDescription("Nueva");
        
        ProductCategoryEntity savedEntity = new ProductCategoryEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setDescription("Nueva");
        when(springDataRepository.save(any(ProductCategoryEntity.class))).thenReturn(savedEntity);

        categoryRepository.save(category);

        verify(springDataRepository).save(any(ProductCategoryEntity.class));
    }

    @Test
    void save_DeberiaActualizarEntidad_CuandoYaExiste() {
        UUID id = UUID.randomUUID();
        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setDescription("Actualizada");

        ProductCategoryEntity existingEntity = new ProductCategoryEntity();
        existingEntity.setDescription("Vieja");

        when(springDataRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(any(ProductCategoryEntity.class))).thenAnswer(i -> i.getArgument(0));

        categoryRepository.save(category);

        assertEquals("Actualizada", existingEntity.getDescription());
    }
}
