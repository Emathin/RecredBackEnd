package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateSupplier;
import com.recred.backend.application.CreateSupplierCommand;
import com.recred.backend.application.DeleteSupplier;
import com.recred.backend.application.GetSupplier;
import com.recred.backend.application.ListSuppliers;
import com.recred.backend.application.RecommendSupplierForProducts;
import com.recred.backend.application.UpdatePriceListItemMapping;
import com.recred.backend.application.UpdateSupplier;
import com.recred.backend.application.UpdateSupplierCommand;
import com.recred.backend.application.UploadSupplierPriceList;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.models.RecomendacionProveedor;
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
import java.util.stream.Collectors;

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
        UUID kiosqueroId = getKiosqueroId(authentication);
        Proveedor res = createSupplier.execute(new CreateSupplierCommand(
                kiosqueroId,
                request.nombre(),
                request.telefono(),
                request.email(),
                request.diasVisita(),
                request.notas()
        ));
        return ResponseEntity.ok(SupplierResponse.fromDomain(res));
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponse>> list(Authentication authentication) {
        UUID kiosqueroId = getKiosqueroId(authentication);
        List<SupplierResponse> res = listSuppliers.execute(kiosqueroId).stream()
                .map(SupplierResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> get(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID kiosqueroId = getKiosqueroId(authentication);
        Proveedor res = getSupplier.execute(id, kiosqueroId);
        return ResponseEntity.ok(SupplierResponse.fromDomain(res));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierRequest request,
            Authentication authentication
    ) {
        UUID kiosqueroId = getKiosqueroId(authentication);
        Proveedor res = updateSupplier.execute(new UpdateSupplierCommand(
                id,
                kiosqueroId,
                request.nombre(),
                request.telefono(),
                request.email(),
                request.diasVisita(),
                request.notas()
        ));
        return ResponseEntity.ok(SupplierResponse.fromDomain(res));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID kiosqueroId = getKiosqueroId(authentication);
        deleteSupplier.execute(id, kiosqueroId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/price-lists", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListaPrecioProveedorResponse> uploadPriceList(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        UUID kiosqueroId = getKiosqueroId(authentication);
        ListaPrecioProveedor res = uploadSupplierPriceList.execute(
                kiosqueroId,
                id,
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType()
        );
        return ResponseEntity.ok(ListaPrecioProveedorResponse.fromDomain(res));
    }

    @PatchMapping("/price-list-items/{itemId}/mapping")
    public ResponseEntity<ItemListaPrecioProveedorResponse> updateItemMapping(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemMappingRequest request,
            Authentication authentication
    ) {
        UUID kiosqueroId = getKiosqueroId(authentication);
        ItemListaPrecioProveedor res = updatePriceListItemMapping.execute(
                kiosqueroId,
                itemId,
                request.productoInventarioId()
        );
        return ResponseEntity.ok(ItemListaPrecioProveedorResponse.fromDomain(res));
    }

    @PostMapping("/purchase-recommendations")
    public ResponseEntity<List<RecomendacionProveedor>> getRecommendations(
            @Valid @RequestBody PurchaseRecommendationRequest request,
            Authentication authentication
    ) {
        UUID kiosqueroId = getKiosqueroId(authentication);
        List<RecomendacionProveedor> res = recommendSupplierForProducts.execute(
                kiosqueroId,
                request.productosIds()
        );
        return ResponseEntity.ok(res);
    }

    private UUID getKiosqueroId(Authentication authentication) {
        String subject = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
        Usuario usuario = usuarioRepository.findByIdCognito(subject)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario autenticado no sincronizado"));

        return kiosqueroRepository.findKiosqueroIdByUsuarioId(usuario.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "El usuario autenticado no es un kiosquero registrado"));
    }
}
