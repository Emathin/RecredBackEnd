package com.recred.backend.infrastructure.web;

import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.LoadStockIARequest;
import com.recred.backend.application.LoadStockIAResponse;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.domain.exceptions.ImageValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/load-stock")
public class LoadStockIAController {

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/jpg");

    private final LoadStockIA loadStockIA;
    private final SaveProduct saveProduct;
    private final PlanAccessGuard planAccessGuard;

    public LoadStockIAController(
            LoadStockIA loadStockIA,
            SaveProduct saveProduct,
            PlanAccessGuard planAccessGuard
    ) {
        this.loadStockIA = loadStockIA;
        this.saveProduct = saveProduct;
        this.planAccessGuard = planAccessGuard;
    }

    @PostMapping("/upload-image")
    public ResponseEntity<LoadStockPredictionDTO> uploadImage(
            Authentication authentication,
            @RequestParam("image") MultipartFile image) {
        planAccessGuard.requireVendorIntermediate(authentication);
        validateImage(image);
        
        try {
            LoadStockIARequest request = new LoadStockIARequest(
                    image.getBytes(),
                    image.getOriginalFilename(),
                    image.getContentType(),
                    image.getSize()
            );
            
            LoadStockIAResponse response = loadStockIA.execute(request);
            
            LoadStockPredictionDTO dto = LoadStockPredictionDTO.builder()
                    .nombre(response.name())
                    .descripcion(response.description())
                    .peso(response.weight())
                    .contieneAzucar(response.containsSugar())
                    .contieneMani(response.containsPeanuts())
                    .contieneLactosa(response.containsLactose())
                    .contieneTacc(response.containsGluten())
                    .urlImagen(response.imageUrl())
                    .build();
            
            return ResponseEntity.ok(dto);
        } catch (IOException e) {
            throw new RuntimeException("Error al procesar la imagen", e);
        }
    }

    @PostMapping("/save-product")
    public ResponseEntity<ProductResponseDTO> saveProduct(
            @jakarta.validation.Valid @RequestBody ProductRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductResponseDTO.fromApplication(
                        saveProduct.execute(request.toSaveRequest())));
    }

    private void validateImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new ImageValidationException("El archivo no puede estar vacío");
        }

        if (isInvalidContentType(image.getContentType())) {
            throw new ImageValidationException("Solo se aceptan imágenes JPG, JPEG o PNG");
        }
    }

    private boolean isInvalidContentType(String contentType) {
        return contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType);
    }
}
