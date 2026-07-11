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
    
    private final UUID kioskOwnerId = randomUUID();
    private final UUID supplierId = randomUUID();
    private final UUID priceListId = randomUUID();
    private final UUID itemId = randomUUID();
    private final UUID inventoryProductId = randomUUID();
    private final UUID productId = randomUUID();
    private final UUID userId = randomUUID();

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
    void givenValidRequestWhenCreatingThenResolvesKioskOwnerAndMapsResponse() {
        SupplierRequest request = supplierRequest("Proveedor Uno");
        Authentication authentication = authenticatedKiosquero();
        CreateSupplierRequest useCaseRequest = givenCreateRequest();
        when(createSupplier.execute(useCaseRequest))
                .thenReturn(supplierDetails("Proveedor Uno"));

        ResponseEntity<SupplierResponse> response = controller.create(request, authentication);

        thenCreateResponseIsMapped(response, useCaseRequest);
    }

    @Test
    void givenValidRequestWhenListingThenResolvesKioskOwnerAndMapsResponse() {
        Authentication authentication = authenticatedKiosquero();
        ListSuppliersRequest request = new ListSuppliersRequest(kioskOwnerId);
        when(listSuppliers.execute(request)).thenReturn(new ListSuppliersResponse(List.of(
                supplierDetails("Proveedor Uno")
        )));

        ResponseEntity<List<SupplierResponse>> response = controller.list(authentication);

        thenListResponseIsMapped(response, request);
    }

    @Test
    void givenSupplierIdWhenGettingThenResolvesKioskOwnerAndDelegates() {
        Authentication authentication = authenticatedKiosquero();
        GetSupplierRequest request = new GetSupplierRequest(supplierId, kioskOwnerId);
        when(getSupplier.execute(request))
                .thenReturn(supplierDetails("Proveedor Uno"));

        ResponseEntity<SupplierResponse> response = controller.get(supplierId, authentication);

        thenGetResponseIsMapped(response, request);
    }

    @Test
    void givenSupplierIdAndDataWhenUpdatingThenResolvesKioskOwnerAndDelegates() {
        SupplierRequest request = supplierRequest("Proveedor Actualizado");
        Authentication authentication = authenticatedKiosquero();
        UpdateSupplierRequest useCaseRequest = givenUpdateRequest();
        when(updateSupplier.execute(useCaseRequest))
                .thenReturn(supplierDetails("Proveedor Actualizado"));

        ResponseEntity<SupplierResponse> response =
                controller.update(supplierId, request, authentication);

        thenUpdateResponseIsMapped(response, useCaseRequest);
    }

    @Test
    void givenSupplierIdWhenDeletingThenResolvesKioskOwnerAndReturnsNoContent() {
        Authentication authentication = authenticatedKiosquero();

        ResponseEntity<Void> response = controller.delete(supplierId, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteSupplier).execute(new DeleteSupplierRequest(supplierId, kioskOwnerId));
    }

    @Test
    void givenPriceListFileWhenUploadingThenSendsDataAndMapsResponse() throws Exception {
        byte[] fileContent = "producto,precio".getBytes();
        Authentication authentication = authenticatedKiosquero();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lista.csv",
                "text/csv",
                fileContent
        );
        when(uploadSupplierPriceList.execute(any(UploadSupplierPriceListRequest.class)))
                .thenReturn(priceList());

        ResponseEntity<ListaPrecioProveedorResponse> response =
                controller.uploadPriceList(supplierId, file, authentication);

        thenUploadResponseIsMapped(response, fileContent);
    }

    @Test
    void givenMappingDataWhenUpdatingItemMappingThenResolvesKioskOwnerAndMapsResponse() {
        Authentication authentication = authenticatedKiosquero();
        UpdatePriceListItemMappingRequest request = new UpdatePriceListItemMappingRequest(
                kioskOwnerId,
                itemId,
                inventoryProductId
        );
        when(updatePriceListItemMapping.execute(request))
                .thenReturn(itemMapping());

        ResponseEntity<ItemListaPrecioProveedorResponse> response =
                controller.updateItemMapping(
                        itemId,
                        new UpdateItemMappingRequest(inventoryProductId),
                        authentication
                );

        thenItemMappingIsUpdated(response, request);
    }

    @Test
    void givenProductsWhenGettingRecommendationsThenResolvesKioskOwnerAndMapsResponse() {
        Authentication authentication = authenticatedKiosquero();
        RecommendSupplierForProductsRequest request = new RecommendSupplierForProductsRequest(
                kioskOwnerId,
                List.of(productId)
        );
        when(recommendSupplierForProducts.execute(request))
                .thenReturn(recommendation());

        ResponseEntity<List<SupplierRecommendationResponse>> response =
                controller.getRecommendations(
                        new PurchaseRecommendationRequest(List.of(productId)),
                        authentication
                );

        thenRecommendationsAreMapped(response, request);
    }

    @Test
    void givenNoAuthenticationWhenListingThenThrowsAuthenticationException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.list(null));
    }

    @Test
    void givenUnsynchronizedUserWhenListingThenThrowsAuthenticationException() {
        Authentication authentication = authentication();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.list(authentication));
    }

    @Test
    void givenUserNotKioskOwnerWhenListingThenThrowsAccessDeniedException() {
        Authentication authentication = authentication();
        Usuario user = user();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(userId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> controller.list(authentication));
    }

    private Authentication authenticatedKiosquero() {
        Authentication authentication = authentication();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.of(user()));
        when(kiosqueroRepository.findKiosqueroIdByUsuarioId(userId))
                .thenReturn(Optional.of(kioskOwnerId));
        return authentication;
    }

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(COGNITO_SUBJECT);
        return authentication;
    }

    private Usuario user() {
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

    private SupplierDetailsResponse supplierDetails(String name) {
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
                List.of(priceList())
        );
    }

    private UploadSupplierPriceListResponse priceList() {
        return new UploadSupplierPriceListResponse(
                priceListId,
                supplierId,
                "https://storage.test/lista.csv",
                "lista.csv",
                true,
                LocalDateTime.of(2026, 7, 2, 10, 0),
                List.of(priceListItem())
        );
    }

    private UploadSupplierPriceListResponse.Item priceListItem() {
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

    private UpdatePriceListItemMappingResponse itemMapping() {
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

    private RecommendSupplierForProductsResponse recommendation() {
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

    private CreateSupplierRequest givenCreateRequest() {
        return new CreateSupplierRequest(
                kioskOwnerId,
                "Proveedor Uno",
                "11223344",
                "proveedor@test.com",
                "Lunes",
                "Entrega temprano"
        );
    }

    private UpdateSupplierRequest givenUpdateRequest() {
        return new UpdateSupplierRequest(
                supplierId,
                kioskOwnerId,
                "Proveedor Actualizado",
                "11223344",
                "proveedor@test.com",
                "Lunes",
                "Entrega temprano"
        );
    }

    private void thenCreateResponseIsMapped(
            ResponseEntity<SupplierResponse> response, CreateSupplierRequest useCaseRequest) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(supplierId, response.getBody().id());
        assertEquals("Proveedor Uno", response.getBody().nombre());
        assertEquals(1, response.getBody().listasPrecios().size());
        verify(createSupplier).execute(useCaseRequest);
    }

    private void thenListResponseIsMapped(
            ResponseEntity<List<SupplierResponse>> response, ListSuppliersRequest request) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(supplierId, response.getBody().get(0).id());
        verify(listSuppliers).execute(request);
    }

    private void thenGetResponseIsMapped(
            ResponseEntity<SupplierResponse> response, GetSupplierRequest request) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(supplierId, response.getBody().id());
        verify(getSupplier).execute(request);
    }

    private void thenUpdateResponseIsMapped(
            ResponseEntity<SupplierResponse> response, UpdateSupplierRequest useCaseRequest) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Proveedor Actualizado", response.getBody().nombre());
        verify(updateSupplier).execute(useCaseRequest);
    }

    private void thenUploadResponseIsMapped(
            ResponseEntity<ListaPrecioProveedorResponse> response, byte[] fileContent) {
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

    private void thenItemMappingIsUpdated(
            ResponseEntity<ItemListaPrecioProveedorResponse> response, UpdatePriceListItemMappingRequest request) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(itemId, response.getBody().id());
        assertEquals(inventoryProductId, response.getBody().productoInventarioId());
        verify(updatePriceListItemMapping).execute(request);
    }

    private void thenRecommendationsAreMapped(
            ResponseEntity<List<SupplierRecommendationResponse>> response, RecommendSupplierForProductsRequest request) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(productId, response.getBody().get(0).inventoryProductId());
        assertEquals(supplierId, response.getBody().get(0).recommendedSupplierId());
        verify(recommendSupplierForProducts).execute(request);
    }
}
