package com.recred.backend.unit.application;

import com.recred.backend.application.UploadProductImage;
import com.recred.backend.application.UploadProductImageRequest;
import com.recred.backend.application.UploadProductImageResponse;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

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
        MultipartFile imagen = mock(MultipartFile.class);
        String expectedUrl = "http://cloudinary.com/image.jpg";

        when(cloudinaryService.uploadImage(imagen)).thenReturn(expectedUrl);

        UploadProductImageRequest request = new UploadProductImageRequest(buffetId, productoId, imagen);
        UploadProductImageResponse response = uploadProductImage.execute(request);

        assertEquals(expectedUrl, response.urlImagen());
        verify(cloudinaryService).uploadImage(imagen);
        verify(inventoryRepository).updateUrlImagen(buffetId, productoId, expectedUrl);
    }

    @Test
    void execute_CuandoFallaSubeImagen_LanzaExcepcion() {
        UUID buffetId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        MultipartFile imagen = mock(MultipartFile.class);

        when(cloudinaryService.uploadImage(imagen)).thenThrow(new RuntimeException("Cloudinary error"));

        UploadProductImageRequest request = new UploadProductImageRequest(buffetId, productoId, imagen);

        assertThrows(RuntimeException.class, () -> uploadProductImage.execute(request));
        verify(inventoryRepository, never()).updateUrlImagen(any(), any(), any());
    }
}
