package com.recred.backend.unit.application;

import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.LoadStockIARequest;
import com.recred.backend.application.LoadStockIAResponse;
import com.recred.backend.domain.services.LoadStockAnalyzer;
import com.recred.backend.domain.services.StorageService;
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
    private StorageService storageService;

    @InjectMocks
    private LoadStockIA loadStockIA;

    @Test
    void execute_DeberiaLlamarAlServicioYRetornarResultado() {
        byte[] fileBytes = "content".getBytes();
        LoadStockIARequest request = new LoadStockIARequest(fileBytes, "test.jpg", "image/jpeg", fileBytes.length);
        LoadStockIAResponse expectedResponse = new LoadStockIAResponse("Test", "Desc", "100", "si", "no", "si", "si", null);
        String expectedImageUrl = "http://cloudinary.com/image.jpg";

        when(storageService.uploadFile(request.fileBytes(), request.fileName(), request.contentType())).thenReturn(expectedImageUrl);
        when(loadStockAnalyzer.analyze(request)).thenReturn(expectedResponse);

        LoadStockIAResponse result = loadStockIA.execute(request);

        assertEquals(expectedResponse.name(), result.name());
        assertEquals(expectedImageUrl, result.imageUrl());
        verify(storageService).uploadFile(request.fileBytes(), request.fileName(), request.contentType());
        verify(loadStockAnalyzer).analyze(request);
    }
}
