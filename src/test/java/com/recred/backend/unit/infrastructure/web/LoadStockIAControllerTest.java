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
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import com.recred.backend.infrastructure.web.ProductRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        LoadStockIAResponse responseIa = new LoadStockIAResponse("Test", "Desc", "0", "false", "false", "false", "false", "url");
        when(loadStockIA.execute(any(LoadStockIARequest.class))).thenReturn(responseIa);

        ResponseEntity<LoadStockPredictionDTO> response = controller.uploadImage(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test", response.getBody().getNombre());
    }

    @Test
    void uploadImage_ConArchivoVacio_LanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile("image", "", "image/jpeg", new byte[0]);

        ImageValidationException exception = assertThrows(ImageValidationException.class,
                () -> controller.uploadImage(file));
        assertEquals("El archivo no puede estar vacío", exception.getMessage());
    }

    @Test
    void uploadImage_ConTipoInvalido_LanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile("image", "test.txt", "text/plain", "content".getBytes());

        ImageValidationException exception = assertThrows(ImageValidationException.class,
                () -> controller.uploadImage(file));
        assertEquals("Solo se aceptan imágenes JPG, JPEG o PNG", exception.getMessage());
    }

    @Test
    void uploadImage_ConContentTypeNull_LanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", null, "content".getBytes());

        ImageValidationException exception = assertThrows(ImageValidationException.class,
                () -> controller.uploadImage(file));
        assertEquals("Solo se aceptan imágenes JPG, JPEG o PNG", exception.getMessage());
    }

    @Test
    void uploadImage_ConTipoPng_RetornaOk() {
        MockMultipartFile file = new MockMultipartFile("image", "test.png", "image/png", "content".getBytes());
        LoadStockIAResponse responseIa = new LoadStockIAResponse("Test", "Desc", "0", "false", "false", "false", "false", "url");
        when(loadStockIA.execute(any(LoadStockIARequest.class))).thenReturn(responseIa);

        ResponseEntity<LoadStockPredictionDTO> response = controller.uploadImage(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test", response.getBody().getNombre());
    }

    @Test
    void uploadImage_ConTipoJpg_RetornaOk() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpg", "content".getBytes());
        LoadStockIAResponse responseIa = new LoadStockIAResponse("Test", "Desc", "0", "false", "false", "false", "false", "url");
        when(loadStockIA.execute(any(LoadStockIARequest.class))).thenReturn(responseIa);

        ResponseEntity<LoadStockPredictionDTO> response = controller.uploadImage(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test", response.getBody().getNombre());
    }

    @Test
    void saveProduct_ConDatosValidos_RetornaCreated() {
        ProductRequestDTO dto = new ProductRequestDTO("Prod", "Desc", BigDecimal.TEN, BigDecimal.ONE, false, UUID.randomUUID(), "Cat",
                UUID.randomUUID(), 10, null, null, Set.of(), Set.of(), "url");
        Producto savedProduct = new Producto();
        savedProduct.setId(UUID.randomUUID());
        savedProduct.setNombre("Prod");
        
        when(saveProduct.execute(any(SaveProductRequest.class)))
                .thenReturn(ProductResponse.fromDomain(savedProduct));

        ResponseEntity<ProductResponseDTO> response = controller.saveProduct(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(savedProduct.getId(), response.getBody().id());
        verify(saveProduct).execute(any(SaveProductRequest.class));
    }
}
