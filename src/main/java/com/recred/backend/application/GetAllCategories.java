package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.repositories.CategoryRepository;

import java.util.List;

public class GetAllCategories {

    private final CategoryRepository categoryRepository;

    public GetAllCategories(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public GetAllCategoriesResponse execute(GetAllCategoriesRequest request) {
        return new GetAllCategoriesResponse(categories().stream()
                .map(CategoryResponse::fromDomain)
                .toList());
    }

    private List<CategoriaProducto> categories() {
        return categoryRepository.getAllCategories();
    }
}
