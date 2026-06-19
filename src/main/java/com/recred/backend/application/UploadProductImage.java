package com.recred.backend.application;

import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public class UploadProductImage {

    private final CloudinaryService cloudinaryService;
    private final InventoryRepository inventoryRepository;

    public UploadProductImage(
            CloudinaryService cloudinaryService,
            InventoryRepository inventoryRepository) {
        this.cloudinaryService = cloudinaryService;
        this.inventoryRepository = inventoryRepository;
    }

    public String execute(UUID buffetId, UUID productoId, MultipartFile imagen) {
        String urlImagen = cloudinaryService.uploadImage(imagen);
        inventoryRepository.updateUrlImagen(buffetId, productoId, urlImagen);
        return urlImagen;
    }
}
