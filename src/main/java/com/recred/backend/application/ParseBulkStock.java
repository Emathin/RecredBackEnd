package com.recred.backend.application;

import com.recred.backend.domain.exceptions.FileValidationException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import com.recred.backend.domain.repositories.ClasificacionTipoRepository;
import com.recred.backend.domain.services.BulkStockAnalyzer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ParseBulkStock {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "image/jpeg",
            "image/png"
    );

    private final BulkStockAnalyzer bulkStockAnalyzer;
    private final CategoryRepository categoryRepository;
    private final ClasificacionSaludRepository saludRepository;
    private final ClasificacionTipoRepository tipoRepository;

    public ParseBulkStock(
            BulkStockAnalyzer bulkStockAnalyzer,
            CategoryRepository categoryRepository,
            ClasificacionSaludRepository saludRepository,
            ClasificacionTipoRepository tipoRepository
    ) {
        this.bulkStockAnalyzer = bulkStockAnalyzer;
        this.categoryRepository = categoryRepository;
        this.saludRepository = saludRepository;
        this.tipoRepository = tipoRepository;
    }

    public BulkStockParseResponse execute(ParseBulkStockRequest parseRequest) {
        validateFile(parseRequest.fileContent(), parseRequest.contentType());
        List<String> availableCategories = findAvailableCategoryNames();
        List<RawBulkProduct> rawProducts = analyzeBulkStock(parseRequest, availableCategories);

        return new BulkStockParseResponse(resolveProducts(rawProducts));
    }

    private List<String> findAvailableCategoryNames() {
        return categoryRepository.getAllCategories().stream()
                .map(CategoriaProducto::getDescripcion)
                .toList();
    }

    private List<RawBulkProduct> analyzeBulkStock(
            ParseBulkStockRequest parseRequest,
            List<String> availableCategories
    ) {
        return bulkStockAnalyzer.analyze(
                parseRequest.fileContent(),
                parseRequest.contentType(),
                availableCategories
        );
    }

    private List<BulkProductData> resolveProducts(List<RawBulkProduct> rawProducts) {
        return rawProducts.stream()
                .map(this::resolveProduct)
                .toList();
    }

    private BulkProductData resolveProduct(RawBulkProduct rawProduct) {
        ResolvedCategory resolvedCategory = resolveCategory(rawProduct.categoriaNombre());
        Set<UUID> healthClassificationIds = resolveHealthClassifications(rawProduct);
        Set<UUID> typeClassificationIds = resolveTypeClassifications(rawProduct);

        return new BulkProductData(
                rawProduct.nombre(),
                rawProduct.descripcion(),
                rawProduct.precio(),
                rawProduct.peso(),
                rawProduct.requierePreparacion(),
                resolvedCategory.categoryId(),
                resolvedCategory.newCategoryName(),
                rawProduct.stockActual(),
                healthClassificationIds,
                typeClassificationIds
        );
    }

    private ResolvedCategory resolveCategory(String categoryName) {
        UUID categoryId = null;
        String newCategoryName = null;

        if (categoryName != null && !categoryName.isBlank()) {
            Optional<CategoriaProducto> category = categoryRepository
                    .findByDescription(categoryName);
            if (category.isPresent()) {
                categoryId = category.get().getId();
            } else {
                newCategoryName = categoryName;
            }
        }

        return new ResolvedCategory(categoryId, newCategoryName);
    }

    private Set<UUID> resolveHealthClassifications(RawBulkProduct rawProduct) {
        return rawProduct.saludEtiquetas().stream()
                .map(saludRepository::findByDescripcion)
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getId())
                .collect(Collectors.toSet());
    }

    private Set<UUID> resolveTypeClassifications(RawBulkProduct rawProduct) {
        return rawProduct.tipoEtiquetas().stream()
                .map(tipoRepository::findByDescripcion)
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getId())
                .collect(Collectors.toSet());
    }

    private void validateFile(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new FileValidationException("El archivo no puede estar vacío");
        }

        if (contentType == null || 
            !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileValidationException(
                    "Formato de archivo no permitido. " + 
                    "Se aceptan PDF, CSV e imágenes.");
        }
    }

    private record ResolvedCategory(UUID categoryId, String newCategoryName) {
    }
}
