package com.recred.backend.unit.application;

import com.recred.backend.application.RecommendSupplierForProducts;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.models.RecomendacionProveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendSupplierForProductsTest {

    @Mock
    private ItemListaPrecioProveedorRepository itemRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private RecommendSupplierForProducts recommendSupplierForProducts;

    private UUID kiosqueroId;
    private UUID productoId;

    @BeforeEach
    void setUp() {
        kiosqueroId = UUID.randomUUID();
        productoId = UUID.randomUUID();
    }

    @Test
    void execute_CuandoHayMultiplesProveedores_RecomiendaElMasBaratoYAgrupaAlternativas() {
        // Arrange
        UUID proveedorAId = UUID.randomUUID();
        Proveedor proveedorA = new Proveedor();
        proveedorA.setId(proveedorAId);
        proveedorA.setNombre("Proveedor A (Caro)");

        ListaPrecioProveedor listaA = new ListaPrecioProveedor();
        listaA.setId(UUID.randomUUID());
        listaA.setProveedorId(proveedorAId);
        proveedorA.setListasPrecios(List.of(listaA));

        UUID proveedorBId = UUID.randomUUID();
        Proveedor proveedorB = new Proveedor();
        proveedorB.setId(proveedorBId);
        proveedorB.setNombre("Proveedor B (Barato)");

        ListaPrecioProveedor listaB = new ListaPrecioProveedor();
        listaB.setId(UUID.randomUUID());
        listaB.setProveedorId(proveedorBId);
        proveedorB.setListasPrecios(List.of(listaB));

        when(proveedorRepository.findByKiosqueroId(kiosqueroId)).thenReturn(List.of(proveedorA, proveedorB));

        // Ítems de listas de precios activos mapeados al producto
        ItemListaPrecioProveedor itemCaro = new ItemListaPrecioProveedor();
        itemCaro.setId(UUID.randomUUID());
        itemCaro.setListaPrecioId(listaA.getId());
        itemCaro.setProductoInventarioId(productoId);
        itemCaro.setNombreProductoInventario("Alfajor Triple");
        itemCaro.setPrecio(new BigDecimal("600.00"));
        itemCaro.setUnidad("unidad");

        ItemListaPrecioProveedor itemBarato = new ItemListaPrecioProveedor();
        itemBarato.setId(UUID.randomUUID());
        itemBarato.setListaPrecioId(listaB.getId());
        itemBarato.setProductoInventarioId(productoId);
        itemBarato.setNombreProductoInventario("Alfajor Triple");
        itemBarato.setPrecio(new BigDecimal("450.00")); // Más barato
        itemBarato.setUnidad("unidad");

        // El repositorio simula el orden de precio ASC
        when(itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, List.of(productoId)))
                .thenReturn(List.of(itemBarato, itemCaro));

        // Act
        List<RecomendacionProveedor> result = recommendSupplierForProducts.execute(kiosqueroId, List.of(productoId));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        RecomendacionProveedor rec = result.get(0);
        assertEquals(productoId, rec.getProductoInventarioId());
        assertEquals(proveedorBId, rec.getProveedorRecomendadoId());
        assertEquals("Proveedor B (Barato)", rec.getNombreProveedorRecomendado());
        assertEquals(new BigDecimal("450.00"), rec.getMejorPrecio());

        // Alternativas
        assertEquals(1, rec.getAlternativas().size());
        assertEquals(proveedorAId, rec.getAlternativas().get(0).proveedorId());
        assertEquals("Proveedor A (Caro)", rec.getAlternativas().get(0).nombreProveedor());
        assertEquals(new BigDecimal("600.00"), rec.getAlternativas().get(0).precio());

        verify(proveedorRepository).findByKiosqueroId(kiosqueroId);
        verify(itemRepository).findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, List.of(productoId));
    }

    @Test
    void execute_CuandoProductoNoTieneCotizacion_RetornaRecomendacionVacia() {
        // Arrange
        when(proveedorRepository.findByKiosqueroId(kiosqueroId)).thenReturn(List.of());
        when(itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, List.of(productoId)))
                .thenReturn(List.of());

        // Act
        List<RecomendacionProveedor> result = recommendSupplierForProducts.execute(kiosqueroId, List.of(productoId));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        RecomendacionProveedor rec = result.get(0);
        assertEquals(productoId, rec.getProductoInventarioId());
        assertNull(rec.getProveedorRecomendadoId());
        assertEquals(0, rec.getAlternativas().size());

        verify(proveedorRepository).findByKiosqueroId(kiosqueroId);
        verify(itemRepository).findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, List.of(productoId));
    }
}
