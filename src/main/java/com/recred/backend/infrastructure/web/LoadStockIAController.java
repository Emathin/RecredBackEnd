package com.recred.backend.infrastructure.web;

import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.domain.exceptions.ImageValidationException;
import com.recred.backend.domain.models.Producto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/load-stock")
public class LoadStockIAController {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/jpg");

    private final LoadStockIA loadStockIA;
    private final SaveProduct saveProduct;

    public LoadStockIAController(LoadStockIA loadStockIA, SaveProduct saveProduct) {
        this.loadStockIA = loadStockIA;
        this.saveProduct = saveProduct;
    }

    @PostMapping("/upload-image")
    public ResponseEntity<LoadStockPredictionDTO> uploadImage(@RequestParam("image") MultipartFile image) {
        validateImage(image);
        return ResponseEntity.ok(loadStockIA.procesarImagen(image));
    }

    @PostMapping("/save-product")
    public ResponseEntity<Producto> saveProduct(@RequestBody SaveProductDTO saveProductDTO) {
        Producto createdProduct = saveProduct.execute(saveProductDTO.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    private void validateImage(MultipartFile image) {
        if (image.isEmpty())
            throw new ImageValidationException("El archivo no puede estar vacío");

        if (isInvalidContentType(image.getContentType()))
            throw new ImageValidationException("Solo se aceptan imágenes JPG, JPEG o PNG");
    }

    private boolean isInvalidContentType(String contentType) {
        return contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType);
    }
}
