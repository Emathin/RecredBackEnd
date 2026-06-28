package com.recred.backend.application;

import com.recred.backend.domain.models.AlternativaProveedor;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.models.RecomendacionProveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RecommendSupplierForProducts {

    private final ItemListaPrecioProveedorRepository itemRepository;
    private final ProveedorRepository proveedorRepository;

    public RecommendSupplierForProducts(
            ItemListaPrecioProveedorRepository itemRepository,
            ProveedorRepository proveedorRepository
    ) {
        this.itemRepository = itemRepository;
        this.proveedorRepository = proveedorRepository;
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
        // (ordenados por producto y menor precio)
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

        // 4. Armar la recomendación recomendada (más económica) y sus alternativas
        for (Map.Entry<UUID, List<ItemListaPrecioProveedor>> entry : itemsByProduct.entrySet()) {
            UUID prodId = entry.getKey();
            List<ItemListaPrecioProveedor> prodItems = entry.getValue();

            if (prodItems.isEmpty()) {
                RecomendacionProveedor rec = new RecomendacionProveedor();
                rec.setProductoInventarioId(prodId);
                rec.setNombreProducto("Producto sin cotización");
                rec.setAlternativas(List.of());
                recomendaciones.add(rec);
                continue;
            }

            // Recomendado es el primero (menor precio debido al orden de la consulta)
            ItemListaPrecioProveedor recommendedItem = prodItems.get(0);
            UUID recommendedListId = recommendedItem.getListaPrecioId();
            UUID recommendedProviderId = listaProveedorMap.get(recommendedListId);
            String recommendedProviderName = recommendedProviderId != null 
                    ? proveedorNombres.get(recommendedProviderId) : "Desconocido";

            // Alternativas
            List<AlternativaProveedor> alternativas = new ArrayList<>();
            for (int i = 1; i < prodItems.size(); i++) {
                ItemListaPrecioProveedor altItem = prodItems.get(i);
                UUID altListId = altItem.getListaPrecioId();
                UUID altProviderId = listaProveedorMap.get(altListId);
                String altProviderName = altProviderId != null 
                        ? proveedorNombres.get(altProviderId) : "Desconocido";

                alternativas.add(new AlternativaProveedor(
                        altProviderId,
                        altProviderName,
                        altItem.getPrecio(),
                        altItem.getUnidad()
                ));
            }

            recomendaciones.add(new RecomendacionProveedor(
                    prodId,
                    recommendedItem.getNombreProductoInventario(),
                    recommendedProviderId,
                    recommendedProviderName,
                    recommendedItem.getPrecio(),
                    recommendedItem.getUnidad(),
                    alternativas
            ));
        }

        return recomendaciones;
    }
}
