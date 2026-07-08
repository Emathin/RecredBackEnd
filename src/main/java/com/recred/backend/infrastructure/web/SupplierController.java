package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateSupplier;
import com.recred.backend.application.CreateSupplierRequest;
import com.recred.backend.application.DeleteSupplier;
import com.recred.backend.application.DeleteSupplierRequest;
import com.recred.backend.application.GetSupplier;
import com.recred.backend.application.GetSupplierRequest;
import com.recred.backend.application.ListSuppliers;
import com.recred.backend.application.ListSuppliersRequest;
import com.recred.backend.application.RecommendSupplierForProducts;
import com.recred.backend.application.RecommendSupplierForProductsRequest;
import com.recred.backend.application.RecommendSupplierForProductsResponse;
import com.recred.backend.application.UpdatePriceListItemMapping;
import com.recred.backend.application.UpdatePriceListItemMappingRequest;
import com.recred.backend.application.UpdateSupplier;
import com.recred.backend.application.UpdateSupplierRequest;
import com.recred.backend.application.UploadSupplierPriceList;
import com.recred.backend.application.UploadSupplierPriceListRequest;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final CreateSupplier createSupplier;
    private final ListSuppliers listSuppliers;
    private final GetSupplier getSupplier;
    private final UpdateSupplier updateSupplier;
    private final DeleteSupplier deleteSupplier;
    private final UploadSupplierPriceList uploadSupplierPriceList;
    private final UpdatePriceListItemMapping updatePriceListItemMapping;
    private final RecommendSupplierForProducts recommendSupplierForProducts;

    private final UsuarioRepository usuarioRepository;
    private final KiosqueroRepository kiosqueroRepository;

    public SupplierController(
            CreateSupplier createSupplier,
            ListSuppliers listSuppliers,
            GetSupplier getSupplier,
            UpdateSupplier updateSupplier,
            DeleteSupplier deleteSupplier,
            UploadSupplierPriceList uploadSupplierPriceList,
            UpdatePriceListItemMapping updatePriceListItemMapping,
            RecommendSupplierForProducts recommendSupplierForProducts,
            UsuarioRepository usuarioRepository,
            KiosqueroRepository kiosqueroRepository
    ) {
        this.createSupplier = createSupplier;
        this.listSuppliers = listSuppliers;
        this.getSupplier = getSupplier;
        this.updateSupplier = updateSupplier;
        this.deleteSupplier = deleteSupplier;
        this.uploadSupplierPriceList = uploadSupplierPriceList;
        this.updatePriceListItemMapping = updatePriceListItemMapping;
        this.recommendSupplierForProducts = recommendSupplierForProducts;
        this.usuarioRepository = usuarioRepository;
        this.kiosqueroRepository = kiosqueroRepository;
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(
            @Valid @RequestBody SupplierRequest request,
            Authentication authentication
    ) {
        UUID kioskOwnerId = getKioskOwnerId(authentication);
        SupplierResponse response = SupplierResponse.fromApplication(
                createSupplier.execute(new CreateSupplierRequest(
                        kioskOwnerId,
                        request.name(),
                        request.phone(),
                        request.email(),
                        request.visitDays(),
                        request.notes()
                ))
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponse>> list(Authentication authentication) {
        UUID kioskOwnerId = getKioskOwnerId(authentication);
        List<SupplierResponse> response = listSuppliers.execute(
                        new ListSuppliersRequest(kioskOwnerId))
                .suppliers()
                .stream()
                .map(SupplierResponse::fromApplication)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> get(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID kioskOwnerId = getKioskOwnerId(authentication);
        SupplierResponse response = SupplierResponse.fromApplication(
                getSupplier.execute(new GetSupplierRequest(id, kioskOwnerId))
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierRequest request,
            Authentication authentication
    ) {
        UUID kioskOwnerId = getKioskOwnerId(authentication);
        SupplierResponse response = SupplierResponse.fromApplication(
                updateSupplier.execute(new UpdateSupplierRequest(
                        id,
                        kioskOwnerId,
                        request.name(),
                        request.phone(),
                        request.email(),
                        request.visitDays(),
                        request.notes()
                ))
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID kioskOwnerId = getKioskOwnerId(authentication);
        deleteSupplier.execute(new DeleteSupplierRequest(id, kioskOwnerId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/price-lists", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListaPrecioProveedorResponse> uploadPriceList(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        UUID kioskOwnerId = getKioskOwnerId(authentication);
        ListaPrecioProveedorResponse response = ListaPrecioProveedorResponse.fromApplication(
                uploadSupplierPriceList.execute(
                        new UploadSupplierPriceListRequest(
                                kioskOwnerId,
                                id,
                                file.getBytes(),
                                file.getOriginalFilename(),
                                file.getContentType()
                        )
                )
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/price-list-items/{itemId}/mapping")
    public ResponseEntity<ItemListaPrecioProveedorResponse> updateItemMapping(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemMappingRequest request,
            Authentication authentication
    ) {
        UUID kioskOwnerId = getKioskOwnerId(authentication);
        ItemListaPrecioProveedorResponse response =
                ItemListaPrecioProveedorResponse.fromApplication(
                        updatePriceListItemMapping.execute(
                                new UpdatePriceListItemMappingRequest(
                                        kioskOwnerId,
                                        itemId,
                                        request.inventoryProductId()
                                )
                        )
                );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/purchase-recommendations")
    public ResponseEntity<List<SupplierRecommendationResponse>> getRecommendations(
            @Valid @RequestBody PurchaseRecommendationRequest request,
            Authentication authentication
    ) {
        UUID kioskOwnerId = getKioskOwnerId(authentication);
        RecommendSupplierForProductsResponse response = recommendSupplierForProducts.execute(
                new RecommendSupplierForProductsRequest(
                        kioskOwnerId,
                        request.productIds()
                )
        );
        return ResponseEntity.ok(response.recommendations()
                .stream()
                .map(SupplierRecommendationResponse::fromApplication)
                .toList());
    }

    private UUID getKioskOwnerId(Authentication authentication) {
        String subject = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Authenticated user is required"));
        Usuario user = usuarioRepository.findByIdCognito(subject)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Authenticated user is not synchronized"));

        return kiosqueroRepository.findKiosqueroIdByUsuarioId(user.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user is not a registered kiosk owner"));
    }
}
