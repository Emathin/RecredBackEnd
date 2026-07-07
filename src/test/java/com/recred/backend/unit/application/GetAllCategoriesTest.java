package com.recred.backend.unit.application;

import com.recred.backend.application.GetAllCategories;
import com.recred.backend.application.GetAllCategoriesRequest;
import com.recred.backend.application.GetAllCategoriesResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllCategoriesTest {

    @Mock
    private CategoryRepository categoryRepository;

    private GetAllCategories useCase;
    private GetAllCategoriesResponse response;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        useCase = new GetAllCategories(categoryRepository);
    }

    @Test
    void givenCategoriesExist_whenGettingCategories_thenReturnsAllCategories() {
        givenCategoriesExist();

        whenGetAllCategories();

        thenCategoriesWereReturned();
        thenCategoriesWereRequested();
    }

    private void givenCategoriesExist() {
        categoryId = randomUUID();
        when(categoryRepository.getAllCategories()).thenReturn(List.of(
                category(categoryId, "Bebidas", true),
                category(randomUUID(), "Snacks", false)
        ));
    }

    private void whenGetAllCategories() {
        response = useCase.execute(new GetAllCategoriesRequest());
    }

    private void thenCategoriesWereReturned() {
        assertEquals(2, response.categories().size());
        assertEquals(categoryId, response.categories().get(0).id());
        assertEquals("Bebidas", response.categories().get(0).descripcion());
        assertEquals(true, response.categories().get(0).activo());
    }

    private void thenCategoriesWereRequested() {
        verify(categoryRepository).getAllCategories();
    }

    private CategoriaProducto category(UUID requestedCategoryId, String description, boolean active) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(requestedCategoryId);
        category.setDescripcion(description);
        category.setActivo(active);
        return category;
    }
}
