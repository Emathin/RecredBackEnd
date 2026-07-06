package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreateSupplier;
import com.recred.backend.application.CreateSupplierRequest;
import com.recred.backend.application.DeleteSupplier;
import com.recred.backend.application.DeleteSupplierRequest;
import com.recred.backend.application.GetSupplier;
import com.recred.backend.application.GetSupplierRequest;
import com.recred.backend.application.ListSuppliers;
import com.recred.backend.application.ListSuppliersRequest;
import com.recred.backend.application.ListSuppliersResponse;
import com.recred.backend.application.RecommendSupplierForProducts;
import com.recred.backend.application.RecommendSupplierForProductsRequest;
import com.recred.backend.application.RecommendSupplierForProductsResponse;
import com.recred.backend.application.SupplierDetailsResponse;
import com.recred.backend.application.UpdatePriceListItemMapping;
import com.recred.backend.application.UpdatePriceListItemMappingRequest;
import com.recred.backend.application.UpdatePriceListItemMappingResponse;
import com.recred.backend.application.UpdateSupplier;
import com.recred.backend.application.UpdateSupplierRequest;
import com.recred.backend.application.UploadSupplierPriceList;
import com.recred.backend.application.UploadSupplierPriceListRequest;
import com.recred.backend.application.UploadSupplierPriceListResponse;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.web.ItemListaPrecioProveedorResponse;
import com.recred.backend.infrastructure.web.ListaPrecioProveedorResponse;
import com.recred.backend.infrastructure.web.PurchaseRecommendationRequest;
import com.recred.backend.infrastructure.web.SupplierController;
import com.recred.backend.infrastructure.web.SupplierRecommendationResponse;
import com.recred.backend.infrastructure.web.SupplierRequest;
import com.recred.backend.infrastructure.web.SupplierResponse;
import com.recred.backend.infrastructure.web.UpdateItemMappingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

    private static final String COGNITO_SUBJECT = "kiosk-owner-subject";

    @Mock
    private CreateSupplier createSupplier;
    @Mock
    private ListSuppliers listSuppliers;
    @Mock
    private GetSupplier getSupplier;
    @Mock
    private UpdateSupplier updateSupplier;
    @Mock
    private DeleteSupplier deleteSupplier;
    @Mock
    private UploadSupplierPriceList uploadSupplierPriceList;
    @Mock
    private UpdatePriceListItemMapping updatePriceListItemMapping;
    @Mock
    private RecommendSupplierForProducts recommendSupplierForProducts;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private KiosqueroRepository kiosqueroRepository;

    private SupplierController controller;

    @BeforeEach
    void setUp() {
        controller = new SupplierController(
                createSupplier,
                listSuppliers,
                getSupplier,
                updateSupplier,
                deleteSupplier,
                uploadSupplierPriceList,
                updatePriceListItemMapping,
                recommendSupplierForProducts,
                usuarioRepository,
                kiosqueroRepository
        );
    }

    @Test
    void createShouldResolveKioskOwnerAndMapResponse() {
        UUID kioskOwnerId = randomUUID();
        UUID supplierId = randomUUID();
        SupplierRequest request = supplierRequest("Proveedor Uno");
        Authentication authentication = authenticatedKiosquero(kioskOwnerId);
        CreateSupplierRequest useCaseRequest = new CreateSupplierRequest(
                kioskOwnerId,
                "Proveedor Uno",
                "11223344",
                "proveedor@test.com",
                "Lunes",
                "Entrega temprano"
        );
        when(createSupplier.execute(useCaseRequest))
                .thenReturn(supplierDetails(supplierId, kioskOwnerId, "Proveedor Uno"));

        ResponseEntity<SupplierResponse> response = controller.create(request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(supplierId, response.getBody().id());
        assertEquals("Proveedor Uno", response.getBody().nombre());
        assertEquals(1, response.getBody().listasPrecios().size());
        verify(createSupplier).execute(useCaseRequest);
    }

    @Test
    void listShouldResolveKioskOwnerAndMapResponse() {
        UUID kioskOwnerId = randomUUID();
        UUID supplierId = randomUUID();
        Authentication authentication = authenticatedKiosquero(kioskOwnerId);
        ListSuppliersRequest request = new ListSuppliersRequest(kioskOwnerId);
        when(listSuppliers.execute(request)).thenReturn(new ListSuppliersResponse(List.of(
                supplierDetails(supplierId, kioskOwnerId, "Proveedor Uno")
        )));

        ResponseEntity<List<SupplierResponse>> response = controller.list(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(supplierId, response.getBody().get(0).id());
        verify(listSuppliers).execute(request);
    }

    @Test
    void getShouldResolveKioskOwnerAndDelegateWithSupplierId() {
        UUID kioskOwnerId = randomUUID();
        UUID supplierId = randomUUID();
        Authentication authentication = authenticatedKiosquero(kioskOwnerId);
        GetSupplierRequest request = new GetSupplierRequest(supplierId, kioskOwnerId);
        when(getSupplier.execute(request))
                .thenReturn(supplierDetails(supplierId, kioskOwnerId, "Proveedor Uno"));

        ResponseEntity<SupplierResponse> response = controller.get(supplierId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(supplierId, response.getBody().id());
        verify(getSupplier).execute(request);
    }

    @Test
    void updateShouldResolveKioskOwnerAndDelegateWithRequestData() {
        UUID kioskOwnerId = randomUUID();
        UUID supplierId = randomUUID();
        SupplierRequest request = supplierRequest("Proveedor Actualizado");
        Authentication authentication = authenticatedKiosquero(kioskOwnerId);
        UpdateSupplierRequest useCaseRequest = new UpdateSupplierRequest(
                supplierId,
                kioskOwnerId,
                "Proveedor Actualizado",
                "11223344",
                "proveedor@test.com",
                "Lunes",
                "Entrega temprano"
        );
        when(updateSupplier.execute(useCaseRequest))
                .thenReturn(supplierDetails(supplierId, kioskOwnerId, "Proveedor Actualizado"));

        ResponseEntity<SupplierResponse> response =
                controller.update(supplierId, request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Proveedor Actualizado", response.getBody().nombre());
        verify(updateSupplier).execute(useCaseRequest);
    }

    @Test
    void deleteShouldResolveKioskOwnerAndReturnNoContent() {
        UUID kioskOwnerId = randomUUID();
        UUID supplierId = randomUUID();
        Authentication authentication = authenticatedKiosquero(kioskOwnerId);

        ResponseEntity<Void> response = controller.delete(supplierId, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteSupplier).execute(new DeleteSupplierRequest(supplierId, kioskOwnerId));
    }

    @Test
    void uploadPriceListShouldSendFileDataAndMapResponse() throws Exception {
        UUID kioskOwnerId = randomUUID();
        UUID supplierId = randomUUID();
        UUID priceListId = randomUUID();
        byte[] fileContent = "producto,precio".getBytes();
        Authentication authentication = authenticatedKiosquero(kioskOwnerId);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lista.csv",
                "text/csv",
                fileContent
        );
        when(uploadSupplierPriceList.execute(any(UploadSupplierPriceListRequest.class)))
                .thenReturn(priceList(priceListId, supplierId));

        ResponseEntity<ListaPrecioProveedorResponse> response =
                controller.uploadPriceList(supplierId, file, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(priceListId, response.getBody().id());
        assertEquals(1, response.getBody().items().size());

        ArgumentCaptor<UploadSupplierPriceListRequest> captor =
                ArgumentCaptor.forClass(UploadSupplierPriceListRequest.class);
        verify(uploadSupplierPriceList).execute(captor.capture());
        assertEquals(kioskOwnerId, captor.getValue().kioskOwnerId());
        assertEquals(supplierId, captor.getValue().supplierId());
        assertArrayEquals(fileContent, captor.getValue().fileContent());
        assertEquals("lista.csv", captor.getValue().originalFilename());
        assertEquals("text/csv", captor.getValue().contentType());
    }

    @Test
    void updateItemMappingShouldResolveKioskOwnerAndMapResponse() {
        UUID kioskOwnerId = randomUUID();
        UUID itemId = randomUUID();
        UUID inventoryProductId = randomUUID();
        Authentication authentication = authenticatedKiosquero(kioskOwnerId);
        UpdatePriceListItemMappingRequest request = new UpdatePriceListItemMappingRequest(
                kioskOwnerId,
                itemId,
                inventoryProductId
        );
        when(updatePriceListItemMapping.execute(request))
                .thenReturn(itemMapping(itemId, inventoryProductId));

        ResponseEntity<ItemListaPrecioProveedorResponse> response =
                controller.updateItemMapping(
                        itemId,
                        new UpdateItemMappingRequest(inventoryProductId),
                        authentication
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(itemId, response.getBody().id());
        assertEquals(inventoryProductId, response.getBody().productoInventarioId());
        verify(updatePriceListItemMapping).execute(request);
    }

    @Test
    void recommendationsShouldResolveKioskOwnerAndMapResponse() {
        UUID kioskOwnerId = randomUUID();
        UUID productId = randomUUID();
        UUID supplierId = randomUUID();
        Authentication authentication = authenticatedKiosquero(kioskOwnerId);
        RecommendSupplierForProductsRequest request = new RecommendSupplierForProductsRequest(
                kioskOwnerId,
                List.of(productId)
        );
        when(recommendSupplierForProducts.execute(request))
                .thenReturn(recommendation(productId, supplierId));

        ResponseEntity<List<SupplierRecommendationResponse>> response =
                controller.getRecommendations(
                        new PurchaseRecommendationRequest(List.of(productId)),
                        authentication
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(productId, response.getBody().get(0).inventoryProductId());
        assertEquals(supplierId, response.getBody().get(0).recommendedSupplierId());
        verify(recommendSupplierForProducts).execute(request);
    }

    @Test
    void listWithoutAuthenticationShouldThrowAuthenticationException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.list(null));
    }

    @Test
    void listWhenUserIsNotSynchronizedShouldThrowAuthenticationException() {
        Authentication authentication = authentication();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.list(authentication));
    }

    @Test
    void listWhenUserIsNotKioskOwnerShouldThrowAccessDeniedException() {
        UUID userId = randomUUID();
        Authentication authentication = authentication();
        Usuario user = user(userId);
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(userId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> controller.list(authentication));
    }

    private Authentication authenticatedKiosquero(UUID kioskOwnerId) {
        UUID userId = randomUUID();
        Authentication authentication = authentication();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.of(user(userId)));
        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(userId))
                .thenReturn(Optional.of(kioskOwnerId));
        return authentication;
    }

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(COGNITO_SUBJECT);
        return authentication;
    }

    private Usuario user(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setIdCognito(COGNITO_SUBJECT);
        return user;
    }

    private SupplierRequest supplierRequest(String name) {
        return new SupplierRequest(
                name,
                "11223344",
                "proveedor@test.com",
                "Lunes",
                "Entrega temprano"
        );
    }

    private SupplierDetailsResponse supplierDetails(
            UUID supplierId,
            UUID kioskOwnerId,
            String name
    ) {
        return new SupplierDetailsResponse(
                supplierId,
                kioskOwnerId,
                name,
                "11223344",
                "proveedor@test.com",
                "Lunes",
                "Entrega temprano",
                LocalDateTime.of(2026, 7, 2, 10, 0),
                LocalDateTime.of(2026, 7, 2, 11, 0),
                List.of(priceList(randomUUID(), supplierId))
        );
    }

    private UploadSupplierPriceListResponse priceList(UUID priceListId, UUID supplierId) {
        return new UploadSupplierPriceListResponse(
                priceListId,
                supplierId,
                "https://storage.test/lista.csv",
                "lista.csv",
                true,
                LocalDateTime.of(2026, 7, 2, 10, 0),
                List.of(priceListItem(randomUUID(), priceListId))
        );
    }

    private UploadSupplierPriceListResponse.Item priceListItem(UUID itemId, UUID priceListId) {
        return new UploadSupplierPriceListResponse.Item(
                itemId,
                priceListId,
                "Leche proveedor",
                randomUUID(),
                "Leche",
                new BigDecimal("1200.00"),
                "12 u",
                "Caja",
                true
        );
    }

    private UpdatePriceListItemMappingResponse itemMapping(
            UUID itemId,
            UUID inventoryProductId
    ) {
        return new UpdatePriceListItemMappingResponse(
                itemId,
                randomUUID(),
                "Leche proveedor",
                inventoryProductId,
                "Leche",
                new BigDecimal("1200.00"),
                "12 u",
                "Caja",
                true
        );
    }

    private RecommendSupplierForProductsResponse recommendation(
            UUID productId,
            UUID supplierId
    ) {
        return new RecommendSupplierForProductsResponse(List.of(
                new RecommendSupplierForProductsResponse.Recommendation(
                        productId,
                        "Leche",
                        supplierId,
                        "Proveedor Uno",
                        new BigDecimal("1200.00"),
                        "12 u",
                        new BigDecimal("100.00"),
                        List.of(new RecommendSupplierForProductsResponse.Alternative(
                                randomUUID(),
                                "Proveedor Dos",
                                new BigDecimal("1320.00"),
                                "12 u",
                                new BigDecimal("110.00")
                        ))
                )
        ));
    }
}
