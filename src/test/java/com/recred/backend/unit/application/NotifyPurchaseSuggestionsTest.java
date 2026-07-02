package com.recred.backend.unit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.NotifyPurchaseSuggestionsRequest;
import com.recred.backend.application.NotifyPurchaseSuggestionsResponse;
import com.recred.backend.application.NotifyPurchaseSuggestions;
import com.recred.backend.application.PurchaseSuggestIARequest;
import com.recred.backend.application.PurchaseSuggestionResponse;
import com.recred.backend.application.PurchaseSuggestIA;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifyPurchaseSuggestionsTest {

    @Mock
    private PurchaseSuggestIA purchaseSuggestIA;
    @Mock
    private PushNotificationSender pushNotificationSender;
    @Mock
    private UsuarioService usuarioService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotifyPurchaseSuggestions useCase;

    @Test
    @DisplayName("execute_CuandoHaySugerencia_EnviaNotificacionConMetadatos")
    void execute_CuandoHaySugerencia_EnviaNotificacionConMetadatos() {
        String cognitoId = "cognito-id-test";
        UUID userId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        SugerenciaIA sugerencia = SugerenciaIA.builder()
                .id(suggestionId)
                .alumnoId(alumnoId)
                .productoId(productId)
                .mensaje("Mensaje test")
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setRol(RolUsuario.ALUMNO);

        when(purchaseSuggestIA.execute(new PurchaseSuggestIARequest(cognitoId)))
                .thenReturn(Optional.of(PurchaseSuggestionResponse.fromDomain(sugerencia)));
        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(usuario);


        useCase.execute(new NotifyPurchaseSuggestionsRequest(cognitoId));


        ArgumentCaptor<PushNotification> captor = ArgumentCaptor.forClass(PushNotification.class);
        verify(pushNotificationSender, times(1)).send(captor.capture());

        PushNotification sentNotification = captor.getValue();
        assertEquals(userId, sentNotification.userId());
        assertNull(sentNotification.title());
        assertNull(sentNotification.message());
        assertEquals("PURCHASE_SUGGESTION", sentNotification.data().get("type"));
        assertEquals(suggestionId.toString(), sentNotification.data().get("sugerenciaId"));
        assertEquals(alumnoId.toString(), sentNotification.data().get("alumnoId"));
        assertNotNull(sentNotification.data().get("titulo"));
        assertNotNull(sentNotification.data().get("mensaje"));
        assertNotNull(sentNotification.data().get("producto"));
    }

    @Test
    @DisplayName("execute_CuandoNoHaySugerencia_NoEnviaNada")
    void execute_CuandoNoHaySugerencia_NoEnviaNada() {

        String cognitoId = "cognito-id-test";
        when(purchaseSuggestIA.execute(new PurchaseSuggestIARequest(cognitoId)))
                .thenReturn(Optional.empty());

        useCase.execute(new NotifyPurchaseSuggestionsRequest(cognitoId));

        verifyNoInteractions(pushNotificationSender);
    }

    @Test
    void execute_CuandoElUsuarioNoTieneRol_UsaRolPendiente() {
        String cognitoId = "cognito-id-test";
        UUID userId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        SugerenciaIA sugerencia = SugerenciaIA.builder()
                .id(UUID.randomUUID())
                .alumnoId(alumnoId)
                .build();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        when(purchaseSuggestIA.execute(new PurchaseSuggestIARequest(cognitoId)))
                .thenReturn(Optional.of(PurchaseSuggestionResponse.fromDomain(sugerencia)));
        when(usuarioService.obtenerUsuarioActual(cognitoId)).thenReturn(usuario);

        NotifyPurchaseSuggestionsResponse response = useCase.execute(
                new NotifyPurchaseSuggestionsRequest(cognitoId));
        PushNotificationResponse notification = response.notification();

        assertEquals("PENDIENTE", notification.data().get("rol"));
        verify(pushNotificationSender).send(any(PushNotification.class));
    }

    @Test
    void createNotification_CuandoLaSugerenciaTieneProducto_SerializaProductoEnData() {
        UUID userId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        SugerenciaIA sugerencia = SugerenciaIA.builder()
                .id(UUID.randomUUID())
                .alumnoId(alumnoId)
                .producto(producto())
                .build();

        PushNotification notification = useCase.createNotification(
                userId,
                PurchaseSuggestionResponse.fromDomain(sugerencia),
                "ALUMNO"
        );

        assertEquals(userId, notification.userId());
        assertEquals("PURCHASE_SUGGESTION", notification.data().get("type"));
        assertEquals("ALUMNO", notification.data().get("rol"));
        assertNotNull(notification.data().get("producto"));
        org.junit.jupiter.api.Assertions.assertTrue(
                notification.data().get("producto").contains("Yogur")
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                notification.data().get("producto").contains("Saludable")
        );
    }

    @Test
    void createNotification_CuandoFallaSerializacion_UsaProductoVacio() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {});
        NotifyPurchaseSuggestions failingUseCase = new NotifyPurchaseSuggestions(
                purchaseSuggestIA,
                pushNotificationSender,
                usuarioService,
                failingMapper
        );
        SugerenciaIA sugerencia = SugerenciaIA.builder()
                .id(UUID.randomUUID())
                .alumnoId(UUID.randomUUID())
                .producto(producto())
                .build();

        PushNotification notification = failingUseCase.createNotification(
                UUID.randomUUID(),
                PurchaseSuggestionResponse.fromDomain(sugerencia),
                "ALUMNO"
        );

        assertEquals("{}", notification.data().get("producto"));
    }

    @Test
    void createNotification_CuandoProductoNoTieneCategoriaNiClasificaciones_UsaFallbacks()
            throws Exception {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Manzana");
        producto.setPrecio(BigDecimal.ONE);
        producto.setDescripcion(null);
        producto.setCategoria(null);
        producto.setClasificacionesSalud(null);
        SugerenciaIA sugerencia = SugerenciaIA.builder()
                .id(UUID.randomUUID())
                .alumnoId(UUID.randomUUID())
                .producto(producto)
                .build();

        PushNotification notification = useCase.createNotification(
                UUID.randomUUID(),
                PurchaseSuggestionResponse.fromDomain(sugerencia),
                "PADRE"
        );

        Map<String, Object> productoData = objectMapper.readValue(
                (String) notification.data().get("producto"),
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                }
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> categoria = (Map<String, Object>) productoData.get("categoria");
        assertEquals("", productoData.get("descripcion"));
        assertEquals("", categoria.get("id"));
        assertEquals("General", categoria.get("descripcion"));
        assertEquals(List.of(), productoData.get("clasificacionesSalud"));
    }

    private Producto producto() {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(UUID.randomUUID());
        categoria.setDescripcion("Lacteos");

        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setId(UUID.randomUUID());
        clasificacion.setDescripcion("Saludable");

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Yogur");
        producto.setPrecio(new BigDecimal("120.00"));
        producto.setDescripcion(null);
        producto.setCategoria(categoria);
        producto.setClasificacionesSalud(Set.of(clasificacion));
        return producto;
    }
}
