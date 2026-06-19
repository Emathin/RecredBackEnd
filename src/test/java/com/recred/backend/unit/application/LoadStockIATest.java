package com.recred.backend.unit.application;

import com.recred.backend.application.LoadStockIA;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import com.recred.backend.infrastructure.gemini.LoadStockIAService;
import com.recred.backend.infrastructure.web.LoadStockPredictionDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadStockIATest {

    @Mock
    private LoadStockIAService loadStockIAService;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private LoadStockIA loadStockIA;

    @Test
    void procesarImagen_DeberiaLlamarAlServicioYRetornarResultado() {
        MultipartFile image = mock(MultipartFile.class);
        LoadStockPredictionDTO expectedResponse = LoadStockPredictionDTO.builder().nombre("Test").build();
        String expectedImageUrl = "http://cloudinary.com/image.jpg";

        when(cloudinaryService.uploadImage(image)).thenReturn(expectedImageUrl);
        when(loadStockIAService.analyze(image)).thenReturn(expectedResponse);

        LoadStockPredictionDTO result = loadStockIA.procesarImagen(image);

        assertEquals(expectedResponse, result);
        assertEquals(expectedImageUrl, result.getUrlImagen());
        verify(cloudinaryService).uploadImage(image);
        verify(loadStockIAService).analyze(image);
    }
}
