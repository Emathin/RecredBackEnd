package com.recred.backend.unit.application;

import com.recred.backend.application.BulkStockParseResponse;
import com.recred.backend.application.ParseBulkStock;
import com.recred.backend.application.ParseBulkStockRequest;
import com.recred.backend.application.RawBulkProduct;
import com.recred.backend.domain.exceptions.FileValidationException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import com.recred.backend.domain.repositories.ClasificacionTipoRepository;
import com.recred.backend.domain.services.BulkStockAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParseBulkStockTest {

    @Mock
    private BulkStockAnalyzer bulkStockAnalyzer;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ClasificacionSaludRepository saludRepository;

    @Mock
    private ClasificacionTipoRepository tipoRepository;

    @InjectMocks
    private ParseBulkStock parseBulkStock;

    @Test
    void execute_RetornaProductosResueltos_CuandoArchivoEsValido() {
        byte[] content = "dummy content".getBytes();
        String contentType = "application/pdf";
        ParseBulkStockRequest command = new ParseBulkStockRequest(content, contentType);

        RawBulkProduct rawProduct = new RawBulkProduct(
                "Coca Cola",
                "Bebida 500ml",
                new BigDecimal("1500"),
                new BigDecimal("500"),
                false,
                "Bebidas",
                100,
                List.of("Sin TACC"),
                List.of("Bebida")
        );

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(UUID.randomUUID());
        categoria.setDescripcion("Bebidas");
        
        when(categoryRepository.getAllCategories()).thenReturn(List.of(categoria));
        when(bulkStockAnalyzer.analyze(eq(content), eq(contentType), anyList()))
                .thenReturn(List.of(rawProduct));

        when(categoryRepository.findByDescription("Bebidas")).thenReturn(Optional.of(categoria));

        ClasificacionSalud salud = new ClasificacionSalud();
        salud.setId(UUID.randomUUID());
        when(saludRepository.findByDescripcion("Sin TACC")).thenReturn(Optional.of(salud));

        ClasificacionTipo tipo = new ClasificacionTipo();
        tipo.setId(UUID.randomUUID());
        when(tipoRepository.findByDescripcion("Bebida")).thenReturn(Optional.of(tipo));

        BulkStockParseResponse response = parseBulkStock.execute(command);

        assertNotNull(response);
        assertEquals(1, response.products().size());
        assertEquals("Coca Cola", response.products().get(0).nombre());
        assertEquals(categoria.getId(), response.products().get(0).categoriaId());
        assertNull(response.products().get(0).nuevaCategoriaNombre());
        assertEquals(1, response.products().get(0).saludEtiquetasIds().size());
        assertEquals(1, response.products().get(0).tipoEtiquetasIds().size());
    }

    @Test
    void execute_LanzaExcepcion_CuandoArchivoEstaVacio() {
        ParseBulkStockRequest command = new ParseBulkStockRequest(new byte[0], "application/pdf");

        assertThrows(FileValidationException.class, () -> parseBulkStock.execute(command));
    }

    @Test
    void execute_LanzaExcepcion_CuandoFormatoNoEsPermitido() {
        ParseBulkStockRequest command = new ParseBulkStockRequest(
                "content".getBytes(),
                "application/invalid");

        assertThrows(FileValidationException.class, () -> parseBulkStock.execute(command));
    }

    @Test
    void execute_RetornaNuevaCategoria_CuandoNoExisteEnRepo() {
        byte[] content = "dummy".getBytes();
        String contentType = "text/csv";
        ParseBulkStockRequest command = new ParseBulkStockRequest(content, contentType);

        RawBulkProduct rawProduct = new RawBulkProduct(
                "Alfajor", null, null, null, false, "Golosinas", null, List.of(), List.of()
        );

        when(categoryRepository.getAllCategories()).thenReturn(List.of());
        when(bulkStockAnalyzer.analyze(eq(content), eq(contentType), anyList()))
                .thenReturn(List.of(rawProduct));
        when(categoryRepository.findByDescription("Golosinas")).thenReturn(Optional.empty());

        BulkStockParseResponse response = parseBulkStock.execute(command);

        assertNull(response.products().get(0).categoriaId());
        assertEquals("Golosinas", response.products().get(0).nuevaCategoriaNombre());
    }
}
