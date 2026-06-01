package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetAllCategories;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final GetAllCategories getAllCategories;

    public CategoryController(
            GetAllCategories getAllCategories
    ) {
        this.getAllCategories = getAllCategories;
    }

    @GetMapping
    public List<CategoryResponse> getCategories() {
        return getAllCategories.execute()
                .stream()
                .map(CategoryResponse::fromDomain)
                .collect(Collectors.toList());
    }
}