package com.recred.backend.unit.application;

import com.recred.backend.application.UploadProductImage;
import com.recred.backend.application.UploadProductImageRequest;
import com.recred.backend.application.UploadProductImageResponse;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadProductImageTest {

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private UploadProductImage uploadProductImage;

    @Test
    void execute_SubeImagenYActualizaRepositorio() {
        UUID buffetId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        byte[] imagen = "content".getBytes();
        String fileName = "image.jpg";
        String expectedUrl = "http://cloudinary.com/image.jpg";
        UploadProductImageRequest request = new UploadProductImageRequest(buffetId, productoId, imagen, fileName);

        when(cloudinaryService.uploadImage(imagen, fileName)).thenReturn(expectedUrl);

        UploadProductImageResponse response = uploadProductImage.execute(request);

        assertEquals(expectedUrl, response.urlImagen());
        verify(cloudinaryService).uploadImage(imagen, fileName);
        verify(inventoryRepository).updateUrlImagen(buffetId, productoId, expectedUrl);
    }

    @Test
    void execute_CuandoFallaSubeImagen_LanzaExcepcion() {
        UUID buffetId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        byte[] imagen = "content".getBytes();
        String fileName = "image.jpg";
        UploadProductImageRequest request = new UploadProductImageRequest(buffetId, productoId, imagen, fileName);

        when(cloudinaryService.uploadImage(imagen, fileName)).thenThrow(new RuntimeException("Cloudinary error"));

        Executable execution = () -> uploadProductImage.execute(request);

        assertThrows(RuntimeException.class, execution);
        verify(inventoryRepository, never()).updateUrlImagen(any(), any(), any());
    }
}
