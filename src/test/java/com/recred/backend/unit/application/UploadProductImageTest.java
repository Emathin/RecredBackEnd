package com.recred.backend.unit.application;

import com.recred.backend.application.UploadProductImage;
import com.recred.backend.application.UploadProductImageRequest;
import com.recred.backend.application.UploadProductImageResponse;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadProductImageTest {

    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final byte[] FILE_BYTES = "content".getBytes();
    private static final String FILE_NAME = "image.jpg";
    private static final String EXPECTED_URL = "http://cloudinary.com/image.jpg";

    @Mock
    private StorageService storageService;

    @Mock
    private InventoryRepository inventoryRepository;

    private UploadProductImage uploadProductImage;

    @BeforeEach
    void setUp() {
        uploadProductImage = new UploadProductImage(storageService, inventoryRepository);
    }

    @Test
    void execute_UploadsImageAndUpdatesRepository() {
        givenImageUploadWillSucceed();

        UploadProductImageResponse response = whenExecutingUseCase();

        thenImageWasUploadedAndInventoryUpdated(response);
    }

    @Test
    void execute_WhenUploadFails_ThrowsException() {
        givenImageUploadWillFail();

        Executable execution = this::whenExecutingUseCase;

        thenThrowsException(execution);
    }

    private void givenImageUploadWillSucceed() {
        when(storageService.uploadFile(FILE_BYTES, FILE_NAME, null)).thenReturn(EXPECTED_URL);
    }

    private void givenImageUploadWillFail() {
        when(storageService.uploadFile(FILE_BYTES, FILE_NAME, null))
                .thenThrow(new RuntimeException("Storage error"));
    }

    private UploadProductImageResponse whenExecutingUseCase() {
        UploadProductImageRequest request = new UploadProductImageRequest(
                BUFFET_ID, PRODUCT_ID, FILE_BYTES, FILE_NAME);
        return uploadProductImage.execute(request);
    }

    private void thenImageWasUploadedAndInventoryUpdated(UploadProductImageResponse response) {
        assertEquals(EXPECTED_URL, response.urlImagen());
        verify(storageService).uploadFile(FILE_BYTES, FILE_NAME, null);
        verify(inventoryRepository).updateUrlImagen(BUFFET_ID, PRODUCT_ID, EXPECTED_URL);
    }

    private void thenThrowsException(Executable execution) {
        assertThrows(RuntimeException.class, execution);
        verify(inventoryRepository, never()).updateUrlImagen(any(), any(), anyString());
    }
}
