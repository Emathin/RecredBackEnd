package com.recred.backend.infrastructure.web;

import com.recred.backend.application.BulkStockParseResponse;
import com.recred.backend.application.ParseBulkStock;
import com.recred.backend.application.ParseBulkStockCommand;
import com.recred.backend.application.SaveBulkProducts;
import com.recred.backend.application.SaveProductCommand;
import com.recred.backend.domain.models.Producto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Bulk Stock", description = "Endpoints para carga masiva de productos y stock")
public class BulkStockController {

    private final ParseBulkStock parseBulkStock;
    private final SaveBulkProducts saveBulkProducts;

    public BulkStockController(ParseBulkStock parseBulkStock, SaveBulkProducts saveBulkProducts) {
        this.parseBulkStock = parseBulkStock;
        this.saveBulkProducts = saveBulkProducts;
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Procesa un archivo (PDF, CSV) para extraer productos masivamente")
    public ResponseEntity<BulkStockParseResponse> uploadFile(
            @RequestParam("file") MultipartFile file) {
        try {
            ParseBulkStockCommand command = new ParseBulkStockCommand(
                    file.getBytes(),
                    file.getContentType()
            );
            BulkStockParseResponse response = parseBulkStock.execute(command);
            return ResponseEntity.ok(response);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error reading uploaded file", e);
        }
    }

    @PostMapping("/bulk")
    @Operation(summary = "Guarda múltiples productos en una sola transacción")
    public ResponseEntity<List<ProductResponseDTO>> createBulk(
            @Valid @RequestBody List<ProductRequestDTO> request) {
        List<SaveProductCommand> commands = request.stream()
                .map(ProductRequestDTO::toSaveCommand)
                .collect(Collectors.toList());
        List<Producto> savedProducts = saveBulkProducts.execute(commands);
        List<ProductResponseDTO> response = savedProducts.stream()
                .map(ProductResponseDTO::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
