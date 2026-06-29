package com.recred.backend.application;

import com.recred.backend.domain.models.AlternativaProveedor;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.models.RecomendacionProveedor;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecommendSupplierForProducts {

    private final ItemListaPrecioProveedorRepository itemRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductRepository productRepository;

    public RecommendSupplierForProducts(
            ItemListaPrecioProveedorRepository itemRepository,
            ProveedorRepository proveedorRepository,
            ProductRepository productRepository
    ) {
        this.itemRepository = itemRepository;
        this.proveedorRepository = proveedorRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<RecomendacionProveedor> execute(UUID kiosqueroId, List<UUID> productoIds) {
        if (productoIds == null || productoIds.isEmpty()) {
            return List.of();
        }

        // 1. Obtener proveedores del kiosquero para mapear IDs a nombres
        List<Proveedor> proveedores = proveedorRepository.findByKiosqueroId(kiosqueroId);
        Map<UUID, String> proveedorNombres = new HashMap<>();
        Map<UUID, UUID> listaProveedorMap = new HashMap<>();

        for (Proveedor p : proveedores) {
            proveedorNombres.put(p.getId(), p.getNombre());
            if (p.getListasPrecios() != null) {
                for (ListaPrecioProveedor l : p.getListasPrecios()) {
                    listaProveedorMap.put(l.getId(), p.getId());
                }
            }
        }

        // 2. Obtener todos los ítems activos para los productos seleccionados
        List<ItemListaPrecioProveedor> items = itemRepository
                .findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, productoIds);

        // 3. Inicializar el mapa de resultados garantizando el orden y presencia
        // de todos los productos solicitados
        Map<UUID, List<ItemListaPrecioProveedor>> itemsByProduct = new LinkedHashMap<>();
        for (UUID prodId : productoIds) {
            itemsByProduct.put(prodId, new ArrayList<>());
        }

        for (ItemListaPrecioProveedor item : items) {
            if (item.getProductoInventarioId() != null 
                    && itemsByProduct.containsKey(item.getProductoInventarioId())) {
                itemsByProduct.get(item.getProductoInventarioId()).add(item);
            }
        }

        List<RecomendacionProveedor> recomendaciones = new ArrayList<>();

        // 4. Armar la recomendación recomendada (más económica unitariamente) y sus alternativas
        for (Map.Entry<UUID, List<ItemListaPrecioProveedor>> entry : itemsByProduct.entrySet()) {
            UUID prodId = entry.getKey();
            List<ItemListaPrecioProveedor> prodItems = entry.getValue();

            if (prodItems.isEmpty()) {
                RecomendacionProveedor rec = new RecomendacionProveedor();
                rec.setProductoInventarioId(prodId);
                String nombre = productRepository.findById(prodId)
                        .map(Producto::getNombre)
                        .orElse("Producto no encontrado");
                rec.setNombreProducto(nombre); // Frontend will mark this as non-quoted
                rec.setAlternativas(List.of());
                recomendaciones.add(rec);
                continue;
            }

            // Ordenar ítems en memoria por precio unitario calculado ascendentemente
            prodItems.sort((a, b) -> {
                BigDecimal unitPriceA = calcularPrecioUnitario(a.getPrecio(), a.getUnidad());
                BigDecimal unitPriceB = calcularPrecioUnitario(b.getPrecio(), b.getUnidad());
                return unitPriceA.compareTo(unitPriceB);
            });

            // Recomendado es el primero (menor precio unitario debido al ordenamiento)
            ItemListaPrecioProveedor recommendedItem = prodItems.get(0);
            UUID recommendedListId = recommendedItem.getListaPrecioId();
            UUID recommendedProviderId = listaProveedorMap.get(recommendedListId);
            String recommendedProviderName = recommendedProviderId != null 
                    ? proveedorNombres.get(recommendedProviderId) : "Desconocido";
            BigDecimal bestUnitPrice = calcularPrecioUnitario(
                    recommendedItem.getPrecio(), recommendedItem.getUnidad());

            // Alternativas
            List<AlternativaProveedor> alternativas = new ArrayList<>();
            for (int i = 1; i < prodItems.size(); i++) {
                ItemListaPrecioProveedor altItem = prodItems.get(i);
                UUID altListId = altItem.getListaPrecioId();
                UUID altProviderId = listaProveedorMap.get(altListId);
                String altProviderName = altProviderId != null 
                        ? proveedorNombres.get(altProviderId) : "Desconocido";
                BigDecimal altUnitPrice = calcularPrecioUnitario(
                        altItem.getPrecio(), altItem.getUnidad());

                alternativas.add(new AlternativaProveedor(
                        altProviderId,
                        altProviderName,
                        altItem.getPrecio(),
                        altItem.getUnidad(),
                        altUnitPrice
                ));
            }

            recomendaciones.add(new RecomendacionProveedor(
                    prodId,
                    recommendedItem.getNombreProductoInventario(),
                    recommendedProviderId,
                    recommendedProviderName,
                    recommendedItem.getPrecio(),
                    recommendedItem.getUnidad(),
                    bestUnitPrice,
                    alternativas
            ));
        }

        return recomendaciones;
    }

    public static BigDecimal calcularPrecioUnitario(BigDecimal precio, String unidad) {
        if (precio == null) {
            return BigDecimal.ZERO;
        }
        int cantidad = extraerCantidad(unidad);
        return precio.divide(BigDecimal.valueOf(cantidad), 2, java.math.RoundingMode.HALF_UP);
    }

    public static int extraerCantidad(String unidad) {
        if (unidad == null || unidad.isBlank()) {
            return 1;
        }
        String s = unidad.toLowerCase().trim();

        // 1. "pack de 6", "caja x12", "x 24", etc.
        Pattern p1 = Pattern.compile(
                "(?:pack|caja|bolsa|bulto|display|promo)?\\s*(?:x|de)\\s*(\\d+)", 
                Pattern.CASE_INSENSITIVE
        );
        Matcher m1 = p1.matcher(s);
        if (m1.find()) {
            try {
                int val = Integer.parseInt(m1.group(1));
                if (val > 0) {
                    return val;
                }
            } catch (NumberFormatException e) {}
        }

        // 2. "pack 6", "caja 12"
        Pattern p2 = Pattern.compile(
                "(?:pack|caja|bolsa|bulto|display|promo)\\s+(\\d+)", 
                Pattern.CASE_INSENSITIVE
        );
        Matcher m2 = p2.matcher(s);
        if (m2.find()) {
            try {
                int val = Integer.parseInt(m2.group(1));
                if (val > 0) {
                    return val;
                }
            } catch (NumberFormatException e) {}
        }

        // 3. "12u", "6 uds", "24 unidades"
        Pattern p3 = Pattern.compile(
                "(\\d+)\\s*(?:unidades|unids|uds|ud|un|u)\\b", 
                Pattern.CASE_INSENSITIVE
        );
        Matcher m3 = p3.matcher(s);
        if (m3.find()) {
            try {
                int val = Integer.parseInt(m3.group(1));
                if (val > 0) {
                    return val;
                }
            } catch (NumberFormatException e) {}
        }

        return 1;
    }
}
