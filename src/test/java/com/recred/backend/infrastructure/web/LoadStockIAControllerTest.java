package com.recred.backend.infrastructure.web;

import com.recred.backend.application.LoadStockIA;
import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SaveProductCommand;
import com.recred.backend.domain.exceptions.ImageValidationException;
import com.recred.backend.domain.models.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

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

    @InjectMocks
    private LoadStockIAController controller;

    @Test
    void uploadImage_ConImagenValida_RetornaOk() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        LoadStockPredictionDTO expectedResponse = LoadStockPredictionDTO.builder().nombre("Test").build();
        when(loadStockIA.procesarImagen(file)).thenReturn(expectedResponse);

        ResponseEntity<LoadStockPredictionDTO> response = controller.uploadImage(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    void uploadImage_ConArchivoVacio_LanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile("image", "", "image/jpeg", new byte[0]);

        ImageValidationException exception = assertThrows(ImageValidationException.class, () -> controller.uploadImage(file));
        assertEquals("El archivo no puede estar vacío", exception.getMessage());
    }

    @Test
    void uploadImage_ConTipoInvalido_LanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile("image", "test.txt", "text/plain", "content".getBytes());

        ImageValidationException exception = assertThrows(ImageValidationException.class, () -> controller.uploadImage(file));
        assertEquals("Solo se aceptan imágenes JPG, JPEG o PNG", exception.getMessage());
    }

    @Test
    void uploadImage_ConContentTypeNull_LanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", null, "content".getBytes());

        ImageValidationException exception = assertThrows(ImageValidationException.class, () -> controller.uploadImage(file));
        assertEquals("Solo se aceptan imágenes JPG, JPEG o PNG", exception.getMessage());
    }

    @Test
    void uploadImage_ConTipoPng_RetornaOk() {
        MockMultipartFile file = new MockMultipartFile("image", "test.png", "image/png", "content".getBytes());
        LoadStockPredictionDTO expectedResponse = LoadStockPredictionDTO.builder().nombre("Test").build();
        when(loadStockIA.procesarImagen(file)).thenReturn(expectedResponse);

        ResponseEntity<LoadStockPredictionDTO> response = controller.uploadImage(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    void uploadImage_ConTipoJpg_RetornaOk() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpg", "content".getBytes());
        LoadStockPredictionDTO expectedResponse = LoadStockPredictionDTO.builder().nombre("Test").build();
        when(loadStockIA.procesarImagen(file)).thenReturn(expectedResponse);

        ResponseEntity<LoadStockPredictionDTO> response = controller.uploadImage(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    void saveProduct_ConDatosValidos_RetornaCreated() {
        SaveProductDTO dto = new SaveProductDTO("Prod", "Desc", null, null, false, UUID.randomUUID(), null, UUID.randomUUID(), 10, null, null);
        Producto savedProduct = new Producto();
        when(saveProduct.execute(any(SaveProductCommand.class))).thenReturn(savedProduct);

        ResponseEntity<Producto> response = controller.saveProduct(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(savedProduct, response.getBody());
        verify(saveProduct).execute(any(SaveProductCommand.class));
    }
}
