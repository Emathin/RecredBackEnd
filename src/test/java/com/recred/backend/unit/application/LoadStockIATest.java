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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadStockIATest {

    private static final byte[] FILE_BYTES = "content".getBytes();
    private static final String FILE_NAME = "test.jpg";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final String IMAGE_URL = "http://cloudinary.com/image.jpg";
    private static final String PRODUCT_NAME = "Test";
    private static final String PRODUCT_DESC = "Desc";
    private static final String WEIGHT = "100";
    private static final String YES = "si";
    private static final String NO = "no";

    @Mock
    private LoadStockAnalyzer loadStockAnalyzer;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private LoadStockIA loadStockIA;

    @Test
    void execute_ShouldUploadFileAndAnalyzeStock() {
        LoadStockIARequest request = givenRequest();
        LoadStockIAResponse expectedResponse = givenStorageAndAnalyzerReturns(request);

        LoadStockIAResponse result = whenExecute(request);

        thenResponseMatchesAndServicesCalled(result, expectedResponse, request);
    }

    private LoadStockIARequest givenRequest() {
        return new LoadStockIARequest(FILE_BYTES, FILE_NAME, CONTENT_TYPE, FILE_BYTES.length);
    }

    private LoadStockIAResponse givenStorageAndAnalyzerReturns(LoadStockIARequest request) {
        LoadStockIAResponse response = new LoadStockIAResponse(
                PRODUCT_NAME, PRODUCT_DESC, WEIGHT, YES, NO, YES, YES, null
        );
        
        when(storageService.uploadFile(
                request.fileBytes(), request.fileName(), request.contentType()
        )).thenReturn(IMAGE_URL);
        
        when(loadStockAnalyzer.analyze(request)).thenReturn(response);
        
        return response;
    }

    private LoadStockIAResponse whenExecute(LoadStockIARequest request) {
        return loadStockIA.execute(request);
    }

    private void thenResponseMatchesAndServicesCalled(
            LoadStockIAResponse result,
            LoadStockIAResponse expectedResponse,
            LoadStockIARequest request) {
        assertEquals(expectedResponse.name(), result.name());
        assertEquals(IMAGE_URL, result.imageUrl());
        
        verify(storageService).uploadFile(
                request.fileBytes(), request.fileName(), request.contentType()
        );
        verify(loadStockAnalyzer).analyze(request);
    }
}
