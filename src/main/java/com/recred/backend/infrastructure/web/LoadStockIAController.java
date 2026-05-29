package com.recred.backend.infrastructure.web;

import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.domain.models.Producto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/load-stock")
public class LoadStockIAController {

    private final LoadStockIA loadStockIA;
    private final SaveProduct saveProduct;


    public LoadStockIAController(LoadStockIA loadStockIA, SaveProduct saveProduct) {
        this.saveProduct = saveProduct;
        this.loadStockIA = loadStockIA;
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image) {

        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo no puede estar vacío");
        }

        String contentType = image.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") &&
                !contentType.equals("image/png") && !contentType.equals("image/jpg"))) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Solo se aceptan imágenes JPG, JPEG o PNG");
        }

        try {
            return ResponseEntity.ok(loadStockIA.procesarImagen(image));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar la imagen: " + e.getMessage());
        }
    }

    @PostMapping("/save-product")
    public ResponseEntity<Producto> saveProduct(@RequestBody SaveProductDTO saveProductDTO) {
        Producto createProduct = saveProduct.save(saveProductDTO.toModel());
        return ResponseEntity.status(HttpStatus.CREATED).body(createProduct);
    }


}