package com.recred.backend.unit.application;

import com.recred.backend.application.RawSupplierPriceItem;
import com.recred.backend.application.UploadSupplierPriceList;
import com.recred.backend.application.UploadSupplierPriceListRequest;
import com.recred.backend.application.UploadSupplierPriceListResponse;
import com.recred.backend.domain.exceptions.FileValidationException;
import com.recred.backend.domain.exceptions.OperationNotAllowedException;
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
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
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

    private static final String PDF_FILENAME = "precios.pdf";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String PRICE_LIST_URL = "https://cloudinary.com/precios.pdf";

    @Mock
    private ProveedorRepository supplierRepository;
    @Mock
    private ListaPrecioProveedorRepository priceListRepository;
    @Mock
    private ItemListaPrecioProveedorRepository priceListItemRepository;
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
    private List<Producto> activeProducts;
    private ListaPrecioProveedor savedPriceList;
    private UploadSupplierPriceListRequest uploadRequest;
    private UploadSupplierPriceListResponse uploadResponse;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        kioskOwnerId = randomUUID();
        supplierId = randomUUID();
        supplier = supplier(supplierId, kioskOwnerId);
        fileContent = "nombre,precio,unidad,notas\nAlfajor,500,unidad,sin tacc"
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void givenValidPriceList_whenUploadingSupplierPriceList_thenUploadsAndAutoMatchesItems() {
        givenDefaultUploadRequest();
        givenSupplierExists();
        givenFileIsUploaded();
        givenAnalyzerReturnsItems();
        givenActiveProducts();
        givenPriceListCanBeSaved();
        givenItemsCanBeSaved();

        whenUploadingSupplierPriceList();

        thenPriceListWasReturned();
        thenMatchedProductWasIncluded();
        thenUnmatchedProductWasIncluded();
        thenUploadFlowWasExecuted();
    }

    @Test
    void givenSupplierFromAnotherKioskOwner_whenUploadingPriceList_thenThrowsOperationNotAllowed() {
        givenUploadRequestForKioskOwner(randomUUID());
        givenSupplierExists();

        whenUploadSupplierPriceListFails(OperationNotAllowedException.class);

        thenSupplierWasRead();
        thenUploadDependenciesWereNotCalled();
    }

    @Test
    void givenOversizedFile_whenUploadingPriceList_thenThrowsFileValidationException() {
        givenUploadRequestWithFile(new byte[6 * 1024 * 1024], PDF_FILENAME, PDF_CONTENT_TYPE);

        whenUploadSupplierPriceListFails(FileValidationException.class);

        thenErrorMessageIs("File exceeds the maximum allowed size (5MB)");
        thenNoDependencyWasCalled();
    }

    @Test
    void givenInvalidMimeType_whenUploadingPriceList_thenThrowsFileValidationException() {
        givenUploadRequestWithFile(fileContent, "precios.png", "image/png");

        whenUploadSupplierPriceListFails(FileValidationException.class);

        thenErrorMessageIs("Unsupported file format. Only PDF and CSV files are allowed");
        thenNoDependencyWasCalled();
    }

    private void givenDefaultUploadRequest() {
        givenUploadRequestWithFile(fileContent, PDF_FILENAME, PDF_CONTENT_TYPE);
    }

    private void givenUploadRequestForKioskOwner(UUID requestKioskOwnerId) {
        uploadRequest = request(
                requestKioskOwnerId,
                supplierId,
                fileContent,
                PDF_FILENAME,
                PDF_CONTENT_TYPE);
    }

    private void givenUploadRequestWithFile(
            byte[] content,
            String filename,
            String contentType) {
        uploadRequest = request(kioskOwnerId, supplierId, content, filename, contentType);
    }

    private void givenSupplierExists() {
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(supplier));
    }

    private void givenFileIsUploaded() {
        when(storageService.uploadFile(
                eq(fileContent),
                eq(PDF_FILENAME),
                eq(PDF_CONTENT_TYPE)))
                .thenReturn(PRICE_LIST_URL);
    }

    private void givenAnalyzerReturnsItems() {
        when(supplierPriceListAnalyzer.analyze(eq(fileContent), eq(PDF_CONTENT_TYPE)))
                .thenReturn(rawItems());
    }

    private void givenActiveProducts() {
        activeProducts = activeProducts();
        when(productRepository.findAllActive()).thenReturn(activeProducts);
    }

    private void givenPriceListCanBeSaved() {
        savedPriceList = savedPriceList();
        when(priceListRepository.save(any(ListaPrecioProveedor.class)))
                .thenReturn(savedPriceList);
    }

    private void givenItemsCanBeSaved() {
        when(priceListItemRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void whenUploadingSupplierPriceList() {
        uploadResponse = uploadSupplierPriceList.execute(uploadRequest);
    }

    private <T extends Throwable> void whenUploadSupplierPriceListFails(
            Class<T> exceptionType) {
        Executable execution = () -> uploadSupplierPriceList.execute(uploadRequest);
        thrownException = assertThrows(exceptionType, execution);
    }

    private void thenPriceListWasReturned() {
        assertNotNull(uploadResponse);
        assertEquals(savedPriceList.getId(), uploadResponse.id());
        assertEquals(3, uploadResponse.items().size());
    }

    private void thenMatchedProductWasIncluded() {
        UploadSupplierPriceListResponse.Item item = responseItem(
                "Alfajor Jorgito Chocolate");
        assertEquals(activeProducts.get(0).getId(), item.inventoryProductId());
        assertFalse(item.mappingConfirmed());
    }

    private void thenUnmatchedProductWasIncluded() {
        UploadSupplierPriceListResponse.Item item = responseItem("Producto Inexistente");
        assertNull(item.inventoryProductId());
    }

    private void thenUploadFlowWasExecuted() {
        verify(supplierRepository).findById(supplierId);
        verify(storageService).uploadFile(fileContent, PDF_FILENAME, PDF_CONTENT_TYPE);
        verify(supplierPriceListAnalyzer).analyze(fileContent, PDF_CONTENT_TYPE);
        verify(productRepository).findAllActive();
        verify(priceListRepository).save(any(ListaPrecioProveedor.class));
        verify(priceListItemRepository).saveAll(anyList());
    }

    private void thenSupplierWasRead() {
        verify(supplierRepository).findById(supplierId);
    }

    private void thenUploadDependenciesWereNotCalled() {
        verifyNoInteractions(
                storageService,
                supplierPriceListAnalyzer,
                productRepository,
                priceListRepository,
                priceListItemRepository);
    }

    private void thenNoDependencyWasCalled() {
        verifyNoInteractions(
                supplierRepository,
                storageService,
                supplierPriceListAnalyzer,
                productRepository,
                priceListRepository,
                priceListItemRepository);
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private UploadSupplierPriceListResponse.Item responseItem(String supplierProductName) {
        return uploadResponse.items().stream()
                .filter(item -> item.supplierProductName().equals(supplierProductName))
                .findFirst()
                .orElseThrow();
    }

    private List<RawSupplierPriceItem> rawItems() {
        return List.of(
                new RawSupplierPriceItem(
                        "Alfajor Jorgito Chocolate",
                        new BigDecimal("550.00"),
                        "unidad",
                        null),
                new RawSupplierPriceItem(
                        "Gaseosa Coca-Cola 500ml",
                        new BigDecimal("1200.00"),
                        "unidad",
                        null),
                new RawSupplierPriceItem(
                        "Producto Inexistente",
                        new BigDecimal("100.00"),
                        "unidad",
                        null));
    }

    private List<Producto> activeProducts() {
        List<Producto> products = new ArrayList<>();
        products.add(product("Jorgito Chocolate"));
        products.add(product("Coca Cola 500ml"));
        return products;
    }

    private Proveedor supplier(UUID id, UUID ownerId) {
        Proveedor supplier = new Proveedor();
        supplier.setId(id);
        supplier.setKiosqueroId(ownerId);
        supplier.setNombre("Distribuidora Kiosco");
        return supplier;
    }

    private Producto product(String name) {
        Producto product = new Producto();
        product.setId(randomUUID());
        product.setNombre(name);
        return product;
    }

    private ListaPrecioProveedor savedPriceList() {
        ListaPrecioProveedor priceList = new ListaPrecioProveedor();
        priceList.setId(randomUUID());
        priceList.setProveedorId(supplierId);
        return priceList;
    }

    private UploadSupplierPriceListRequest request(
            UUID kioskOwnerId,
            UUID supplierId,
            byte[] content,
            String filename,
            String contentType) {
        return new UploadSupplierPriceListRequest(
                kioskOwnerId,
                supplierId,
                content,
                filename,
                contentType);
    }
}
