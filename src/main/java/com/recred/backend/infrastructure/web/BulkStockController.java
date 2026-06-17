package com.recred.backend.infrastructure.web;

import com.recred.backend.application.BulkStockParseResponse;
import com.recred.backend.application.ParseBulkStock;
import com.recred.backend.application.ParseBulkStockCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Bulk Stock", description = "Endpoints para carga masiva de productos y stock")
public class BulkStockController {

    private final ParseBulkStock parseBulkStock;

    public BulkStockController(ParseBulkStock parseBulkStock) {
        this.parseBulkStock = parseBulkStock;
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Procesa un archivo (PDF, Excel, CSV) para extraer productos masivamente")
    public ResponseEntity<BulkStockParseResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        BulkStockParseResponse response = parseBulkStock.execute(new ParseBulkStockCommand(file));
        return ResponseEntity.ok(response);
    }
}
