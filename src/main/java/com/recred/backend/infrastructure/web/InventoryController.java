package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AdjustInventoryStock;
import com.recred.backend.application.AdjustInventoryStockCommand;
import com.recred.backend.application.GetStockMovements;
import com.recred.backend.application.InventoryQuickActionCommand;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.UploadProductImage;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final AdjustInventoryStock adjustInventoryStock;
    private final ListInventoryProducts listInventoryProducts;
    private final GetStockMovements getStockMovements;
    private final KiosqueroAccessGuard kiosqueroAccessGuard;
    private final UploadProductImage uploadProductImage;

    public InventoryController(
            AdjustInventoryStock adjustInventoryStock,
            ListInventoryProducts listInventoryProducts,
            GetStockMovements getStockMovements,
            KiosqueroAccessGuard kiosqueroAccessGuard,
            UploadProductImage uploadProductImage
    ) {
        this.adjustInventoryStock = adjustInventoryStock;
        this.listInventoryProducts = listInventoryProducts;
        this.getStockMovements = getStockMovements;
        this.kiosqueroAccessGuard = kiosqueroAccessGuard;
        this.uploadProductImage = uploadProductImage;
    }

    @GetMapping("/{buffetId}")
    public ResponseEntity<List<ProductResponseDTO>> listInventory(
            @PathVariable UUID buffetId,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        List<ProductResponseDTO> inventory = listInventoryProducts.execute(buffetId).stream()
                .map(ProductResponseDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/{buffetId}/overview")
    public ResponseEntity<List<InventoryOverviewResponse>> inventoryOverview(
            @PathVariable UUID buffetId,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        List<InventoryOverviewResponse> inventory = listInventoryProducts
                .execute(buffetId)
                .stream()
                .map(InventoryOverviewResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(inventory);
    }

    @PatchMapping("/{buffetId}/products/{productId}/stock")
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable UUID buffetId,
            @PathVariable UUID productId,
            @Valid @RequestBody AdjustInventoryStockRequest request,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        Inventario inventory = adjustInventoryStock.execute(
                new AdjustInventoryStockCommand(
                        buffetId,
                        productId,
                        request.stockActual(),
                        request.stockMinimo(),
                        request.estadoInventario(),
                        request.tipoManejoInventario(),
                        request.disponible(),
                        request.cupoMaximoDiario(),
                        request.motivo(),
                        request.usuarioId()
                ));
        return ResponseEntity.ok(InventoryResponse.fromDomain(inventory));
    }

    @PatchMapping("/{buffetId}/products/{productId}/quick-action")
    public ResponseEntity<InventoryResponse> quickAction(
            @PathVariable UUID buffetId,
            @PathVariable UUID productId,
            @Valid @RequestBody QuickInventoryActionRequest request,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        Inventario inventory = adjustInventoryStock.execute(
                new InventoryQuickActionCommand(
                        buffetId,
                        productId,
                        request.action(),
                        request.quantity(),
                        request.stockMinimo(),
                        request.motivo(),
                        request.usuarioId()
                ));
        return ResponseEntity.ok(InventoryResponse.fromDomain(inventory));
    }

    @GetMapping("/{buffetId}/products/{productId}/movements")
    public ResponseEntity<List<StockMovementResponse>> movements(
            @PathVariable UUID buffetId,
            @PathVariable UUID productId,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        List<StockMovementResponse> movements = getStockMovements.execute(buffetId, productId)
                .stream()
                .map(StockMovementResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(movements);
    }

    @PostMapping(
            value = "/{buffetId}/products/{productId}/imagen",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ImagenUploadResponse> subirImagen(
            @PathVariable UUID buffetId,
            @PathVariable UUID productId,
            @RequestParam("imagen") MultipartFile imagen,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        String url = uploadProductImage.execute(buffetId, productId, imagen);
        return ResponseEntity.ok(new ImagenUploadResponse(url));
    }

    public record ImagenUploadResponse(String urlImagen) {}

    public record AdjustInventoryStockRequest(
            @PositiveOrZero Integer stockActual,
            @PositiveOrZero Integer stockMinimo,
            EstadoInventario estadoInventario,
            TipoManejoInventario tipoManejoInventario,
            Boolean disponible,
            @PositiveOrZero Integer cupoMaximoDiario,
            @NotBlank String motivo,
            UUID usuarioId
    ) {
    }

    public record QuickInventoryActionRequest(
            @NotNull InventoryQuickActionType action,
            @PositiveOrZero Integer quantity,
            @PositiveOrZero Integer stockMinimo,
            String motivo,
            UUID usuarioId
    ) {
    }

    public record InventoryOverviewResponse(
            UUID productId,
            String nombre,
            BigDecimal precio,
            String tipoManejoInventario,
            String estadoInventario,
            Integer stockActual,
            Integer stockReservado,
            Integer stockDisponible,
            Integer stockMinimo,
            Integer cupoMaximoDiario,
            Integer cupoDisponibleDia,
            String urlImagen,
            Boolean disponible,
            Boolean bajoStock,
            Boolean agotado
    ) {
        static InventoryOverviewResponse fromDomain(Producto product) {
            String mode = simpleMode(product);
            Integer cupoMaximoDiario = "CUPO_DIARIO".equals(mode)
                    ? dailyCapacity(product)
                    : null;
            Integer cupoDisponibleDia = "CUPO_DIARIO".equals(mode)
                    ? product.getStockDisponible()
                    : null;
            boolean soldOut = isSoldOut(product);
            return new InventoryOverviewResponse(
                    product.getId(),
                    product.getNombre(),
                    product.getPrecio(),
                    mode,
                    product.getEstadoInventario() == null
                            ? null
                            : product.getEstadoInventario().name(),
                    product.getStockActual(),
                    product.getStockReservado(),
                    product.getStockDisponible(),
                    product.getStockMinimo(),
                    cupoMaximoDiario,
                    cupoDisponibleDia,
                    product.getUrlImagen(),
                    !soldOut && product.getEstadoInventario() != EstadoInventario.DESACTIVADO,
                    product.getEstadoInventario() == EstadoInventario.BAJO_STOCK,
                    soldOut
            );
        }

        private static String simpleMode(Producto product) {
            if (product.getTipoManejoInventario() == TipoManejoInventario.CUPO_DIARIO) {
                return "CUPO_DIARIO";
            }

            if (product.getTipoManejoInventario()
                    == TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE) {
                return "DISPONIBLE_NO_DISPONIBLE";
            }

            return "STOCK_EXACTO";
        }

        private static Integer dailyCapacity(Producto product) {
            return product.getCupoMaximoDiario() == null
                    ? product.getStockActual()
                    : product.getCupoMaximoDiario();
        }

        private static boolean isSoldOut(Producto product) {
            if (product.getEstadoInventario() == EstadoInventario.SIN_STOCK
                    || product.getEstadoInventario() == EstadoInventario.DESACTIVADO) {
                return true;
            }

            return product.getStockDisponible() != null && product.getStockDisponible() <= 0;
        }
    }

    public record InventoryResponse(
            UUID id,
            UUID buffetId,
            UUID productoId,
            Integer stockActual,
            Integer stockReservado,
            Integer stockMinimo,
            Integer stockDisponible,
            String estadoInventario,
            String tipoManejoInventario,
            Integer cupoMaximoDiario,
            String urlImagen
    ) {
        static InventoryResponse fromDomain(Inventario inventory) {
            EstadoInventario estado = inventory.estadoActual();
            return new InventoryResponse(
                    inventory.getId(),
                    inventory.getBuffetId(),
                    inventory.getProductoId(),
                    inventory.getStockActual(),
                    inventory.getStockReservado(),
                    inventory.stockMinimoEfectivo(),
                    inventory.stockDisponible(),
                    estado == null ? null : estado.name(),
                    inventory.getTipoManejo() == null
                            ? null
                            : inventory.getTipoManejo().name(),
                    inventory.getCupoMaximoDiario(),
                    inventory.getUrlImagen()
            );
        }
    }

    public record StockMovementResponse(
            UUID id,
            UUID inventarioId,
            String tipo,
            Integer cantidad,
            Integer cantidadAnterior,
            Integer cantidadNueva,
            String motivo,
            UUID usuarioId,
            UUID compraId,
            LocalDateTime creadoEn
    ) {
        static StockMovementResponse fromDomain(MovimientoStock movement) {
            return new StockMovementResponse(
                    movement.id(),
                    movement.inventarioId(),
                    movement.tipo() == null ? null : movement.tipo().name(),
                    movement.cantidad(),
                    movement.cantidadAnterior(),
                    movement.cantidadNueva(),
                    movement.motivo(),
                    movement.usuarioId(),
                    movement.compraId(),
                    movement.creadoEn()
            );
        }
    }
}
