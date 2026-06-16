package com.recred.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.AdjustInventoryStock;
import com.recred.backend.application.AdjustInventoryStockCommand;
import com.recred.backend.application.GetStockMovements;
import com.recred.backend.application.InventoryQuickActionCommand;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.models.TipoMovimientoStock;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.recred.backend.infrastructure.web.InventoryController;
import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private AdjustInventoryStock adjustInventoryStock;
    @Mock
    private ListInventoryProducts listInventoryProducts;
    @Mock
    private GetStockMovements getStockMovements;
    @Mock
    private KiosqueroAccessGuard kiosqueroAccessGuard;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new InventoryController(
                adjustInventoryStock,
                listInventoryProducts,
                getStockMovements,
                kiosqueroAccessGuard
        )).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void listInventory_ShouldReturnProductsWithAvailability() throws Exception {
        UUID buffetId = UUID.randomUUID();
        Producto product = product();

        when(listInventoryProducts.execute(buffetId)).thenReturn(List.of(product));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/inventory/{buffetId}",
                        buffetId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId().toString()))
                .andExpect(jsonPath("$[0].stockActual").value(12))
                .andExpect(jsonPath("$[0].stockReservado").value(2))
                .andExpect(jsonPath("$[0].stockDisponible").value(10))
                .andExpect(jsonPath("$[0].estadoInventario").value("DISPONIBLE"))
                .andExpect(jsonPath("$[0].tipoManejoInventario").value("STOCK_EXACTO"));

        verify(listInventoryProducts).execute(buffetId);
    }

    @Test
    void adjustStock_ShouldReturnUpdatedInventory() throws Exception {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Inventario inventory = inventory(buffetId, productId);

        when(adjustInventoryStock.execute(any(AdjustInventoryStockCommand.class)))
                .thenReturn(inventory);

        mockMvc.perform(patch(
                        "/api/v1/inventory/{buffetId}/products/{productId}/stock",
                        buffetId,
                        productId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "stockActual", 12,
                                "motivo", "Correccion manual",
                                "usuarioId", userId.toString()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(12))
                .andExpect(jsonPath("$.stockDisponible").value(10));

        ArgumentCaptor<AdjustInventoryStockCommand> captor =
                ArgumentCaptor.forClass(AdjustInventoryStockCommand.class);
        verify(adjustInventoryStock).execute(captor.capture());
        assertEquals(buffetId, captor.getValue().buffetId());
        assertEquals(productId, captor.getValue().productId());
        assertEquals(12, captor.getValue().stockActual());
        assertEquals("Correccion manual", captor.getValue().motivo());
        assertEquals(userId, captor.getValue().usuarioId());
    }

    @Test
    void inventoryOverview_ShouldReturnSimpleSellerState() throws Exception {
        UUID buffetId = UUID.randomUUID();
        Producto product = product();
        product.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        product.setStockActual(20);
        product.setStockReservado(5);
        product.setStockDisponible(15);
        product.setCupoMaximoDiario(20);

        when(listInventoryProducts.execute(buffetId)).thenReturn(List.of(product));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/inventory/{buffetId}/overview",
                        buffetId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(product.getId().toString()))
                .andExpect(jsonPath("$[0].tipoManejoInventario").value("CUPO_DIARIO"))
                .andExpect(jsonPath("$[0].estadoInventario").value("DISPONIBLE"))
                .andExpect(jsonPath("$[0].stockDisponible").value(15))
                .andExpect(jsonPath("$[0].cupoMaximoDiario").value(20))
                .andExpect(jsonPath("$[0].cupoDisponibleDia").value(15))
                .andExpect(jsonPath("$[0].disponible").value(true))
                .andExpect(jsonPath("$[0].agotado").value(false));
    }

    @Test
    void quickAction_ShouldMapSimpleActionCommand() throws Exception {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Inventario inventory = inventory(buffetId, productId);

        when(adjustInventoryStock.execute(any(InventoryQuickActionCommand.class)))
                .thenReturn(inventory);

        mockMvc.perform(patch(
                        "/api/v1/inventory/{buffetId}/products/{productId}/quick-action",
                        buffetId,
                        productId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "ADD_STOCK",
                                "quantity", 5,
                                "stockMinimo", 2,
                                "usuarioId", userId.toString()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(12));

        ArgumentCaptor<InventoryQuickActionCommand> captor =
                ArgumentCaptor.forClass(InventoryQuickActionCommand.class);
        verify(adjustInventoryStock).execute(captor.capture());
        assertEquals(buffetId, captor.getValue().buffetId());
        assertEquals(productId, captor.getValue().productId());
        assertEquals(InventoryQuickActionType.ADD_STOCK, captor.getValue().action());
        assertEquals(5, captor.getValue().quantity());
        assertEquals(2, captor.getValue().stockMinimo());
        assertEquals(userId, captor.getValue().usuarioId());
    }

    @Test
    void movements_ShouldReturnStockMovements() throws Exception {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MovimientoStock movement = new MovimientoStock(
                UUID.randomUUID(),
                inventoryId,
                TipoMovimientoStock.AJUSTE,
                5,
                10,
                15,
                "Carga inicial",
                userId,
                null,
                LocalDateTime.of(2026, 6, 8, 10, 0)
        );

        when(getStockMovements.execute(buffetId, productId)).thenReturn(List.of(movement));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/inventory/{buffetId}/products/{productId}/movements",
                        buffetId,
                        productId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inventarioId").value(inventoryId.toString()))
                .andExpect(jsonPath("$[0].tipo").value("AJUSTE"))
                .andExpect(jsonPath("$[0].cantidad").value(5))
                .andExpect(jsonPath("$[0].motivo").value("Carga inicial"))
                .andExpect(jsonPath("$[0].usuarioId").value(userId.toString()));

        verify(getStockMovements).execute(buffetId, productId);
    }

    @Test
    void inventoryOverview_ShouldMapAlternativeModesAndSoldOutStates() throws Exception {
        UUID buffetId = UUID.randomUUID();
        Producto availabilityProduct = product();
        availabilityProduct.setTipoManejoInventario(
                TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE
        );
        availabilityProduct.setEstadoInventario(EstadoInventario.BAJO_STOCK);
        availabilityProduct.setStockDisponible(1);
        Producto defaultModeProduct = product();
        defaultModeProduct.setTipoManejoInventario(null);
        defaultModeProduct.setEstadoInventario(null);
        defaultModeProduct.setStockDisponible(null);
        Producto dailyProduct = product();
        dailyProduct.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        dailyProduct.setCupoMaximoDiario(null);
        dailyProduct.setStockActual(7);
        dailyProduct.setStockDisponible(0);
        Producto disabledProduct = product();
        disabledProduct.setEstadoInventario(EstadoInventario.DESACTIVADO);

        when(listInventoryProducts.execute(buffetId)).thenReturn(List.of(
                availabilityProduct,
                defaultModeProduct,
                dailyProduct,
                disabledProduct
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/inventory/{buffetId}/overview",
                        buffetId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoManejoInventario")
                        .value("DISPONIBLE_NO_DISPONIBLE"))
                .andExpect(jsonPath("$[0].bajoStock").value(true))
                .andExpect(jsonPath("$[1].tipoManejoInventario").value("STOCK_EXACTO"))
                .andExpect(jsonPath("$[2].cupoMaximoDiario").value(7))
                .andExpect(jsonPath("$[2].agotado").value(true))
                .andExpect(jsonPath("$[2].disponible").value(false))
                .andExpect(jsonPath("$[3].agotado").value(true))
                .andExpect(jsonPath("$[3].disponible").value(false));
    }

    @Test
    void adjustStock_WhenInventoryStateIsNull_ShouldReturnNullFields() throws Exception {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Inventario inventory = org.mockito.Mockito.mock(Inventario.class);
        when(inventory.getId()).thenReturn(UUID.randomUUID());
        when(inventory.getBuffetId()).thenReturn(buffetId);
        when(inventory.getProductoId()).thenReturn(productId);
        when(inventory.getStockActual()).thenReturn(12);
        when(inventory.getStockReservado()).thenReturn(2);
        when(inventory.stockMinimoEfectivo()).thenReturn(1);
        when(inventory.stockDisponible()).thenReturn(10);
        when(inventory.estadoActual()).thenReturn(null);
        when(inventory.getTipoManejo()).thenReturn(null);

        when(adjustInventoryStock.execute(any(AdjustInventoryStockCommand.class)))
                .thenReturn(inventory);

        mockMvc.perform(patch(
                        "/api/v1/inventory/{buffetId}/products/{productId}/stock",
                        buffetId,
                        productId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "stockActual", 12,
                                "motivo", "Correccion manual"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoInventario").doesNotExist())
                .andExpect(jsonPath("$.tipoManejoInventario").doesNotExist());
    }

    @Test
    void movements_WhenMovementTypeIsNull_ShouldReturnNullType() throws Exception {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        MovimientoStock movement = new MovimientoStock(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                5,
                10,
                15,
                "Carga inicial",
                UUID.randomUUID(),
                null,
                LocalDateTime.of(2026, 6, 8, 10, 0)
        );

        when(getStockMovements.execute(buffetId, productId)).thenReturn(List.of(movement));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/inventory/{buffetId}/products/{productId}/movements",
                        buffetId,
                        productId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").doesNotExist());
    }

    private Inventario inventory(UUID buffetId, UUID productId) {
        Inventario inventory = new Inventario();
        inventory.setId(UUID.randomUUID());
        inventory.setBuffetId(buffetId);
        inventory.setProductoId(productId);
        inventory.setStockActual(12);
        inventory.setStockReservado(2);
        inventory.setStockMinimo(1);
        inventory.setEstado(EstadoInventario.DISPONIBLE);
        inventory.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        return inventory;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Alfajor");
        product.setDescripcion("Alfajor simple");
        product.setPrecio(new BigDecimal("10.00"));
        product.setStockActual(12);
        product.setStockReservado(2);
        product.setStockDisponible(10);
        product.setEstadoInventario(EstadoInventario.DISPONIBLE);
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        return product;
    }
}
