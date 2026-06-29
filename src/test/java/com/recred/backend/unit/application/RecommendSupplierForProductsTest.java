package com.recred.backend.unit.application;

import com.recred.backend.application.RecommendSupplierForProducts;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.models.RecomendacionProveedor;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendSupplierForProductsTest {

    @Mock
    private ItemListaPrecioProveedorRepository itemRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductRepository productRepository;

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

        when(itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, List.of(productoId)))
                .thenReturn(List.of(itemCaro, itemBarato));

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
        assertEquals(new BigDecimal("450.00"), rec.getMejorPrecioUnitario());

        // Alternativas
        assertEquals(1, rec.getAlternativas().size());
        assertEquals(proveedorAId, rec.getAlternativas().get(0).proveedorId());
        assertEquals("Proveedor A (Caro)", rec.getAlternativas().get(0).nombreProveedor());
        assertEquals(new BigDecimal("600.00"), rec.getAlternativas().get(0).precio());
        assertEquals(new BigDecimal("600.00"), rec.getAlternativas().get(0).precioUnitario());

        verify(proveedorRepository).findByKiosqueroId(kiosqueroId);
        verify(itemRepository).findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, List.of(productoId));
    }

    @Test
    void execute_CuandoComparacionPorUnidadSeleccionaElMasBaratoUnitariamente() {
        // Arrange
        UUID proveedorAId = UUID.randomUUID();
        Proveedor proveedorA = new Proveedor();
        proveedorA.setId(proveedorAId);
        proveedorA.setNombre("Proveedor A (Pack x6)");

        ListaPrecioProveedor listaA = new ListaPrecioProveedor();
        listaA.setId(UUID.randomUUID());
        listaA.setProveedorId(proveedorAId);
        proveedorA.setListasPrecios(List.of(listaA));

        UUID proveedorBId = UUID.randomUUID();
        Proveedor proveedorB = new Proveedor();
        proveedorB.setId(proveedorBId);
        proveedorB.setNombre("Proveedor B (Unitario)");

        ListaPrecioProveedor listaB = new ListaPrecioProveedor();
        listaB.setId(UUID.randomUUID());
        listaB.setProveedorId(proveedorBId);
        proveedorB.setListasPrecios(List.of(listaB));

        when(proveedorRepository.findByKiosqueroId(kiosqueroId)).thenReturn(List.of(proveedorA, proveedorB));

        // Proveedor A vende pack de 6 a $3000 ($500 por unidad)
        ItemListaPrecioProveedor itemPack = new ItemListaPrecioProveedor();
        itemPack.setId(UUID.randomUUID());
        itemPack.setListaPrecioId(listaA.getId());
        itemPack.setProductoInventarioId(productoId);
        itemPack.setNombreProductoInventario("Gaseosa 500ml");
        itemPack.setPrecio(new BigDecimal("3000.00"));
        itemPack.setUnidad("Pack de 6");

        // Proveedor B vende unidad a $600 ($600 por unidad)
        ItemListaPrecioProveedor itemUnitario = new ItemListaPrecioProveedor();
        itemUnitario.setId(UUID.randomUUID());
        itemUnitario.setListaPrecioId(listaB.getId());
        itemUnitario.setProductoInventarioId(productoId);
        itemUnitario.setNombreProductoInventario("Gaseosa 500ml");
        itemUnitario.setPrecio(new BigDecimal("600.00"));
        itemUnitario.setUnidad("unidad");

        when(itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, List.of(productoId)))
                .thenReturn(List.of(itemUnitario, itemPack));

        // Act
        List<RecomendacionProveedor> result = recommendSupplierForProducts.execute(kiosqueroId, List.of(productoId));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        RecomendacionProveedor rec = result.get(0);
        assertEquals(productoId, rec.getProductoInventarioId());
        // Debe recomendar el Pack de 6 (Proveedor A) porque el precio unitario es $500, menor que $600
        assertEquals(proveedorAId, rec.getProveedorRecomendadoId());
        assertEquals("Proveedor A (Pack x6)", rec.getNombreProveedorRecomendado());
        assertEquals(new BigDecimal("3000.00"), rec.getMejorPrecio()); // Precio del pack
        assertEquals(new BigDecimal("500.00"), rec.getMejorPrecioUnitario()); // Precio unitario calculado

        // Alternativas (Proveedor B)
        assertEquals(1, rec.getAlternativas().size());
        assertEquals(proveedorBId, rec.getAlternativas().get(0).proveedorId());
        assertEquals(new BigDecimal("600.00"), rec.getAlternativas().get(0).precio());
        assertEquals(new BigDecimal("600.00"), rec.getAlternativas().get(0).precioUnitario());
    }

    @Test
    void execute_CuandoProductoNoTieneCotizacion_RetornaRecomendacionVaciaConNombreReal() {
        // Arrange
        when(proveedorRepository.findByKiosqueroId(kiosqueroId)).thenReturn(List.of());
        when(itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, List.of(productoId)))
                .thenReturn(List.of());

        Producto mockProduct = new Producto();
        mockProduct.setId(productoId);
        mockProduct.setNombre("Alfajor Triple");
        when(productRepository.findById(productoId)).thenReturn(Optional.of(mockProduct));

        // Act
        List<RecomendacionProveedor> result = recommendSupplierForProducts.execute(kiosqueroId, List.of(productoId));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        RecomendacionProveedor rec = result.get(0);
        assertEquals(productoId, rec.getProductoInventarioId());
        assertEquals("Alfajor Triple", rec.getNombreProducto());
        assertNull(rec.getProveedorRecomendadoId());
        assertEquals(0, rec.getAlternativas().size());

        verify(proveedorRepository).findByKiosqueroId(kiosqueroId);
        verify(itemRepository).findItemsActivosByKiosqueroIdAndProductoIds(kiosqueroId, List.of(productoId));
        verify(productRepository).findById(productoId);
    }
}
