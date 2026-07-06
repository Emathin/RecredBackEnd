package com.recred.backend.infrastructure.web;

import com.recred.backend.application.BulkStockParseResponse;
import com.recred.backend.application.DeleteProduct;
import com.recred.backend.application.DeleteProductRequest;
import com.recred.backend.application.GetAllProducts;
import com.recred.backend.application.GetAllProductsRequest;
import com.recred.backend.application.GetProductById;
import com.recred.backend.application.GetProductByIdRequest;
import com.recred.backend.application.ParseBulkStock;
import com.recred.backend.application.ParseBulkStockRequest;
import com.recred.backend.application.SaveBulkProducts;
import com.recred.backend.application.SaveBulkProductsRequest;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SaveProductRequest;
import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductPrice;
import com.recred.backend.application.UpdateProductPriceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Endpoints para gestión de productos y stock")
public class ProductController {
    private final GetAllProducts getAllProducts;
    private final GetProductById getProductById;
    private final SaveProduct saveProduct;
    private final UpdateProduct updateProduct;
    private final DeleteProduct deleteProduct;
    private final UpdateProductPrice updateProductPrice;
    private final ParseBulkStock parseBulkStock;
    private final SaveBulkProducts saveBulkProducts;

    public ProductController(
            GetAllProducts getAllProducts,
            GetProductById getProductById,
            SaveProduct saveProduct,
            UpdateProduct updateProduct,
            DeleteProduct deleteProduct,
            UpdateProductPrice updateProductPrice,
            ParseBulkStock parseBulkStock,
            SaveBulkProducts saveBulkProducts) {
        this.getAllProducts = getAllProducts;
        this.getProductById = getProductById;
        this.saveProduct = saveProduct;
        this.updateProduct = updateProduct;
        this.deleteProduct = deleteProduct;
        this.updateProductPrice = updateProductPrice;
        this.parseBulkStock = parseBulkStock;
        this.saveBulkProducts = saveBulkProducts;
    }

    @PatchMapping("/{productId}/precio")
    public ResponseEntity<Void> updatePrice(
            @PathVariable UUID productId,
            @RequestParam(required = false) BigDecimal nuevoPrecio,
            @RequestParam(required = false) UUID buffetId,
            @RequestBody(required = false) UpdateProductPriceRequestDTO request) {
        updateProductPrice.execute(new UpdateProductPriceRequest(
                productId,
                resolveNewPrice(nuevoPrecio, request),
                resolveBuffetId(buffetId, request)));
        return ResponseEntity.ok().build();
    }

    private BigDecimal resolveNewPrice(
            BigDecimal queryPrice,
            UpdateProductPriceRequestDTO request) {
        if (queryPrice != null) {
            return queryPrice;
        }
        if (request != null) {
            return request.nuevoPrecio();
        }
        throw new IllegalArgumentException("Product price is required");
    }

    private UUID resolveBuffetId(UUID queryBuffetId, UpdateProductPriceRequestDTO request) {
        if (queryBuffetId != null) {
            return queryBuffetId;
        }
        if (request != null) {
            return request.buffetId();
        }
        return null;
    }

    public record UpdateProductPriceRequestDTO(BigDecimal nuevoPrecio, UUID buffetId) {
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> findAll(
            @RequestParam(required = false) UUID buffetId) {
        List<ProductResponseDTO> products = getAllProducts
                .execute(new GetAllProductsRequest(buffetId))
                .products()
                .stream()
                .map(ProductResponseDTO::fromApplication)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Procesa un archivo (PDF, CSV) para extraer productos masivamente")
    public ResponseEntity<BulkStockParseResponse> uploadFile(
            @RequestParam("file") MultipartFile file) {
        try {
            ParseBulkStockRequest parseRequest = new ParseBulkStockRequest(
                    file.getBytes(),
                    file.getContentType()
            );
            BulkStockParseResponse response = parseBulkStock.execute(parseRequest);
            return ResponseEntity.ok(response);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error reading uploaded file", e);
        }
    }

    @PostMapping("/bulk")
    @Operation(summary = "Guarda múltiples productos en una sola transacción")
    public ResponseEntity<List<ProductResponseDTO>> createBulk(
            @Valid @RequestBody List<ProductRequestDTO> request) {
        List<SaveProductRequest> products = request.stream()
                .map(ProductRequestDTO::toSaveRequest)
                .collect(Collectors.toList());
        List<ProductResponseDTO> response = saveBulkProducts
                .execute(new SaveBulkProductsRequest(products))
                .products()
                .stream()
                .map(ProductResponseDTO::fromApplication)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> findById(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID buffetId) {
        return ResponseEntity.ok(ProductResponseDTO.fromApplication(
                getProductById.execute(new GetProductByIdRequest(id, buffetId))));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(
            @Valid @RequestBody ProductRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductResponseDTO.fromApplication(saveProduct.execute(
                        request.toSaveRequest())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDTO request) {
        return ResponseEntity.ok(ProductResponseDTO.fromApplication(
                updateProduct.execute(request.toUpdateRequest(id))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteProduct.execute(new DeleteProductRequest(id));
        return ResponseEntity.noContent().build();
    }
}
