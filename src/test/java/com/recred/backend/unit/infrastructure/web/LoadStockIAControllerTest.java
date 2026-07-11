package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.LoadStockIARequest;
import com.recred.backend.application.LoadStockIAResponse;
import com.recred.backend.application.ProductResponse;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SaveProductRequest;
import com.recred.backend.domain.exceptions.ImageValidationException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.web.LoadStockIAController;
import com.recred.backend.infrastructure.web.LoadStockPredictionDTO;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import com.recred.backend.infrastructure.web.ProductRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadStockIAControllerTest {

    @Mock
    private LoadStockIA loadStockIA;

    @Mock
    private SaveProduct saveProduct;

    @Mock
    private PlanAccessGuard planAccessGuard;

    @InjectMocks
    private LoadStockIAController controller;

    private final String VALID_IMAGE_CONTENT = "content";

    @Test
    void uploadImageWithValidImageShouldReturnOk() {
        Authentication auth = givenAuthentication();
        MockMultipartFile file = givenMultipartFile("test.jpg", "image/jpeg", VALID_IMAGE_CONTENT.getBytes());
        givenLoadStockIAResponse("Test");

        ResponseEntity<LoadStockPredictionDTO> response = whenUploadingImage(auth, file);

        thenPredictionIsReturned(response, "Test");
        thenAccessGuardIsVerified(auth);
    }

    @Test
    void uploadImageWithEmptyFileShouldThrowException() {
        Authentication auth = givenAuthentication();
        MockMultipartFile file = givenMultipartFile("", "image/jpeg", new byte[0]);

        Executable execution = () -> whenUploadingImage(auth, file);
                
        thenThrowsImageValidationExceptionWithMessage(execution, "El archivo no puede estar vacío");
    }

    @Test
    void uploadImageWithInvalidTypeShouldThrowException() {
        Authentication auth = givenAuthentication();
        MockMultipartFile file = givenMultipartFile("test.txt", "text/plain", VALID_IMAGE_CONTENT.getBytes());

        Executable execution = () -> whenUploadingImage(auth, file);
                
        thenThrowsImageValidationExceptionWithMessage(execution, "Solo se aceptan imágenes JPG, JPEG o PNG");
    }

    @Test
    void uploadImageWithNullContentTypeShouldThrowException() {
        Authentication auth = givenAuthentication();
        MockMultipartFile file = givenMultipartFile("test.jpg", null, VALID_IMAGE_CONTENT.getBytes());

        Executable execution = () -> whenUploadingImage(auth, file);
                
        thenThrowsImageValidationExceptionWithMessage(execution, "Solo se aceptan imágenes JPG, JPEG o PNG");
    }

    @Test
    void uploadImageWithPngTypeShouldReturnOk() {
        Authentication auth = givenAuthentication();
        MockMultipartFile file = givenMultipartFile("test.png", "image/png", VALID_IMAGE_CONTENT.getBytes());
        givenLoadStockIAResponse("TestPng");

        ResponseEntity<LoadStockPredictionDTO> response = whenUploadingImage(auth, file);

        thenPredictionIsReturned(response, "TestPng");
    }

    @Test
    void uploadImageWithJpgTypeShouldReturnOk() {
        Authentication auth = givenAuthentication();
        MockMultipartFile file = givenMultipartFile("test.jpg", "image/jpg", VALID_IMAGE_CONTENT.getBytes());
        givenLoadStockIAResponse("TestJpg");

        ResponseEntity<LoadStockPredictionDTO> response = whenUploadingImage(auth, file);

        thenPredictionIsReturned(response, "TestJpg");
    }

    @Test
    void saveProductWithValidDataShouldReturnCreated() {
        ProductRequestDTO dto = givenProductRequest();
        Producto savedProduct = givenSavedProduct();
        
        ResponseEntity<ProductResponseDTO> response = whenSavingProduct(dto);

        thenProductIsSavedAndReturned(response, savedProduct.getId());
    }

    private Authentication givenAuthentication() {
        return mock(Authentication.class);
    }

    private MockMultipartFile givenMultipartFile(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("image", filename, contentType, content);
    }

    private void givenLoadStockIAResponse(String name) {
        LoadStockIAResponse responseIa = new LoadStockIAResponse(name, "Desc", "0", "false", "false", "false", "false", "url");
        when(loadStockIA.execute(any(LoadStockIARequest.class))).thenReturn(responseIa);
    }

    private ProductRequestDTO givenProductRequest() {
        return new ProductRequestDTO("Prod", "Desc", BigDecimal.TEN, BigDecimal.ONE, false, UUID.randomUUID(), "Cat",
                UUID.randomUUID(), 10, null, null, Set.of(), Set.of(), "url");
    }

    private Producto givenSavedProduct() {
        Producto savedProduct = new Producto();
        savedProduct.setId(UUID.randomUUID());
        savedProduct.setNombre("Prod");
        when(saveProduct.execute(any(SaveProductRequest.class)))
                .thenReturn(ProductResponse.fromDomain(savedProduct));
        return savedProduct;
    }

    private ResponseEntity<LoadStockPredictionDTO> whenUploadingImage(Authentication auth, MockMultipartFile file) {
        return controller.uploadImage(auth, file);
    }

    private ResponseEntity<ProductResponseDTO> whenSavingProduct(ProductRequestDTO dto) {
        return controller.saveProduct(dto);
    }

    private void thenPredictionIsReturned(ResponseEntity<LoadStockPredictionDTO> response, String expectedName) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedName, response.getBody().getNombre());
    }

    private void thenAccessGuardIsVerified(Authentication auth) {
        verify(planAccessGuard).requireVendorIntermediate(auth);
    }

    private void thenThrowsImageValidationExceptionWithMessage(Executable execution, String expectedMessage) {
        ImageValidationException exception = assertThrows(ImageValidationException.class, execution);
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void thenProductIsSavedAndReturned(ResponseEntity<ProductResponseDTO> response, UUID expectedId) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedId, response.getBody().id());
        verify(saveProduct).execute(any(SaveProductRequest.class));
    }
}
