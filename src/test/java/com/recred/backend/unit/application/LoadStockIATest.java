package com.recred.backend.unit.application;

import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.LoadStockIARequest;
import com.recred.backend.application.LoadStockIAResponse;
import com.recred.backend.domain.services.LoadStockAnalyzer;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadStockIATest {

    @Mock
    private LoadStockAnalyzer loadStockAnalyzer;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private LoadStockIA loadStockIA;

    @Test
    void execute_DeberiaLlamarAlServicioYRetornarResultado() {
        LoadStockIARequest request = new LoadStockIARequest("content".getBytes(), "test.jpg", "image/jpeg", "content".length());
        LoadStockIAResponse expectedResponse = new LoadStockIAResponse("Test", "Desc", "100", "si", "no", "si", "si", null);
        String expectedImageUrl = "http://cloudinary.com/image.jpg";

        when(cloudinaryService.uploadImage(request.fileBytes(), request.fileName())).thenReturn(expectedImageUrl);
        when(loadStockAnalyzer.analyze(request)).thenReturn(expectedResponse);

        LoadStockIAResponse result = loadStockIA.execute(request);

        assertEquals(expectedResponse.name(), result.name());
        assertEquals(expectedImageUrl, result.imageUrl());
        verify(cloudinaryService).uploadImage(request.fileBytes(), request.fileName());
        verify(loadStockAnalyzer).analyze(request);
    }
}
