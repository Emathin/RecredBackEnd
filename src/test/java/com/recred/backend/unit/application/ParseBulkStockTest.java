package com.recred.backend.unit.application;

import com.recred.backend.application.BulkProductData;
import com.recred.backend.application.BulkStockParseResponse;
import com.recred.backend.application.ParseBulkStock;
import com.recred.backend.application.ParseBulkStockRequest;
import com.recred.backend.application.RawBulkProduct;
import com.recred.backend.domain.exceptions.FileValidationException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import com.recred.backend.domain.repositories.ClasificacionTipoRepository;
import com.recred.backend.domain.services.BulkStockAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ParseBulkStockTest {

    private final BulkStockAnalyzer bulkStockAnalyzer = mock(BulkStockAnalyzer.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ClasificacionSaludRepository saludRepository =
            mock(ClasificacionSaludRepository.class);
    private final ClasificacionTipoRepository tipoRepository =
            mock(ClasificacionTipoRepository.class);
    private final ParseBulkStock useCase = new ParseBulkStock(
            bulkStockAnalyzer,
            categoryRepository,
            saludRepository,
            tipoRepository
    );

    private byte[] fileContent;
    private String contentType;
    private CategoriaProducto category;
    private RawBulkProduct rawProduct;
    private ClasificacionSalud healthClassification;
    private ClasificacionTipo typeClassification;
    private BulkStockParseResponse response;
    private FileValidationException exception;

    @Test
    void execute_WhenFileIsValid_ReturnsResolvedProducts() {
        givenValidPdfFile();
        givenExistingCategory("Bebidas");
        givenAnalyzerReturnsProduct("Coca Cola", "Bebidas");
        givenKnownHealthClassification("Sin TACC");
        givenKnownTypeClassification("Bebida");

        whenParsingBulkStock();

        thenProductWasResolvedWith(
                "Coca Cola",
                category.getId(),
                null,
                healthClassification.getId(),
                typeClassification.getId()
        );
        thenAnalyzerWasCalledWithAvailableCategories(List.of("Bebidas"));
    }

    @Test
    void execute_WhenFileIsEmpty_ThrowsException() {
        givenEmptyPdfFile();

        whenParsingBulkStockFails();

        thenExceptionMessageIs("El archivo no puede estar vacío");
        thenAnalyzerWasNotCalled();
    }

    @Test
    void execute_WhenFormatIsNotAllowed_ThrowsException() {
        givenFileWithInvalidFormat();

        whenParsingBulkStockFails();

        thenExceptionMessageIs("Formato de archivo no permitido. Se aceptan PDF, CSV e imágenes.");
        thenAnalyzerWasNotCalled();
    }

    @Test
    void execute_WhenCategoryDoesNotExist_ReturnsNewCategoryName() {
        givenValidCsvFile();
        givenNoAvailableCategories();
        givenAnalyzerReturnsProduct("Alfajor", "Golosinas");
        givenCategoryDoesNotExist("Golosinas");

        whenParsingBulkStock();

        thenProductWasResolvedWith("Alfajor", null, "Golosinas", null, null);
    }

    private void givenValidPdfFile() {
        fileContent = "dummy content".getBytes();
        contentType = "application/pdf";
    }

    private void givenValidCsvFile() {
        fileContent = "dummy".getBytes();
        contentType = "text/csv";
    }

    private void givenEmptyPdfFile() {
        fileContent = new byte[0];
        contentType = "application/pdf";
    }

    private void givenFileWithInvalidFormat() {
        fileContent = "content".getBytes();
        contentType = "application/invalid";
    }

    private void givenExistingCategory(String categoryName) {
        category = category(categoryName);
        when(categoryRepository.getAllCategories()).thenReturn(List.of(category));
        when(categoryRepository.findByDescription(categoryName)).thenReturn(Optional.of(category));
    }

    private void givenNoAvailableCategories() {
        when(categoryRepository.getAllCategories()).thenReturn(List.of());
    }

    private void givenCategoryDoesNotExist(String categoryName) {
        when(categoryRepository.findByDescription(categoryName)).thenReturn(Optional.empty());
    }

    private void givenAnalyzerReturnsProduct(String productName, String categoryName) {
        rawProduct = rawProduct(productName, categoryName);
        when(bulkStockAnalyzer.analyze(fileContent, contentType, availableCategoryNames()))
                .thenReturn(List.of(rawProduct));
    }

    private void givenKnownHealthClassification(String classificationName) {
        healthClassification = healthClassification();
        when(saludRepository.findByDescripcion(classificationName))
                .thenReturn(Optional.of(healthClassification));
    }

    private void givenKnownTypeClassification(String classificationName) {
        typeClassification = typeClassification();
        when(tipoRepository.findByDescripcion(classificationName))
                .thenReturn(Optional.of(typeClassification));
    }

    private void whenParsingBulkStock() {
        response = useCase.execute(defaultRequest());
    }

    private void whenParsingBulkStockFails() {
        Executable execution = () -> useCase.execute(defaultRequest());

        exception = assertThrows(FileValidationException.class, execution);
    }

    private void thenProductWasResolvedWith(
            String expectedProductName,
            UUID expectedCategoryId,
            String expectedNewCategoryName,
            UUID expectedHealthClassificationId,
            UUID expectedTypeClassificationId
    ) {
        BulkProductData product = response.products().get(0);
        assertEquals(1, response.products().size());
        assertEquals(expectedProductName, product.nombre());
        assertEquals(expectedCategoryId, product.categoriaId());
        assertEquals(expectedNewCategoryName, product.nuevaCategoriaNombre());
        assertEquals(expectedIds(expectedHealthClassificationId), product.saludEtiquetasIds());
        assertEquals(expectedIds(expectedTypeClassificationId), product.tipoEtiquetasIds());
    }

    private void thenAnalyzerWasCalledWithAvailableCategories(List<String> expectedCategories) {
        verify(bulkStockAnalyzer).analyze(fileContent, contentType, expectedCategories);
    }

    private void thenExceptionMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void thenAnalyzerWasNotCalled() {
        verifyNoInteractions(bulkStockAnalyzer);
    }

    private ParseBulkStockRequest defaultRequest() {
        return new ParseBulkStockRequest(fileContent, contentType);
    }

    private List<String> availableCategoryNames() {
        return category == null ? List.of() : List.of(category.getDescripcion());
    }

    private Set<UUID> expectedIds(UUID expectedId) {
        return expectedId == null ? Set.of() : Set.of(expectedId);
    }

    private RawBulkProduct rawProduct(String productName, String categoryName) {
        return new RawBulkProduct(
                productName,
                "Bebida 500ml",
                new BigDecimal("1500"),
                new BigDecimal("500"),
                false,
                categoryName,
                100,
                List.of("Sin TACC"),
                List.of("Bebida")
        );
    }

    private CategoriaProducto category(String categoryName) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(randomUUID());
        category.setDescripcion(categoryName);
        return category;
    }

    private ClasificacionSalud healthClassification() {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(randomUUID());
        return classification;
    }

    private ClasificacionTipo typeClassification() {
        ClasificacionTipo classification = new ClasificacionTipo();
        classification.setId(randomUUID());
        return classification;
    }
}
