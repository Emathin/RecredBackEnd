package com.recred.backend.application;

public record LoadStockIAResponse(
    String name,
    String description,
    String weight,
    String containsSugar,
    String containsPeanuts,
    String containsLactose,
    String containsGluten,
    String imageUrl
) {}
