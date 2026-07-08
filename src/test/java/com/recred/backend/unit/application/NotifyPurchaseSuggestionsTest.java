package com.recred.backend.unit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.NotifyPurchaseSuggestions;
import com.recred.backend.application.NotifyPurchaseSuggestionsRequest;
import com.recred.backend.application.NotifyPurchaseSuggestionsResponse;
import com.recred.backend.application.PurchaseSuggestIA;
import com.recred.backend.application.PurchaseSuggestIARequest;
import com.recred.backend.application.PurchaseSuggestionResponse;
import com.recred.backend.application.PushNotificationResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.UsuarioService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotifyPurchaseSuggestionsTest {

    private final PurchaseSuggestIA purchaseSuggestIA = mock(PurchaseSuggestIA.class);
    private final PushNotificationSender pushNotificationSender =
            mock(PushNotificationSender.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private NotifyPurchaseSuggestions useCase = new NotifyPurchaseSuggestions(
            purchaseSuggestIA,
            pushNotificationSender,
            usuarioService,
            objectMapper
    );

    private final String cognitoId = "cognito-id-test";
    private UUID userId;
    private UUID studentId;
    private UUID suggestionId;
    private SugerenciaIA suggestion;
    private Usuario user;
    private NotifyPurchaseSuggestionsResponse response;
    private PushNotification sentNotification;

    @Test
    void execute_WhenSuggestionExists_SendsDataNotification() {
        givenStudentUser();
        givenSuggestionWithoutProduct();
        givenPurchaseSuggestionExists();

        whenNotifyingPurchaseSuggestions();

        thenNotificationWasSent();
        thenNotificationHasSuggestionMetadata();
    }

    @Test
    void execute_WhenSuggestionDoesNotExist_ReturnsWithoutNotification() {
        givenPurchaseSuggestionDoesNotExist();

        whenNotifyingPurchaseSuggestions();

        thenResponseHasNoNotification();
        thenUserWasNotLoaded();
        thenNotificationWasNotSent();
    }

    @Test
    void execute_WhenUserHasNoRole_UsesPendingRole() {
        givenUserWithoutRole();
        givenSuggestionWithoutProduct();
        givenPurchaseSuggestionExists();

        whenNotifyingPurchaseSuggestions();

        thenResponseNotificationHasRole("PENDIENTE");
        thenNotificationWasSent();
    }

    @Test
    void execute_WhenSuggestionHasProduct_SerializesProductInData() throws Exception {
        givenStudentUser();
        givenSuggestionWithProduct(healthyProduct());
        givenPurchaseSuggestionExists();

        whenNotifyingPurchaseSuggestions();

        thenNotificationWasSent();
        thenSerializedProductHasHealthyProductData();
    }

    @Test
    void execute_WhenProductSerializationFails_UsesEmptyProduct() throws Exception {
        givenUseCaseWithFailingProductSerialization();
        givenStudentUser();
        givenSuggestionWithProduct(healthyProduct());
        givenPurchaseSuggestionExists();

        whenNotifyingPurchaseSuggestions();

        thenNotificationWasSent();
        thenProductDataIs("{}");
    }

    @Test
    void execute_WhenProductHasNoCategoryOrClassifications_UsesProductFallbacks()
            throws Exception {
        givenParentUser();
        givenSuggestionWithProduct(productWithoutCategoryOrClassifications());
        givenPurchaseSuggestionExists();

        whenNotifyingPurchaseSuggestions();

        thenNotificationWasSent();
        thenSerializedProductHasFallbacks();
    }

    private void givenStudentUser() {
        givenUserWithRole(RolUsuario.ALUMNO);
    }

    private void givenParentUser() {
        givenUserWithRole(RolUsuario.PADRE);
    }

    private void givenUserWithoutRole() {
        userId = randomUUID();
        user = new Usuario();
        user.setId(userId);
        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(user);
    }

    private void givenUserWithRole(RolUsuario role) {
        userId = randomUUID();
        user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(user);
    }

    private void givenSuggestionWithoutProduct() {
        studentId = randomUUID();
        suggestionId = randomUUID();
        suggestion = SugerenciaIA.builder()
                .id(suggestionId)
                .alumnoId(studentId)
                .productoId(randomUUID())
                .mensaje("Mensaje test")
                .build();
    }

    private void givenSuggestionWithProduct(Producto product) {
        studentId = randomUUID();
        suggestionId = randomUUID();
        suggestion = SugerenciaIA.builder()
                .id(suggestionId)
                .alumnoId(studentId)
                .producto(product)
                .build();
    }

    private void givenPurchaseSuggestionExists() {
        when(purchaseSuggestIA.execute(new PurchaseSuggestIARequest(cognitoId)))
                .thenReturn(Optional.of(PurchaseSuggestionResponse.fromDomain(suggestion)));
    }

    private void givenPurchaseSuggestionDoesNotExist() {
        when(purchaseSuggestIA.execute(new PurchaseSuggestIARequest(cognitoId)))
                .thenReturn(Optional.empty());
    }

    private void givenUseCaseWithFailingProductSerialization() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {});
        useCase = new NotifyPurchaseSuggestions(
                purchaseSuggestIA,
                pushNotificationSender,
                usuarioService,
                failingMapper
        );
    }

    private void whenNotifyingPurchaseSuggestions() {
        response = useCase.execute(new NotifyPurchaseSuggestionsRequest(cognitoId));
    }

    private void thenNotificationWasSent() {
        ArgumentCaptor<PushNotification> captor =
                ArgumentCaptor.forClass(PushNotification.class);
        verify(pushNotificationSender).send(captor.capture());
        sentNotification = captor.getValue();
    }

    private void thenNotificationHasSuggestionMetadata() {
        assertEquals(userId, sentNotification.userId());
        assertNull(sentNotification.title());
        assertNull(sentNotification.message());
        assertEquals("PURCHASE_SUGGESTION", sentNotification.data().get("type"));
        assertEquals("ALUMNO", sentNotification.data().get("rol"));
        assertEquals(suggestionId.toString(), sentNotification.data().get("sugerenciaId"));
        assertEquals(studentId.toString(), sentNotification.data().get("alumnoId"));
        assertNotNull(sentNotification.data().get("titulo"));
        assertNotNull(sentNotification.data().get("mensaje"));
        assertEquals("{}", sentNotification.data().get("producto"));
    }

    private void thenResponseHasNoNotification() {
        assertNull(response.notification());
    }

    private void thenUserWasNotLoaded() {
        verifyNoInteractions(usuarioService);
    }

    private void thenNotificationWasNotSent() {
        verifyNoInteractions(pushNotificationSender);
    }

    private void thenResponseNotificationHasRole(String expectedRole) {
        PushNotificationResponse notification = response.notification();
        assertEquals(expectedRole, notification.data().get("rol"));
    }

    private void thenSerializedProductHasHealthyProductData() throws Exception {
        Map<String, Object> productData = productData();
        assertEquals("Yogur", productData.get("nombre"));
        assertEquals("Saludable", firstHealthClassification(productData).get("descripcion"));
    }

    private void thenProductDataIs(String expectedProductData) {
        assertEquals(expectedProductData, sentNotification.data().get("producto"));
    }

    private void thenSerializedProductHasFallbacks() throws Exception {
        Map<String, Object> productData = productData();
        Map<String, Object> category = category(productData);
        assertEquals("", productData.get("descripcion"));
        assertEquals("", category.get("id"));
        assertEquals("General", category.get("descripcion"));
        assertEquals(List.of(), productData.get("clasificacionesSalud"));
    }

    private Map<String, Object> productData() throws Exception {
        return objectMapper.readValue(
                sentNotification.data().get("producto"),
                new TypeReference<>() {
                }
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> category(Map<String, Object> productData) {
        return (Map<String, Object>) productData.get("categoria");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstHealthClassification(Map<String, Object> productData) {
        List<Map<String, Object>> classifications =
                (List<Map<String, Object>>) productData.get("clasificacionesSalud");
        return classifications.get(0);
    }

    private Producto healthyProduct() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(randomUUID());
        category.setDescripcion("Lacteos");

        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(randomUUID());
        classification.setDescripcion("Saludable");

        Producto product = product("Yogur");
        product.setCategoria(category);
        product.setClasificacionesSalud(Set.of(classification));
        return product;
    }

    private Producto productWithoutCategoryOrClassifications() {
        Producto product = product("Manzana");
        product.setDescripcion(null);
        product.setCategoria(null);
        product.setClasificacionesSalud(null);
        return product;
    }

    private Producto product(String name) {
        Producto product = new Producto();
        product.setId(randomUUID());
        product.setNombre(name);
        product.setPrecio(BigDecimal.ONE);
        product.setDescripcion(null);
        return product;
    }
}
