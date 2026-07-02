package com.recred.backend.unit.application;

import com.recred.backend.application.GetAllCategories;
import com.recred.backend.application.GetAllCategoriesRequest;
import com.recred.backend.application.GetAllCategoriesResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllCategoriesTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private GetAllCategories getAllCategories;

    @Test
    void execute_shouldReturnAllCategories() {
        CategoriaProducto cat1 = new CategoriaProducto();
        CategoriaProducto cat2 = new CategoriaProducto();
        List<CategoriaProducto> mockCats = List.of(cat1, cat2);

        when(categoryRepository.getAllCategories()).thenReturn(mockCats);

        GetAllCategoriesResponse result = getAllCategories.execute(
                new GetAllCategoriesRequest());

        assertNotNull(result);
        assertEquals(2, result.categories().size());
        verify(categoryRepository).getAllCategories();
    }
}
