package com.recred.backend.unit.application;

import com.recred.backend.application.RawSupplierPriceItem;
import com.recred.backend.application.UploadSupplierPriceList;
import com.recred.backend.application.UploadSupplierPriceListRequest;
import com.recred.backend.application.UploadSupplierPriceListResponse;
import com.recred.backend.domain.exceptions.FileValidationException;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import com.recred.backend.domain.services.StorageService;
import com.recred.backend.domain.services.SupplierPriceListAnalyzer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    private UUID kioskOwnerId;
    private UUID supplierId;
    private Proveedor supplier;
    private byte[] fileContent;

    @BeforeEach
    void setUp() {
        kioskOwnerId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
        supplier = new Proveedor();
        supplier.setId(supplierId);
        supplier.setKiosqueroId(kioskOwnerId);
        supplier.setNombre("Distribuidora Kiosco");
        fileContent = "nombre,precio,unidad,notas\nAlfajor,500,unidad,sin tacc".getBytes();
    }

    @Test
    void executeWhenRequestIsValidUploadsFileAnalyzesItemsAndAutoMatchesProducts() {
        String originalFilename = "precios.pdf";
        String contentType = "application/pdf";
        String mockUrl = "https://cloudinary.com/precios.pdf";
        List<RawSupplierPriceItem> rawItems = rawItems();
        List<Producto> activeProducts = activeProducts();
        ListaPrecioProveedor savedPriceList = savedPriceList();
        UploadSupplierPriceListRequest request = request(
                kioskOwnerId,
                supplierId,
                fileContent,
                originalFilename,
                contentType
        );

        when(proveedorRepository.findById(supplierId)).thenReturn(Optional.of(supplier));
        when(storageService.uploadFile(
                eq(fileContent),
                eq(originalFilename),
                eq("application/pdf")
        )).thenReturn(mockUrl);
        when(supplierPriceListAnalyzer.analyze(
                eq(fileContent),
                eq("application/pdf")
        )).thenReturn(rawItems);
        when(productRepository.findAllActive()).thenReturn(activeProducts);
        when(listaPrecioProveedorRepository.save(any(ListaPrecioProveedor.class)))
                .thenReturn(savedPriceList);
        when(itemListaPrecioProveedorRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UploadSupplierPriceListResponse result = uploadSupplierPriceList.execute(request);

        assertNotNull(result);
        assertEquals(savedPriceList.getId(), result.id());
        assertEquals(3, result.items().size());
        assertAutoMatchedProduct(result, activeProducts.get(0));
        assertUnmatchedProduct(result);

        verify(proveedorRepository).findById(supplierId);
        verify(storageService).uploadFile(any(), any(), any());
        verify(supplierPriceListAnalyzer).analyze(any(), any());
        verify(listaPrecioProveedorRepository).save(any(ListaPrecioProveedor.class));
        verify(itemListaPrecioProveedorRepository).saveAll(anyList());
    }

    @Test
    void executeWhenSupplierDoesNotBelongToKioskOwnerThrowsAccessDeniedException() {
        UUID anotherKioskOwnerId = UUID.randomUUID();
        UploadSupplierPriceListRequest request = request(
                anotherKioskOwnerId,
                supplierId,
                fileContent,
                "precios.pdf",
                "application/pdf"
        );

        when(proveedorRepository.findById(supplierId)).thenReturn(Optional.of(supplier));

        Executable execution = () -> uploadSupplierPriceList.execute(request);

        assertThrows(AccessDeniedException.class, execution);

        verify(proveedorRepository).findById(supplierId);
        verifyNoInteractions(
                storageService,
                supplierPriceListAnalyzer,
                listaPrecioProveedorRepository,
                itemListaPrecioProveedorRepository
        );
    }

    @Test
    void executeWhenFileSizeExceedsLimitThrowsFileValidationException() {
        byte[] largeFileContent = new byte[6 * 1024 * 1024];
        UploadSupplierPriceListRequest request = request(
                kioskOwnerId,
                supplierId,
                largeFileContent,
                "precios.pdf",
                "application/pdf"
        );

        Executable execution = () -> uploadSupplierPriceList.execute(request);

        FileValidationException exception = assertThrows(
                FileValidationException.class,
                execution
        );

        assertEquals("File exceeds the maximum allowed size (5MB)", exception.getMessage());
        verifyNoInteractions(
                storageService,
                supplierPriceListAnalyzer,
                listaPrecioProveedorRepository,
                itemListaPrecioProveedorRepository
        );
    }

    @Test
    void executeWhenMimeTypeIsInvalidThrowsFileValidationException() {
        UploadSupplierPriceListRequest request = request(
                kioskOwnerId,
                supplierId,
                fileContent,
                "precios.png",
                "image/png"
        );

        Executable execution = () -> uploadSupplierPriceList.execute(request);

        FileValidationException exception = assertThrows(
                FileValidationException.class,
                execution
        );

        assertEquals(
                "Unsupported file format. Only PDF and CSV files are allowed",
                exception.getMessage()
        );
        verifyNoInteractions(
                storageService,
                supplierPriceListAnalyzer,
                listaPrecioProveedorRepository,
                itemListaPrecioProveedorRepository
        );
    }

    private List<RawSupplierPriceItem> rawItems() {
        return List.of(
                new RawSupplierPriceItem(
                        "Alfajor Jorgito Chocolate",
                        new BigDecimal("550.00"),
                        "unidad",
                        null
                ),
                new RawSupplierPriceItem(
                        "Gaseosa Coca-Cola 500ml",
                        new BigDecimal("1200.00"),
                        "unidad",
                        null
                ),
                new RawSupplierPriceItem(
                        "Producto Inexistente",
                        new BigDecimal("100.00"),
                        "unidad",
                        null
                )
        );
    }

    private List<Producto> activeProducts() {
        List<Producto> activeProducts = new ArrayList<>();
        Producto alfajor = new Producto();
        alfajor.setId(UUID.randomUUID());
        alfajor.setNombre("Jorgito Chocolate");
        activeProducts.add(alfajor);

        Producto soda = new Producto();
        soda.setId(UUID.randomUUID());
        soda.setNombre("Coca Cola 500ml");
        activeProducts.add(soda);

        return activeProducts;
    }

    private ListaPrecioProveedor savedPriceList() {
        ListaPrecioProveedor savedPriceList = new ListaPrecioProveedor();
        savedPriceList.setId(UUID.randomUUID());
        savedPriceList.setProveedorId(supplierId);
        return savedPriceList;
    }

    private void assertAutoMatchedProduct(
            UploadSupplierPriceListResponse result,
            Producto product
    ) {
        UploadSupplierPriceListResponse.Item item = result.items().stream()
                .filter(value -> value.supplierProductName()
                        .equals("Alfajor Jorgito Chocolate"))
                .findFirst()
                .orElseThrow();
        assertEquals(product.getId(), item.inventoryProductId());
        assertFalse(item.mappingConfirmed());
    }

    private void assertUnmatchedProduct(UploadSupplierPriceListResponse result) {
        UploadSupplierPriceListResponse.Item item = result.items().stream()
                .filter(value -> value.supplierProductName().equals("Producto Inexistente"))
                .findFirst()
                .orElseThrow();
        assertNull(item.inventoryProductId());
    }

    private UploadSupplierPriceListRequest request(
            UUID kioskOwnerId,
            UUID supplierId,
            byte[] content,
            String filename,
            String contentType
    ) {
        return new UploadSupplierPriceListRequest(
                kioskOwnerId,
                supplierId,
                content,
                filename,
                contentType
        );
    }
}
