package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.*;
import com.recred.backend.application.GenerateBuffetMenu;
import com.recred.backend.application.GenerateBuffetMenuRequest;
import com.recred.backend.application.GenerateBuffetMenuResponse;
import com.recred.backend.application.GenerateLowBalanceAlerts;
import com.recred.backend.application.GetAllCategories;
import com.recred.backend.application.GetBuffetByStudent;
import com.recred.backend.application.GetBuffetByStudentRequest;
import com.recred.backend.application.GetBuffetByStudentResponse;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.GetTimeSlotsBySchoolRequest;
import com.recred.backend.application.GetTimeSlotsBySchoolResponse;
import com.recred.backend.application.CreateTimeSlot;
import com.recred.backend.application.DeactivateTimeSlot;
import com.recred.backend.application.GetTimeSlot;
import com.recred.backend.application.ListSchoolTimeSlots;
import com.recred.backend.application.ReactivateTimeSlot;
import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.MenuProductResponse;
import com.recred.backend.application.UpdateTimeSlot;
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
    private ListSchoolTimeSlots listSchoolTimeSlots;
    @Mock
    private GetTimeSlot getTimeSlot;
    @Mock
    private CreateTimeSlot createTimeSlot;
    @Mock
    private UpdateTimeSlot updateTimeSlot;
    @Mock
    private DeactivateTimeSlot deactivateTimeSlot;
    @Mock
    private ReactivateTimeSlot reactivateTimeSlot;
    @Mock
    private SchoolAccessGuard schoolAccessGuard;
    @Mock
    private GenerateLowBalanceAlerts generateLowBalanceAlerts;
    @Mock
    private GetAllCategories getAllCategories;
    @Mock
    private GenerateBuffetMenu generateBuffetMenu;
    @Mock
    private GetBuffetByStudent getBuffetByStudent;
    @Mock
    private FirebasePushNotificationSender pushNotificationSender;

    @Test
    void tutorGlobalDashboardShouldUseAuthenticatedSubject() {
        com.recred.backend.application.GetTutorGlobalDashboardResponse response = 
                new com.recred.backend.application.GetTutorGlobalDashboardResponse(
                "ME",
                BigDecimal.TEN,
                BigDecimal.ZERO,
                null,
                List.of(),
                "GRATUITO"
        );
        when(getTutorGlobalDashboard.execute(any(com.recred.backend.application.GetTutorGlobalDashboardRequest.class)))
                .thenReturn(response);
                
        TutorGlobalDashboardController controller =
                new TutorGlobalDashboardController(getTutorGlobalDashboard, updateTutorDashboardConfig);

        ResponseEntity<TutorGlobalDashboardDTO> httpResponse = controller.getGlobalDashboard(
                new UsernamePasswordAuthenticationToken("sub", null)
        );

        assertEquals(HttpStatus.OK, httpResponse.getStatusCode());
        assertEquals("GRATUITO", httpResponse.getBody().plan());
        assertEquals(BigDecimal.TEN, httpResponse.getBody().totalBalance());
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
        FranjaHorariaController controller = new FranjaHorariaController(
                getTimeSlotsBySchool,
                listSchoolTimeSlots,
                getTimeSlot,
                createTimeSlot,
                updateTimeSlot,
                deactivateTimeSlot,
                reactivateTimeSlot,
                schoolAccessGuard
        );

        ResponseEntity<List<TimeSlotResponse>> response =
                controller.obtenerFranjas(schoolId, false, null);

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

        doThrow(new RuntimeException("boom")).when(generateLowBalanceAlerts)
                .execute(new com.recred.backend.application.GenerateLowBalanceAlertsRequest());

        ResponseEntity<String> error = controller.triggerLowBalanceAlerts();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
        assertTrue(error.getBody().contains("boom"));
    }

    @Test
    void categoryControllerShouldMapCategories() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Bebidas");
        when(getAllCategories.execute(new com.recred.backend.application
                .GetAllCategoriesRequest())).thenReturn(new com.recred.backend.application
                .GetAllCategoriesResponse(List.of(com.recred.backend.application
                        .CategoryResponse.fromDomain(category))));
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
        MenuProductResponse item = MenuProductResponse.permitido(
                product("Jugo", new BigDecimal("10.00"))
        );
        MenuProductoDTO itemDto = MenuProductoDTO.fromApplication(item);
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
        when(generateBuffetMenu.execute(new GenerateBuffetMenuRequest(
                alumnoId,
                buffetId,
                dateTime
        ))).thenReturn(new GenerateBuffetMenuResponse(List.of(item)));
        when(getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId))).thenReturn(Optional.of(responseBuffet));
        MenuBuffetController controller = new MenuBuffetController(
                generateBuffetMenu,
                getBuffetByStudent
        );

        ResponseEntity<List<MenuProductoDTO>> menu =
                controller.obtenerMenu(alumnoId, buffetId, dateTime);
        ResponseEntity<AlumnoBuffetResponseDTO> buffetResponse = controller.obtenerBuffet(alumnoId);

        assertEquals(List.of(itemDto), menu.getBody());
        assertEquals(buffetId, buffetResponse.getBody().id());
    }

    @Test
    void menuBuffetControllerShouldUseNowWhenDateIsMissingAndReturnNotFoundBuffet() {
        UUID alumnoId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(generateBuffetMenu.execute(any(GenerateBuffetMenuRequest.class)))
                .thenReturn(new GenerateBuffetMenuResponse(List.of()));
        when(getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId))).thenReturn(Optional.empty());
        MenuBuffetController controller = new MenuBuffetController(
                generateBuffetMenu,
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
