package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DeleteProduct;
import com.recred.backend.application.GetAllProducts;
import com.recred.backend.application.GetProductById;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.UpdateProduct;
import com.recred.backend.domain.models.Producto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final GetAllProducts getAllProducts;
    private final GetProductById getProductById;
    private final SaveProduct saveProduct;
    private final UpdateProduct updateProduct;
    private final DeleteProduct deleteProduct;

    public ProductController(GetAllProducts getAllProducts,
                             GetProductById getProductById,
                             SaveProduct saveProduct,
                             UpdateProduct updateProduct,
                             DeleteProduct deleteProduct) {
        this.getAllProducts = getAllProducts;
        this.getProductById = getProductById;
        this.saveProduct = saveProduct;
        this.updateProduct = updateProduct;
        this.deleteProduct = deleteProduct;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> findAll(
            @RequestParam(required = false) UUID buffetId) {
        List<ProductResponseDTO> products = getAllProducts.execute(buffetId)
                .stream()
                .map(ProductResponseDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> findById(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID buffetId) {
        Producto product = getProductById.execute(id, buffetId);
        return ResponseEntity.ok(ProductResponseDTO.fromDomain(product));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(
            @Valid @RequestBody ProductRequestDTO request) {
        Producto created = saveProduct.execute(request.toSaveCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductResponseDTO.fromDomain(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDTO request) {
        Producto updated = updateProduct.execute(request.toUpdateCommand(id));
        return ResponseEntity.ok(ProductResponseDTO.fromDomain(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteProduct.execute(id);
        return ResponseEntity.noContent().build();
    }
}
