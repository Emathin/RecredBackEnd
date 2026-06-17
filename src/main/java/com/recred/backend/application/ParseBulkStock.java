package com.recred.backend.application;

import com.recred.backend.domain.exceptions.FileValidationException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import com.recred.backend.domain.repositories.ClasificacionTipoRepository;
import com.recred.backend.domain.services.BulkStockAnalyzer;
import com.recred.backend.infrastructure.web.SaveProductDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ParseBulkStock {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "text/csv"
    );

    private final BulkStockAnalyzer bulkStockAnalyzer;
    private final CategoryRepository categoryRepository;
    private final ClasificacionSaludRepository saludRepository;
    private final ClasificacionTipoRepository tipoRepository;

    public ParseBulkStock(BulkStockAnalyzer bulkStockAnalyzer,
                          CategoryRepository categoryRepository,
                          ClasificacionSaludRepository saludRepository,
                          ClasificacionTipoRepository tipoRepository) {
        this.bulkStockAnalyzer = bulkStockAnalyzer;
        this.categoryRepository = categoryRepository;
        this.saludRepository = saludRepository;
        this.tipoRepository = tipoRepository;
    }

    public BulkStockParseResponse execute(ParseBulkStockCommand command) {
        validateFile(command.file());
        
        List<RawBulkProduct> rawProducts = bulkStockAnalyzer.analyze(command.file());
        
        List<SaveProductDTO> resolvedProducts = rawProducts.stream()
                .map(this::resolveEntity)
                .collect(Collectors.toList());
        
        return new BulkStockParseResponse(resolvedProducts);
    }

    private SaveProductDTO resolveEntity(RawBulkProduct raw) {
        UUID categoriaId = null;
        String nuevaCategoriaNombre = raw.categoriaNombre();

        if (raw.categoriaNombre() != null && !raw.categoriaNombre().isBlank()) {
            Optional<CategoriaProducto> category = categoryRepository.findByDescription(raw.categoriaNombre());
            if (category.isPresent()) {
                categoriaId = category.get().getId();
                nuevaCategoriaNombre = null;
            }
        }

        Set<UUID> saludIds = raw.saludEtiquetas().stream()
                .map(saludRepository::findByDescripcion)
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getId())
                .collect(Collectors.toSet());

        Set<UUID> tipoIds = raw.tipoEtiquetas().stream()
                .map(tipoRepository::findByDescripcion)
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getId())
                .collect(Collectors.toSet());

        return new SaveProductDTO(
                raw.nombre(),
                raw.descripcion(),
                raw.precio(),
                raw.peso(),
                raw.requierePreparacion(),
                categoriaId,
                nuevaCategoriaNombre,
                null, // buffetId se asignará en el front o por contexto
                raw.stockActual(),
                saludIds,
                tipoIds
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("El archivo no puede estar vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileValidationException("Formato de archivo no permitido. Se aceptan PDF, Excel (.xlsx) y CSV.");
        }
    }
}
