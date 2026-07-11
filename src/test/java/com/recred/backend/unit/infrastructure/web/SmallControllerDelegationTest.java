package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreateTimeSlot;
import com.recred.backend.application.DeactivateTimeSlot;
import com.recred.backend.application.GenerateBuffetMenu;
import com.recred.backend.application.GenerateBuffetMenuRequest;
import com.recred.backend.application.GenerateBuffetMenuResponse;
import com.recred.backend.application.GenerateLowBalanceAlerts;
import com.recred.backend.application.GenerateLowBalanceAlertsRequest;
import com.recred.backend.application.GetAllCategories;
import com.recred.backend.application.GetAllCategoriesRequest;
import com.recred.backend.application.GetAllCategoriesResponse;
import com.recred.backend.application.GetBuffetByStudent;
import com.recred.backend.application.GetBuffetByStudentRequest;
import com.recred.backend.application.GetBuffetByStudentResponse;
import com.recred.backend.application.GetTimeSlot;
import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.application.GetTimeSlotsBySchoolRequest;
import com.recred.backend.application.GetTimeSlotsBySchoolResponse;
import com.recred.backend.application.GetTutorGlobalDashboard;
import com.recred.backend.application.GetTutorGlobalDashboardRequest;
import com.recred.backend.application.GetTutorGlobalDashboardResponse;
import com.recred.backend.application.ListSchoolTimeSlots;
import com.recred.backend.application.MenuProductResponse;
import com.recred.backend.application.ReactivateTimeSlot;
import com.recred.backend.application.UpdateTimeSlot;
import com.recred.backend.application.UpdateTutorDashboardConfig;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.infrastructure.push.FirebasePushNotificationSender;
import com.recred.backend.infrastructure.web.AlumnoBuffetResponseDTO;
import com.recred.backend.infrastructure.web.CategoryResponse;
import com.recred.backend.infrastructure.web.CategoryController;
import com.recred.backend.infrastructure.web.FranjaHorariaController;
import com.recred.backend.infrastructure.web.LowBalanceAlertController;
import com.recred.backend.infrastructure.web.MenuBuffetController;
import com.recred.backend.infrastructure.web.MenuProductoDTO;
import com.recred.backend.infrastructure.web.NotificationTestController;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.SchoolAccessGuard;
import com.recred.backend.infrastructure.web.TimeSlotResponse;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardController;
import com.recred.backend.infrastructure.web.TutorGlobalDashboardDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private PlanAccessGuard planAccessGuard;

    @Test
    void tutorGlobalDashboardShouldUseAuthenticatedSubject() {
        String cognitoSubject = "sub";
        GetTutorGlobalDashboardResponse expected = givenTutorGlobalDashboardFor(cognitoSubject);
        TutorGlobalDashboardController controller = givenTutorGlobalDashboardController();

        ResponseEntity<TutorGlobalDashboardDTO> response =
                whenGettingTutorGlobalDashboard(controller, authenticatedAs(cognitoSubject));

        thenTutorGlobalDashboardIsReturned(response, expected);
    }

    @Test
    void tutorGlobalDashboardWithoutAuthenticationShouldThrow() {
        TutorGlobalDashboardController controller = givenTutorGlobalDashboardController();

        Executable action = whenGettingTutorGlobalDashboardWithoutAuthentication(controller);

        thenAuthenticationIsRejected(action);
    }

    @Test
    void franjaHorariaControllerShouldMapTimeSlots() {
        UUID schoolId = UUID.randomUUID();
        GetTimeSlotsBySchoolResponse slot = givenTimeSlotForSchool(schoolId);
        FranjaHorariaController controller = givenFranjaHorariaController();

        ResponseEntity<List<TimeSlotResponse>> response =
                whenGettingTimeSlots(controller, schoolId);

        thenTimeSlotsAreMapped(response, slot);
    }

    @Test
    void lowBalanceControllerShouldReturnOkWhenAlertsAreGenerated() {
        LowBalanceAlertController controller = givenLowBalanceAlertController();

        ResponseEntity<String> response = whenTriggeringLowBalanceAlerts(controller);

        thenLowBalanceAlertsAreOk(response);
    }

    @Test
    void lowBalanceControllerShouldReturnErrorWhenAlertsFail() {
        givenLowBalanceAlertsFail();
        LowBalanceAlertController controller = givenLowBalanceAlertController();

        ResponseEntity<String> response = whenTriggeringLowBalanceAlerts(controller);

        thenLowBalanceAlertsReturnError(response);
    }

    @Test
    void categoryControllerShouldMapCategories() {
        CategoriaProducto category = givenCategory();
        givenCategories(category);
        CategoryController controller = givenCategoryController();

        List<CategoryResponse> response = whenGettingCategories(controller);

        thenCategoriesAreMapped(response, category);
    }

    @Test
    void menuBuffetControllerShouldUseProvidedDate() {
        UUID alumnoId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.of(2026, 6, 15, 10, 0);
        MenuProductResponse item = givenMenuItemFor(alumnoId, buffetId, dateTime);
        MenuBuffetController controller = givenMenuBuffetController();

        ResponseEntity<List<MenuProductoDTO>> response =
                whenGettingMenu(controller, alumnoId, buffetId, dateTime);

        thenMenuIsMapped(response, item);
    }

    @Test
    void menuBuffetControllerShouldMapBuffetWhenFound() {
        UUID alumnoId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        givenBuffetForStudent(alumnoId, buffetId);
        MenuBuffetController controller = givenMenuBuffetController();

        ResponseEntity<AlumnoBuffetResponseDTO> response =
                whenGettingBuffet(controller, alumnoId);

        thenBuffetIsMapped(response, buffetId);
    }

    @Test
    void menuBuffetControllerShouldUseCurrentDateWhenDateIsMissing() {
        UUID alumnoId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        givenEmptyMenuForAnyDate();
        MenuBuffetController controller = givenMenuBuffetController();

        ResponseEntity<List<MenuProductoDTO>> response =
                whenGettingMenu(controller, alumnoId, buffetId, null);

        thenMenuIsEmptyAndGeneratedDateWasUsed(response, alumnoId, buffetId);
    }

    @Test
    void menuBuffetControllerShouldReturnNotFoundWhenStudentHasNoBuffet() {
        UUID alumnoId = UUID.randomUUID();
        givenNoBuffetForStudent(alumnoId);
        MenuBuffetController controller = givenMenuBuffetController();

        ResponseEntity<AlumnoBuffetResponseDTO> response =
                whenGettingBuffet(controller, alumnoId);

        thenBuffetIsNotFound(response);
    }

    @Test
    void notificationTestControllerShouldSendAndReturnNotification() {
        UUID userId = UUID.randomUUID();
        NotificationTestController controller = givenNotificationTestController();

        PushNotification notification = whenSendingTestNotification(controller, userId);

        thenNotificationIsSent(notification, userId);
    }

    private GetTutorGlobalDashboardResponse givenTutorGlobalDashboardFor(String cognitoSubject) {
        GetTutorGlobalDashboardResponse response = new GetTutorGlobalDashboardResponse(
                "ME",
                BigDecimal.TEN,
                BigDecimal.ZERO,
                null,
                List.of(),
                "GRATUITO"
        );
        when(getTutorGlobalDashboard.execute(new GetTutorGlobalDashboardRequest(cognitoSubject)))
                .thenReturn(response);
        return response;
    }

    private TutorGlobalDashboardController givenTutorGlobalDashboardController() {
        return new TutorGlobalDashboardController(
                getTutorGlobalDashboard,
                updateTutorDashboardConfig,
                planAccessGuard
        );
    }

    private Authentication authenticatedAs(String cognitoSubject) {
        return new UsernamePasswordAuthenticationToken(cognitoSubject, null);
    }

    private ResponseEntity<TutorGlobalDashboardDTO> whenGettingTutorGlobalDashboard(
            TutorGlobalDashboardController controller,
            Authentication authentication
    ) {
        return controller.getGlobalDashboard(authentication);
    }

    private Executable whenGettingTutorGlobalDashboardWithoutAuthentication(
            TutorGlobalDashboardController controller
    ) {
        return () -> controller.getGlobalDashboard(null);
    }

    private void thenTutorGlobalDashboardIsReturned(
            ResponseEntity<TutorGlobalDashboardDTO> response,
            GetTutorGlobalDashboardResponse expected
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected.planStr(), response.getBody().plan());
        assertEquals(expected.totalBalance(), response.getBody().totalBalance());
    }

    private void thenAuthenticationIsRejected(Executable action) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, action);
    }

    private GetTimeSlotsBySchoolResponse givenTimeSlotForSchool(UUID schoolId) {
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
        when(getTimeSlotsBySchool.execute(new GetTimeSlotsBySchoolRequest(schoolId)))
                .thenReturn(List.of(slot));
        return slot;
    }

    private FranjaHorariaController givenFranjaHorariaController() {
        return new FranjaHorariaController(
                getTimeSlotsBySchool,
                listSchoolTimeSlots,
                getTimeSlot,
                createTimeSlot,
                updateTimeSlot,
                deactivateTimeSlot,
                reactivateTimeSlot,
                schoolAccessGuard
        );
    }

    private ResponseEntity<List<TimeSlotResponse>> whenGettingTimeSlots(
            FranjaHorariaController controller,
            UUID schoolId
    ) {
        return controller.obtenerFranjas(schoolId, false, null);
    }

    private void thenTimeSlotsAreMapped(
            ResponseEntity<List<TimeSlotResponse>> response,
            GetTimeSlotsBySchoolResponse slot
    ) {
        assertEquals(1, response.getBody().size());
        assertEquals(slot.id(), response.getBody().get(0).id());
    }

    private LowBalanceAlertController givenLowBalanceAlertController() {
        return new LowBalanceAlertController(generateLowBalanceAlerts);
    }

    private void givenLowBalanceAlertsFail() {
        doThrow(new RuntimeException("boom")).when(generateLowBalanceAlerts)
                .execute(new GenerateLowBalanceAlertsRequest());
    }

    private ResponseEntity<String> whenTriggeringLowBalanceAlerts(LowBalanceAlertController controller) {
        return controller.triggerLowBalanceAlerts();
    }

    private void thenLowBalanceAlertsAreOk(ResponseEntity<String> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void thenLowBalanceAlertsReturnError(ResponseEntity<String> response) {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("boom"));
    }

    private CategoriaProducto givenCategory() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Bebidas");
        return category;
    }

    private void givenCategories(CategoriaProducto category) {
        when(getAllCategories.execute(new GetAllCategoriesRequest()))
                .thenReturn(new GetAllCategoriesResponse(List.of(
                        com.recred.backend.application.CategoryResponse.fromDomain(category)
                )));
    }

    private CategoryController givenCategoryController() {
        return new CategoryController(getAllCategories);
    }

    private List<CategoryResponse> whenGettingCategories(CategoryController controller) {
        return controller.getCategories();
    }

    private void thenCategoriesAreMapped(List<CategoryResponse> response, CategoriaProducto category) {
        assertEquals(1, response.size());
        assertEquals(category.getId(), response.get(0).id());
    }

    private MenuProductResponse givenMenuItemFor(
            UUID alumnoId,
            UUID buffetId,
            LocalDateTime dateTime
    ) {
        MenuProductResponse item = MenuProductResponse.permitido(
                product("Jugo", new BigDecimal("10.00"))
        );
        when(generateBuffetMenu.execute(new GenerateBuffetMenuRequest(
                alumnoId,
                buffetId,
                dateTime
        ))).thenReturn(new GenerateBuffetMenuResponse(List.of(item)));
        return item;
    }

    private MenuBuffetController givenMenuBuffetController() {
        return new MenuBuffetController(generateBuffetMenu, getBuffetByStudent);
    }

    private ResponseEntity<List<MenuProductoDTO>> whenGettingMenu(
            MenuBuffetController controller,
            UUID alumnoId,
            UUID buffetId,
            LocalDateTime dateTime
    ) {
        return controller.obtenerMenu(alumnoId, buffetId, dateTime);
    }

    private void thenMenuIsMapped(
            ResponseEntity<List<MenuProductoDTO>> response,
            MenuProductResponse item
    ) {
        assertEquals(List.of(MenuProductoDTO.fromApplication(item)), response.getBody());
    }

    private void givenBuffetForStudent(UUID alumnoId, UUID buffetId) {
        GetBuffetByStudentResponse response = new GetBuffetByStudentResponse(
                buffetId,
                "Buffet",
                null,
                null,
                null,
                true,
                false,
                null,
                null
        );
        when(getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId)))
                .thenReturn(Optional.of(response));
    }

    private ResponseEntity<AlumnoBuffetResponseDTO> whenGettingBuffet(
            MenuBuffetController controller,
            UUID alumnoId
    ) {
        return controller.obtenerBuffet(alumnoId);
    }

    private void thenBuffetIsMapped(
            ResponseEntity<AlumnoBuffetResponseDTO> response,
            UUID buffetId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(buffetId, response.getBody().id());
    }

    private void givenEmptyMenuForAnyDate() {
        when(generateBuffetMenu.execute(any(GenerateBuffetMenuRequest.class)))
                .thenReturn(new GenerateBuffetMenuResponse(List.of()));
    }

    private void thenMenuIsEmptyAndGeneratedDateWasUsed(
            ResponseEntity<List<MenuProductoDTO>> response,
            UUID alumnoId,
            UUID buffetId
    ) {
        ArgumentCaptor<GenerateBuffetMenuRequest> captor =
                ArgumentCaptor.forClass(GenerateBuffetMenuRequest.class);

        assertEquals(List.of(), response.getBody());
        verify(generateBuffetMenu).execute(captor.capture());
        assertEquals(alumnoId, captor.getValue().alumnoId());
        assertEquals(buffetId, captor.getValue().buffetId());
        assertNotNull(captor.getValue().momentoConsulta());
    }

    private void givenNoBuffetForStudent(UUID alumnoId) {
        when(getBuffetByStudent.execute(new GetBuffetByStudentRequest(alumnoId)))
                .thenReturn(Optional.empty());
    }

    private void thenBuffetIsNotFound(ResponseEntity<AlumnoBuffetResponseDTO> response) {
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private NotificationTestController givenNotificationTestController() {
        return new NotificationTestController(pushNotificationSender);
    }

    private PushNotification whenSendingTestNotification(
            NotificationTestController controller,
            UUID userId
    ) {
        return controller.sendTestNotification(userId);
    }

    private void thenNotificationIsSent(PushNotification notification, UUID userId) {
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
