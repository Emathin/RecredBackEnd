package com.recred.backend.unit.application;

import com.recred.backend.application.RawSupplierPriceItem;
import com.recred.backend.application.UploadSupplierPriceList;
import com.recred.backend.domain.exceptions.FileValidationException;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import com.recred.backend.domain.services.StorageService;
import com.recred.backend.domain.services.SupplierPriceListAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadSupplierPriceListTest {

    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private ListaPrecioProveedorRepository listaPrecioProveedorRepository;
    @Mock
    private ItemListaPrecioProveedorRepository itemListaPrecioProveedorRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private SupplierPriceListAnalyzer supplierPriceListAnalyzer;

    @InjectMocks
    private UploadSupplierPriceList uploadSupplierPriceList;

    private UUID kiosqueroId;
    private UUID proveedorId;
    private Proveedor proveedor;
    private byte[] fileContent;

    @BeforeEach
    void setUp() {
        kiosqueroId = UUID.randomUUID();
        proveedorId = UUID.randomUUID();
        proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        proveedor.setKiosqueroId(kiosqueroId);
        proveedor.setNombre("Distribuidora Kiosco");
        fileContent = "nombre,precio,unidad,notas\nAlfajor,500,unidad,sin tacc".getBytes();
    }

    @Test
    void execute_CuandoTodoEsValido_SubeArchivoExtraeConGeminiYAutoMapea() {
        // Arrange
        String originalFilename = "precios.pdf";
        String contentType = "application/pdf";
        String mockUrl = "https://cloudinary.com/precios.pdf";

        List<RawSupplierPriceItem> rawItems = List.of(
                new RawSupplierPriceItem("Alfajor Jorgito Chocolate", new BigDecimal("550.00"), "unidad", null),
                // Este debería auto-mapearse con "Jorgito" por coincidencia de substring
                new RawSupplierPriceItem("Gaseosa Coca-Cola 500ml", new BigDecimal("1200.00"), "unidad", null),
                // Este no debería auto-mapearse
                new RawSupplierPriceItem("Producto Inexistente", new BigDecimal("100.00"), "unidad", null)
        );

        List<Producto> activeProducts = new ArrayList<>();
        Producto p1 = new Producto();
        p1.setId(UUID.randomUUID());
        p1.setNombre("Jorgito Chocolate"); // Substring de "Alfajor Jorgito Chocolate"
        activeProducts.add(p1);

        Producto p2 = new Producto();
        p2.setId(UUID.randomUUID());
        p2.setNombre("Coca Cola 500ml");
        activeProducts.add(p2);

        ListaPrecioProveedor savedLista = new ListaPrecioProveedor();
        savedLista.setId(UUID.randomUUID());
        savedLista.setProveedorId(proveedorId);

        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(storageService.uploadFile(eq(fileContent), eq(originalFilename), eq("application/pdf"))).thenReturn(mockUrl);
        when(supplierPriceListAnalyzer.analyze(eq(fileContent), eq("application/pdf"))).thenReturn(rawItems);
        when(productRepository.findAllActive()).thenReturn(activeProducts);
        when(listaPrecioProveedorRepository.save(any(ListaPrecioProveedor.class))).thenReturn(savedLista);
        when(itemListaPrecioProveedorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ListaPrecioProveedor result = uploadSupplierPriceList.execute(
                kiosqueroId, proveedorId, fileContent, originalFilename, contentType
        );

        // Assert
        assertNotNull(result);
        assertEquals(savedLista.getId(), result.getId());
        assertEquals(3, result.getItems().size());

        // Verificar mapeo del Alfajor Jorgito
        ItemListaPrecioProveedor itemJorgito = result.getItems().stream()
                .filter(i -> i.getNombreProductoProveedor().equals("Alfajor Jorgito Chocolate"))
                .findFirst().orElseThrow();
        assertEquals(p1.getId(), itemJorgito.getProductoInventarioId());
        assertFalse(itemJorgito.isMappingConfirmado());

        // Verificar ítem sin mapeo
        ItemListaPrecioProveedor itemInexistente = result.getItems().stream()
                .filter(i -> i.getNombreProductoProveedor().equals("Producto Inexistente"))
                .findFirst().orElseThrow();
        assertNull(itemInexistente.getProductoInventarioId());

        verify(proveedorRepository).findById(proveedorId);
        verify(storageService).uploadFile(any(), any(), any());
        verify(supplierPriceListAnalyzer).analyze(any(), any());
        verify(listaPrecioProveedorRepository).save(any(ListaPrecioProveedor.class));
        verify(itemListaPrecioProveedorRepository).saveAll(anyList());
    }

    @Test
    void execute_CuandoProveedorNoPerteneceAlKiosquero_LanzaAccessDeniedException() {
        // Arrange
        UUID ajenoKiosqueroId = UUID.randomUUID();
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            uploadSupplierPriceList.execute(
                    ajenoKiosqueroId, proveedorId, fileContent, "precios.pdf", "application/pdf"
            );
        });

        verify(proveedorRepository).findById(proveedorId);
        verifyNoInteractions(storageService, supplierPriceListAnalyzer, listaPrecioProveedorRepository, itemListaPrecioProveedorRepository);
    }

    @Test
    void execute_CuandoTamañoExcede5MB_LanzaFileValidationException() {
        // Arrange
        byte[] largeFileContent = new byte[6 * 1024 * 1024]; // 6MB
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));

        // Act & Assert
        FileValidationException ex = assertThrows(FileValidationException.class, () -> {
            uploadSupplierPriceList.execute(
                    kiosqueroId, proveedorId, largeFileContent, "precios.pdf", "application/pdf"
            );
        });

        assertEquals("El archivo supera el límite máximo de peso permitido (5MB)", ex.getMessage());
        verifyNoInteractions(storageService, supplierPriceListAnalyzer, listaPrecioProveedorRepository, itemListaPrecioProveedorRepository);
    }

    @Test
    void execute_CuandoMimeTypeNoEsValido_LanzaFileValidationException() {
        // Arrange
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));

        // Act & Assert
        FileValidationException ex = assertThrows(FileValidationException.class, () -> {
            uploadSupplierPriceList.execute(
                    kiosqueroId, proveedorId, fileContent, "precios.png", "image/png"
            );
        });

        assertEquals("El formato del archivo no está permitido. Solo se aceptan archivos PDF y CSV", ex.getMessage());
        verifyNoInteractions(storageService, supplierPriceListAnalyzer, listaPrecioProveedorRepository, itemListaPrecioProveedorRepository);
    }
}
