package com.recred.backend.application;

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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UploadSupplierPriceList {

    private final ProveedorRepository proveedorRepository;
    private final ListaPrecioProveedorRepository listaPrecioProveedorRepository;
    private final ItemListaPrecioProveedorRepository itemListaPrecioProveedorRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;
    private final SupplierPriceListAnalyzer supplierPriceListAnalyzer;

    public UploadSupplierPriceList(
            ProveedorRepository proveedorRepository,
            ListaPrecioProveedorRepository listaPrecioProveedorRepository,
            ItemListaPrecioProveedorRepository itemListaPrecioProveedorRepository,
            ProductRepository productRepository,
            StorageService storageService,
            SupplierPriceListAnalyzer supplierPriceListAnalyzer
    ) {
        this.proveedorRepository = proveedorRepository;
        this.listaPrecioProveedorRepository = listaPrecioProveedorRepository;
        this.itemListaPrecioProveedorRepository = itemListaPrecioProveedorRepository;
        this.productRepository = productRepository;
        this.storageService = storageService;
        this.supplierPriceListAnalyzer = supplierPriceListAnalyzer;
    }

    public ListaPrecioProveedor execute(
            UUID kiosqueroId,
            UUID proveedorId,
            byte[] fileContent,
            String originalFilename,
            String contentType
    ) {
        // 1. Validar propiedad del proveedor
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        if (!proveedor.getKiosqueroId().equals(kiosqueroId)) {
            throw new AccessDeniedException("No tiene permisos sobre este proveedor");
        }

        // 2. Validar archivo (MIME type y tamaño de 5MB)
        validateFile(fileContent, originalFilename, contentType);

        // 3. Resolver tipo correcto para mandar a Gemini y subir
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";
        boolean isPdf = "application/pdf".equalsIgnoreCase(contentType) || ".pdf".equals(ext);
        String resolvedMimeType = isPdf ? "application/pdf" : "text/csv";

        // 4. Subir archivo a Cloudinary
        String urlArchivo = storageService.uploadFile(
                fileContent, originalFilename, resolvedMimeType);

        // 5. Analizar con Gemini para obtener ítems
        List<RawSupplierPriceItem> rawItems = supplierPriceListAnalyzer
                .analyze(fileContent, resolvedMimeType);

        // 6. Obtener productos activos para el auto-matching
        List<Producto> activeProducts = productRepository.findAllActive();

        // 7. Crear y persistir la ListaPrecioProveedor
        ListaPrecioProveedor listaPrecio = new ListaPrecioProveedor();
        listaPrecio.setProveedorId(proveedorId);
        listaPrecio.setUrlArchivo(urlArchivo);
        listaPrecio.setNombreOriginal(originalFilename);
        listaPrecio.setActiva(true);
        listaPrecio.setCreadoEn(LocalDateTime.now());

        ListaPrecioProveedor savedLista = listaPrecioProveedorRepository.save(listaPrecio);

        // 8. Auto-mapear los productos y preparar los items
        List<ItemListaPrecioProveedor> itemsToSave = new ArrayList<>();
        for (RawSupplierPriceItem rawItem : rawItems) {
            ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
            item.setListaPrecioId(savedLista.getId());
            item.setNombreProductoProveedor(rawItem.nombreProducto());
            item.setPrecio(rawItem.precio());
            item.setUnidad(rawItem.unidad());
            item.setNotas(rawItem.notas());

            UUID matchId = tryAutoMatch(rawItem.nombreProducto(), activeProducts);
            if (matchId != null) {
                item.setProductoInventarioId(matchId);
                // Buscar el nombre del producto en la lista local para desnormalizar
                activeProducts.stream()
                        .filter(p -> p.getId().equals(matchId))
                        .findFirst()
                        .ifPresent(p -> item.setNombreProductoInventario(p.getNombre()));
                item.setMappingConfirmado(false); // IA sugirió el mapeo
            } else {
                item.setProductoInventarioId(null);
                item.setMappingConfirmado(false);
            }
            itemsToSave.add(item);
        }

        // 9. Persistir todos los items
        List<ItemListaPrecioProveedor> savedItems =
                itemListaPrecioProveedorRepository.saveAll(itemsToSave);
        savedLista.setItems(savedItems);

        return savedLista;
    }

    private void validateFile(byte[] fileContent, String originalFilename, String contentType) {
        if (fileContent == null || fileContent.length == 0) {
            throw new FileValidationException("El archivo no puede estar vacío");
        }

        if (fileContent.length > 5 * 1024 * 1024) {
            throw new FileValidationException(
                    "El archivo supera el límite máximo de peso permitido (5MB)");
        }

        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";

        boolean isPdf = "application/pdf".equalsIgnoreCase(contentType) || ".pdf".equals(ext);
        boolean isCsv = "text/csv".equalsIgnoreCase(contentType) 
                || ".csv".equals(ext) 
                || "text/plain".equalsIgnoreCase(contentType);

        if (!isPdf && !isCsv) {
            throw new FileValidationException(
                    "El formato del archivo no está permitido. Solo se aceptan archivos PDF y CSV");
        }
    }

    private UUID tryAutoMatch(String providerProductName, List<Producto> activeProducts) {
        if (providerProductName == null || providerProductName.isBlank()) {
            return null;
        }

        String normalizedProvider = normalizeString(providerProductName);

        // 1. Coincidencia exacta (normalizada)
        for (Producto prod : activeProducts) {
            if (normalizeString(prod.getNombre()).equals(normalizedProvider)) {
                return prod.getId();
            }
        }

        // 2. Coincidencia de substring
        for (Producto prod : activeProducts) {
            String normalizedProd = normalizeString(prod.getNombre());
            if (normalizedProvider.contains(normalizedProd) 
                    || normalizedProd.contains(normalizedProvider)) {
                return prod.getId();
            }
        }

        return null;
    }

    private String normalizeString(String text) {
        if (text == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        return normalized.toLowerCase().replaceAll("[^a-z0-9]", " ").trim().replaceAll("\\s+", " ");
    }
}
