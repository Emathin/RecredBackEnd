package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.persistence.CategoryRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion("Nueva");
        
        CategoriaProductoEntity savedEntity = new CategoriaProductoEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setDescripcion("Nueva");
        when(springDataRepository.save(any(CategoriaProductoEntity.class))).thenReturn(savedEntity);

        categoryRepository.save(category);

        verify(springDataRepository).save(any(CategoriaProductoEntity.class));
    }

    @Test
    void save_DeberiaActualizarEntidad_CuandoYaExiste() {
        UUID id = UUID.randomUUID();
        CategoriaProducto category = new CategoriaProducto();
        category.setId(id);
        category.setDescripcion("Actualizada");

        CategoriaProductoEntity existingEntity = new CategoriaProductoEntity();
        existingEntity.setDescripcion("Vieja");

        when(springDataRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(any(CategoriaProductoEntity.class))).thenAnswer(i -> i.getArgument(0));

        categoryRepository.save(category);

        assertEquals("Actualizada", existingEntity.getDescripcion());
    }

    @Test
    void getAllCategories_DeberiaMapearTodasLasEntidades() {
        CategoriaProductoEntity entity = new CategoriaProductoEntity();
        entity.setId(UUID.randomUUID());
        entity.setDescripcion("Bebidas");
        entity.setActivo(true);
        when(springDataRepository.findAll()).thenReturn(List.of(entity));

        List<CategoriaProducto> result = categoryRepository.getAllCategories();

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
        assertEquals("Bebidas", result.get(0).getDescripcion());
        assertEquals(true, result.get(0).getActivo());
    }
}
