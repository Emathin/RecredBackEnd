package com.recred.backend.application;

import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.services.StorageService;

public class UploadProductImage {

    private final StorageService storageService;
    private final InventoryRepository inventoryRepository;

    public UploadProductImage(
            StorageService storageService,
            InventoryRepository inventoryRepository) {
        this.storageService = storageService;
        this.inventoryRepository = inventoryRepository;
    }

    public UploadProductImageResponse execute(UploadProductImageRequest request) {
        String urlImagen = uploadImageToStorage(request);
        updateInventory(request, urlImagen);
        return new UploadProductImageResponse(urlImagen);
    }

    private String uploadImageToStorage(UploadProductImageRequest request) {
        return storageService.uploadFile(
                request.imageBytes(), request.originalFilename(), null);
    }

    private void updateInventory(UploadProductImageRequest request, String urlImagen) {
        inventoryRepository.updateUrlImagen(
                request.buffetId(), request.productoId(), urlImagen);
    }
}
