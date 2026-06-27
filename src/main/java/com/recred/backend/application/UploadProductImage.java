package com.recred.backend.application;

import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;

public class UploadProductImage {

    private final CloudinaryService cloudinaryService;
    private final InventoryRepository inventoryRepository;

    public UploadProductImage(
            CloudinaryService cloudinaryService,
            InventoryRepository inventoryRepository) {
        this.cloudinaryService = cloudinaryService;
        this.inventoryRepository = inventoryRepository;
    }

    public UploadProductImageResponse execute(UploadProductImageRequest request) {
        String urlImagen = cloudinaryService.uploadImage(request.imagen());
        inventoryRepository.updateUrlImagen(
                request.buffetId(), request.productoId(), urlImagen);
        return new UploadProductImageResponse(urlImagen);
    }
}
