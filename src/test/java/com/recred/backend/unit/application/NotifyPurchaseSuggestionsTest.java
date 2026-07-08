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
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyPurchaseSuggestionsTest {

    private static final String COGNITO_ID = "cognito-id-test";
    private static final String NOTIFICATION_TITLE = "Recomendación para tu recreo";
    private static final String PUSH_TYPE = "PURCHASE_SUGGESTION";

    @Mock
    private PurchaseSuggestIA purchaseSuggestIA;
    @Mock
    private PushNotificationSender pushNotificationSender;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private NotificationRepository notificationRepository;

    @Captor
    private ArgumentCaptor<PushNotification> pushNotificationCaptor;
    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;
    @Captor
    private ArgumentCaptor<UUID> userIdCaptor;

    private ObjectMapper objectMapper;
    private NotifyPurchaseSuggestions notifyPurchaseSuggestions;
    private NotifyPurchaseSuggestionsResponse notifyPurchaseSuggestionsResponse;
    private NotifyPurchaseSuggestionsRequest notifyPurchaseSuggestionsRequest;
    private UUID userId;
    private UUID studentId;
    private UUID suggestionId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notifyPurchaseSuggestions = new NotifyPurchaseSuggestions(
                purchaseSuggestIA,
                pushNotificationSender,
                usuarioService,
                objectMapper,
                notificationRepository
        );
        notifyPurchaseSuggestionsRequest = new NotifyPurchaseSuggestionsRequest(COGNITO_ID);
    }

    @Test
    void givenSuggestionExists_whenNotifyingPurchaseSuggestions_thenSendsAndPersistsNotification() {
        givenSuggestionWithoutProduct();
        givenAuthenticatedStudent();

        whenNotifyPurchaseSuggestions();

        thenPushNotificationWasSent();
        thenPushNotificationHasSuggestionMetadata();
        thenNotificationWasPersisted();
    }

    @Test
    void givenNoSuggestion_whenNotifyingPurchaseSuggestions_thenDoesNotSendNotification() {
        givenNoSuggestion();

        whenNotifyPurchaseSuggestions();

        thenNoNotificationWasReturned();
        thenNoNotificationWasSentOrPersisted();
    }

    @Test
    void givenUserWithoutRole_whenNotifyingPurchaseSuggestions_thenUsesPendingRole() {
        givenSuggestionWithoutProduct();
        givenAuthenticatedUserWithoutRole();

        whenNotifyPurchaseSuggestions();

        thenPendingRoleWasUsed();
    }

    @Test
    void givenSuggestionWithProduct_whenNotifyingPurchaseSuggestions_thenSerializesProduct() {
        givenSuggestionWithProduct(product());
        givenAuthenticatedStudent();

        whenNotifyPurchaseSuggestions();

        thenProductWasSerializedInPushData();
    }

    @Test
    void givenProductSerializationFails_whenNotifyingPurchaseSuggestions_thenUsesEmptyProduct()
            throws Exception {
        givenUseCaseWithFailingMapper();
        givenSuggestionWithProduct(product());
        givenAuthenticatedStudent();

        whenNotifyPurchaseSuggestions();

        thenEmptyProductWasUsed();
    }

    @Test
    void givenProductWithoutCategory_whenNotifyingPurchaseSuggestions_thenUsesProductFallbacks()
            throws Exception {
        givenSuggestionWithProduct(productWithoutCategoryOrClassifications());
        givenAuthenticatedParent();

        whenNotifyPurchaseSuggestions();

        thenProductFallbacksWereUsed();
    }

    private void givenSuggestionWithoutProduct() {
        givenSuggestion(suggestionWithoutProduct());
    }

    private void givenSuggestionWithProduct(Producto product) {
        givenSuggestion(suggestionWithProduct(product));
    }

    private void givenSuggestion(SugerenciaIA suggestion) {
        when(purchaseSuggestIA.execute(new PurchaseSuggestIARequest(COGNITO_ID)))
                .thenReturn(Optional.of(PurchaseSuggestionResponse.fromDomain(suggestion)));
    }

    private void givenNoSuggestion() {
        when(purchaseSuggestIA.execute(new PurchaseSuggestIARequest(COGNITO_ID)))
                .thenReturn(Optional.empty());
    }

    private void givenAuthenticatedStudent() {
        givenAuthenticatedUser(RolUsuario.ALUMNO);
    }

    private void givenAuthenticatedParent() {
        givenAuthenticatedUser(RolUsuario.PADRE);
    }

    private void givenAuthenticatedUserWithoutRole() {
        givenAuthenticatedUser(null);
    }

    private void givenAuthenticatedUser(RolUsuario role) {
        userId = randomUUID();
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        when(usuarioService.obtenerUsuarioActual(COGNITO_ID)).thenReturn(user);
    }

    private void givenUseCaseWithFailingMapper() throws JsonProcessingException {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {
                });
        notifyPurchaseSuggestions = new NotifyPurchaseSuggestions(
                purchaseSuggestIA,
                pushNotificationSender,
                usuarioService,
                failingMapper,
                notificationRepository
        );
    }

    private void whenNotifyPurchaseSuggestions() {
        notifyPurchaseSuggestionsResponse =
                notifyPurchaseSuggestions.execute(notifyPurchaseSuggestionsRequest);
    }

    private void thenPushNotificationWasSent() {
        verify(pushNotificationSender).send(pushNotificationCaptor.capture());
    }

    private void thenPushNotificationHasSuggestionMetadata() {
        PushNotification notification = pushNotificationCaptor.getValue();
        assertEquals(userId, notification.userId());
        assertNull(notification.title());
        assertNull(notification.message());
        assertEquals(PUSH_TYPE, notification.data().get("type"));
        assertEquals(suggestionId.toString(), notification.data().get("sugerenciaId"));
        assertEquals(studentId.toString(), notification.data().get("alumnoId"));
        assertNotNull(notification.data().get("titulo"));
        assertNotNull(notification.data().get("mensaje"));
        assertNotNull(notification.data().get("producto"));
    }

    private void thenNotificationWasPersisted() {
        verify(notificationRepository).save(notificationCaptor.capture(), userIdCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertEquals(userId, userIdCaptor.getValue());
        assertEquals(NOTIFICATION_TITLE, notification.title());
        assertEquals(studentId, notification.studentId());
        assertNull(notification.productId());
        assertEquals(suggestionId, notification.suggestionId());
    }

    private void thenNoNotificationWasReturned() {
        assertNull(notifyPurchaseSuggestionsResponse.notification());
    }

    private void thenNoNotificationWasSentOrPersisted() {
        verifyNoInteractions(usuarioService, pushNotificationSender, notificationRepository);
    }

    private void thenPendingRoleWasUsed() {
        assertEquals(
                "PENDIENTE",
                notifyPurchaseSuggestionsResponse.notification().data().get("rol")
        );
        verify(pushNotificationSender).send(any(PushNotification.class));
    }

    private void thenProductWasSerializedInPushData() {
        PushNotification notification = sentPushNotification();
        assertEquals(userId, notification.userId());
        assertEquals(PUSH_TYPE, notification.data().get("type"));
        assertEquals("ALUMNO", notification.data().get("rol"));
        assertTrue(notification.data().get("producto").contains("Yogur"));
        assertTrue(notification.data().get("producto").contains("Saludable"));
    }

    private void thenEmptyProductWasUsed() {
        assertEquals("{}", sentPushNotification().data().get("producto"));
    }

    private void thenProductFallbacksWereUsed() throws JsonProcessingException {
        Map<String, Object> productData = sentProductData();
        @SuppressWarnings("unchecked")
        Map<String, Object> category = (Map<String, Object>) productData.get("categoria");
        assertEquals("", productData.get("descripcion"));
        assertEquals("", category.get("id"));
        assertEquals("General", category.get("descripcion"));
        assertEquals(List.of(), productData.get("clasificacionesSalud"));
    }

    private PushNotification sentPushNotification() {
        verify(pushNotificationSender).send(pushNotificationCaptor.capture());
        return pushNotificationCaptor.getValue();
    }

    private Map<String, Object> sentProductData() throws JsonProcessingException {
        return objectMapper.readValue(
                sentPushNotification().data().get("producto"),
                new TypeReference<>() {
                }
        );
    }

    private SugerenciaIA suggestionWithoutProduct() {
        studentId = randomUUID();
        suggestionId = randomUUID();
        return SugerenciaIA.builder()
                .id(suggestionId)
                .alumnoId(studentId)
                .mensaje("Mensaje test")
                .build();
    }

    private SugerenciaIA suggestionWithProduct(Producto product) {
        studentId = randomUUID();
        suggestionId = randomUUID();
        return SugerenciaIA.builder()
                .id(suggestionId)
                .alumnoId(studentId)
                .producto(product)
                .build();
    }

    private Producto product() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(randomUUID());
        category.setDescripcion("Lacteos");

        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(randomUUID());
        classification.setDescripcion("Saludable");

        Producto product = productWithoutCategoryOrClassifications();
        product.setNombre("Yogur");
        product.setPrecio(new BigDecimal("120.00"));
        product.setCategoria(category);
        product.setClasificacionesSalud(Set.of(classification));
        return product;
    }

    private Producto productWithoutCategoryOrClassifications() {
        Producto product = new Producto();
        product.setId(randomUUID());
        product.setNombre("Manzana");
        product.setPrecio(BigDecimal.ONE);
        product.setDescripcion(null);
        product.setCategoria(null);
        product.setClasificacionesSalud(null);
        return product;
    }
}
