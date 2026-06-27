package com.recred.backend.web;

import com.recred.backend.infrastructure.web.*;
import com.recred.backend.application.GenerarMenuBuffet;
import com.recred.backend.application.GenerateLowBalanceAlerts;
import com.recred.backend.application.GetAllCategories;
import com.recred.backend.application.GetBuffetByStudent;
import com.recred.backend.application.GetBuffetByStudentRequest;
import com.recred.backend.application.GetBuffetByStudentResponse;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.GetTimeSlotsBySchoolRequest;
import com.recred.backend.application.GetTimeSlotsBySchoolResponse;
import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.UpdateTutorDashboardConfig;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.infrastructure.push.FirebasePushNotificationSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmallControllerDelegationTest {

    @Mock
    private GetTutorGlobalDashboard getTutorGlobalDashboard;
    @Mock
    private UpdateTutorDashboardConfig updateTutorDashboardConfig;
    @Mock
    private GetTimeSlotsBySchool getTimeSlotsBySchool;
    @Mock
    private GenerateLowBalanceAlerts generateLowBalanceAlerts;
    @Mock
    private GetAllCategories getAllCategories;
    @Mock
    private GenerarMenuBuffet generarMenuBuffet;
    @Mock
    private GetBuffetByStudent getBuffetByStudent;
    @Mock
    private FirebasePushNotificationSender pushNotificationSender;

    @Test
    void tutorGlobalDashboardShouldUseAuthenticatedSubject() {
        TutorGlobalDashboardDTO dashboard = new TutorGlobalDashboardDTO(
                UUID.randomUUID().toString(),
                BigDecimal.TEN,
                BigDecimal.ZERO,
                null,
                List.of(),
                "GRATUITO"
        );
        when(getTutorGlobalDashboard.executeByCognitoSubject("sub")).thenReturn(dashboard);
        TutorGlobalDashboardController controller =
                new TutorGlobalDashboardController(getTutorGlobalDashboard, updateTutorDashboardConfig);

        ResponseEntity<TutorGlobalDashboardDTO> response = controller.getGlobalDashboard(
                new UsernamePasswordAuthenticationToken("sub", null)
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dashboard, response.getBody());
    }

    @Test
    void tutorGlobalDashboardWithoutAuthenticationShouldThrow() {
        UpdateTutorDashboardConfig updateConfig = mock(UpdateTutorDashboardConfig.class);
        TutorGlobalDashboardController controller = new TutorGlobalDashboardController(getTutorGlobalDashboard, updateConfig);

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.getGlobalDashboard(null));
    }

    @Test
    void franjaHorariaControllerShouldMapTimeSlots() {
        UUID schoolId = UUID.randomUUID();
        GetTimeSlotsBySchoolResponse slot = new GetTimeSlotsBySchoolResponse(
                UUID.randomUUID(),
                schoolId,
                "Primer recreo",
                LocalTime.of(9, 0),
                LocalTime.of(9, 20),
                true,
                100,
                10
        );
        when(getTimeSlotsBySchool.execute(new GetTimeSlotsBySchoolRequest(schoolId))).thenReturn(List.of(slot));
        FranjaHorariaController controller = new FranjaHorariaController(getTimeSlotsBySchool);

        ResponseEntity<List<TimeSlotResponse>> response = controller.obtenerFranjas(schoolId);

        assertEquals(1, response.getBody().size());
        assertEquals(slot.id(), response.getBody().get(0).id());
    }

    @Test
    void lowBalanceControllerShouldReturnOkOrError() {
        LowBalanceAlertController controller = new LowBalanceAlertController(
                generateLowBalanceAlerts
        );

        ResponseEntity<String> ok = controller.triggerLowBalanceAlerts();

        assertEquals(HttpStatus.OK, ok.getStatusCode());

        doThrow(new RuntimeException("boom")).when(generateLowBalanceAlerts).execute();

        ResponseEntity<String> error = controller.triggerLowBalanceAlerts();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
        assertTrue(error.getBody().contains("boom"));
    }

    @Test
    void categoryControllerShouldMapCategories() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Bebidas");
        when(getAllCategories.execute()).thenReturn(List.of(category));
        CategoryController controller = new CategoryController(getAllCategories);

        List<CategoryResponse> response = controller.getCategories();

        assertEquals(1, response.size());
        assertEquals(category.getId(), response.get(0).id());
    }

    @Test
    void menuBuffetControllerShouldUseProvidedDateAndMapBuffetOptional() {
        UUID alumnoId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.of(2026, 6, 15, 10, 0);
        MenuProductoDTO item = MenuProductoDTO.permitido(
                product("Jugo", new BigDecimal("10.00"))
        );
        GetBuffetByStudentResponse responseBuffet = new GetBuffetByStudentResponse(
                buffetId,
                "Buffet",
                null,
                null,
                null,
                true,
                null,
                null
        );
        when(generarMenuBuffet.execute(alumnoId, buffetId, dateTime)).thenReturn(List.of(item));
        when(getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId))).thenReturn(Optional.of(responseBuffet));
        MenuBuffetController controller = new MenuBuffetController(
                generarMenuBuffet,
                getBuffetByStudent
        );

        ResponseEntity<List<MenuProductoDTO>> menu =
                controller.obtenerMenu(alumnoId, buffetId, dateTime);
        ResponseEntity<AlumnoBuffetResponseDTO> buffetResponse = controller.obtenerBuffet(alumnoId);

        assertEquals(List.of(item), menu.getBody());
        assertEquals(buffetId, buffetResponse.getBody().id());
    }

    @Test
    void menuBuffetControllerShouldUseNowWhenDateIsMissingAndReturnNotFoundBuffet() {
        UUID alumnoId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(generarMenuBuffet.execute(eq(alumnoId), eq(buffetId), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId))).thenReturn(Optional.empty());
        MenuBuffetController controller = new MenuBuffetController(
                generarMenuBuffet,
                getBuffetByStudent
        );

        ResponseEntity<List<MenuProductoDTO>> menu =
                controller.obtenerMenu(alumnoId, buffetId, null);
        ResponseEntity<AlumnoBuffetResponseDTO> buffetResponse = controller.obtenerBuffet(alumnoId);

        assertEquals(List.of(), menu.getBody());
        assertEquals(HttpStatus.NOT_FOUND, buffetResponse.getStatusCode());
    }

    @Test
    void notificationTestControllerShouldSendAndReturnNotification() {
        UUID userId = UUID.randomUUID();
        NotificationTestController controller = new NotificationTestController(
                pushNotificationSender
        );

        PushNotification notification = controller.sendTestNotification(userId);

        assertEquals(userId, notification.userId());
        verify(pushNotificationSender).send(notification);
    }

    private Producto product(String name, BigDecimal price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(price);
        return product;
    }
}
