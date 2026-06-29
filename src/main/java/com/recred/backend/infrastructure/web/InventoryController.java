package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AdjustInventoryStock;
import com.recred.backend.application.AdjustInventoryStockRequest;
import com.recred.backend.application.AdjustInventoryStockResponse;
import com.recred.backend.application.ApplyInventoryQuickActionRequest;
import com.recred.backend.application.GetStockMovements;
import com.recred.backend.application.GetStockMovementsRequest;
import com.recred.backend.application.GetStockMovementsResponse.StockMovementResponse;
import com.recred.backend.application.InventoryOverviewResponse;
import com.recred.backend.application.ListInventoryProducts;
import com.recred.backend.application.ListInventoryProductsRequest;
import com.recred.backend.application.ListInventoryProductsResponse;
import com.recred.backend.application.UploadProductImage;
import com.recred.backend.application.UploadProductImageRequest;
import com.recred.backend.application.UploadProductImageResponse;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.TipoManejoInventario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
        ListInventoryProductsResponse response = listInventoryProducts.execute(
                new ListInventoryProductsRequest(buffetId));
        List<ProductResponseDTO> inventory = response.products().stream()
                .map(ProductResponseDTO::fromInventoryProduct)
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
                .execute(new ListInventoryProductsRequest(buffetId))
                .products()
                .stream()
                .map(InventoryOverviewResponse::fromProduct)
                .toList();
        return ResponseEntity.ok(inventory);
    }

    @PatchMapping("/{buffetId}/products/{productId}/stock")
    public ResponseEntity<AdjustInventoryStockResponse> adjustStock(
            @PathVariable UUID buffetId,
            @PathVariable UUID productId,
            @Valid @RequestBody AdjustInventoryStockPayload request,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(adjustInventoryStock.execute(new AdjustInventoryStockRequest(
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
        )));
    }

    @PatchMapping("/{buffetId}/products/{productId}/quick-action")
    public ResponseEntity<AdjustInventoryStockResponse> quickAction(
            @PathVariable UUID buffetId,
            @PathVariable UUID productId,
            @Valid @RequestBody QuickInventoryActionPayload request,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(adjustInventoryStock.execute(
                new ApplyInventoryQuickActionRequest(
                        buffetId,
                        productId,
                        request.action(),
                        request.quantity(),
                        request.stockMinimo(),
                        request.motivo(),
                        request.usuarioId()
                )));
    }

    @GetMapping("/{buffetId}/products/{productId}/movements")
    public ResponseEntity<List<StockMovementResponse>> movements(
            @PathVariable UUID buffetId,
            @PathVariable UUID productId,
            Authentication authentication
    ) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return ResponseEntity.ok(getStockMovements.execute(
                new GetStockMovementsRequest(buffetId, productId)).movements());
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
        try {
            UploadProductImageResponse response = uploadProductImage.execute(
                    new UploadProductImageRequest(
                            buffetId,
                            productId,
                            imagen.getBytes(),
                            imagen.getOriginalFilename()
                    )
            );
            return ResponseEntity.ok(new ImagenUploadResponse(response.urlImagen()));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error al leer la imagen", e);
        }
    }

    public record ImagenUploadResponse(String urlImagen) {}

    public record AdjustInventoryStockPayload(
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

    public record QuickInventoryActionPayload(
            @NotNull InventoryQuickActionType action,
            @PositiveOrZero Integer quantity,
            @PositiveOrZero Integer stockMinimo,
            String motivo,
            UUID usuarioId
    ) {
    }
}
