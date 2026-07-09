package com.recred.backend.config;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.BuffetAlreadyExistsForSchoolException;
import com.recred.backend.domain.exceptions.BuffetNotFoundException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.FileValidationException;
import com.recred.backend.domain.exceptions.GradeAlreadyExistsException;
import com.recred.backend.domain.exceptions.GradeNotFoundException;
import com.recred.backend.domain.exceptions.ImageValidationException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.InsufficientStockException;
import com.recred.backend.domain.exceptions.InventoryNotFoundException;
import com.recred.backend.domain.exceptions.InventoryOperationException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.exceptions.InvalidGradeException;
import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.InvalidTimeSlotException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.exceptions.KiosqueroPayoutConfigNotFoundException;
import com.recred.backend.domain.exceptions.LevelNotFoundException;
import com.recred.backend.domain.exceptions.NotificationNotFoundException;
import com.recred.backend.domain.exceptions.OperationNotAllowedException;
import com.recred.backend.domain.exceptions.PickupCutoffTimeExceededException;
import com.recred.backend.domain.exceptions.PickupSlotCapacityExceededException;
import com.recred.backend.domain.exceptions.PickupSlotRulesNotFoundException;
import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.ProductDailyCapacityExceededException;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.exceptions.TimeSlotInUseException;
import com.recred.backend.domain.exceptions.TimeSlotNotFoundException;
import com.recred.backend.domain.exceptions.TimeSlotOverlapException;
import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.exceptions.UsernameExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GlobalExceptionHandler.class);

    @ExceptionHandler(RestriccionHorariaException.class)
    public ResponseEntity<ErrorResponse>
    handleRestriccionHoraria(RestriccionHorariaException ex){
        ErrorResponse error = new ErrorResponse(
                "TIME_RESTRICTION",
                "Restricción Horaria",
                ex.getMessage(),
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                currentRequestId()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(PresupuestoExcedidoException.class)
    public ResponseEntity<Map<String, Object>> handlePresupuestoExcedido(
            PresupuestoExcedidoException ex) {

        LOGGER.warn(
                "Devolviendo error BUDGET_RESTRICTION (400): {} - RequestId: {}",
                ex.getMessage(),
                currentRequestId()
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("code", "BUDGET_RESTRICTION");
        body.put("error", "Límite de Presupuesto Superado");
        body.put("message", ex.getMessage());
        addRequestId(body);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RestriccionSaludException.class)
    public ResponseEntity<ErrorResponse> handleRestriccionSalud(
            RestriccionSaludException ex) {
        LOGGER.error("RestriccionSaludException caught: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.CONFLICT, "Conflicto de Restricción Médica",
                ex.getMessage(), "HEALTH_RESTRICTION");
    }

    @ExceptionHandler(AlumnoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleAlumnoNoEncontrado(AlumnoNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Alumno no encontrado",
                ex.getMessage());
    }

    @ExceptionHandler(UsernameExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameExists(UsernameExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Usuario duplicado",
                ex.getMessage(), "USERNAME_EXISTS");
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse>
    handleInsufficientBalance(InsufficientBalanceException ex){
        LOGGER.error("InsufficientBalanceException caught: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, "Saldo Insuficiente",
                ex.getMessage(), "INSUFFICIENT_BALANCE");
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex
    ) {
        LOGGER.error("InsufficientStockException caught: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.CONFLICT, "Stock insuficiente", ex.getMessage(),
                "STOCK_INSUFFICIENT");
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInventoryNotFound(
            InventoryNotFoundException ex
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, "Inventario no encontrado", ex.getMessage());
    }

    @ExceptionHandler(InventoryOperationException.class)
    public ResponseEntity<ErrorResponse> handleInventoryOperation(
            InventoryOperationException ex
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Operacion de inventario invalida",
                ex.getMessage(), "INVENTORY_OPERATION_INVALID");
    }

    @ExceptionHandler(ImageValidationException.class)
    public ResponseEntity<ErrorResponse> handleImageValidation(ImageValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validación de imagen",
                ex.getMessage());
    }

    @ExceptionHandler(PurchaseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePurchaseNotFound(PurchaseNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Compra no encontrada", ex.getMessage());
    }

    @ExceptionHandler(PurchaseAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePurchaseAccessDenied(
            PurchaseAccessDeniedException ex
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, "Compra no autorizada", ex.getMessage());
    }

    @ExceptionHandler(InvalidPurchaseStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPurchaseStatusTransition(
            InvalidPurchaseStatusTransitionException ex
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Estado de compra invalido",
                ex.getMessage(), "INVALID_PURCHASE_STATUS_TRANSITION");
    }

    @ExceptionHandler(PickupCutoffTimeExceededException.class)
    public ResponseEntity<ErrorResponse> handlePickupCutoffTimeExceeded(
            PickupCutoffTimeExceededException ex
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Horario de corte superado",
                ex.getMessage(), "CUTOFF_TIME_EXCEEDED");
    }

    @ExceptionHandler(PickupSlotCapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handlePickupSlotCapacityExceeded(
            PickupSlotCapacityExceededException ex
    ) {
        LOGGER.error("PickupSlotCapacityExceededException caught: {}",
                ex.getMessage(), ex);
        return buildResponse(HttpStatus.CONFLICT, "Cupo de recreo agotado",
                ex.getMessage(), "TIME_SLOT_CAPACITY_EXCEEDED");
    }

    @ExceptionHandler(PickupSlotRulesNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePickupSlotRulesNotFound(
            PickupSlotRulesNotFoundException ex
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, "Franja horaria no encontrada",
                ex.getMessage(), "TIME_SLOT_NOT_FOUND");
    }

    @ExceptionHandler(TimeSlotNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTimeSlotNotFound(TimeSlotNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Franja horaria no encontrada",
                ex.getMessage(), "TIME_SLOT_NOT_FOUND");
    }

    @ExceptionHandler(InvalidTimeSlotException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTimeSlot(InvalidTimeSlotException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Franja horaria invalida",
                ex.getMessage(), "INVALID_TIME_SLOT");
    }

    @ExceptionHandler(TimeSlotOverlapException.class)
    public ResponseEntity<ErrorResponse> handleTimeSlotOverlap(TimeSlotOverlapException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Franja horaria superpuesta",
                ex.getMessage(), "TIME_SLOT_OVERLAP");
    }

    @ExceptionHandler(TimeSlotInUseException.class)
    public ResponseEntity<ErrorResponse> handleTimeSlotInUse(TimeSlotInUseException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Franja horaria en uso",
                ex.getMessage(), "TIME_SLOT_IN_USE");
    }

    @ExceptionHandler(GradeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGradeNotFound(GradeNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Grado no encontrado",
                ex.getMessage(), "GRADE_NOT_FOUND");
    }

    @ExceptionHandler(InvalidGradeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGrade(InvalidGradeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Grado invalido",
                ex.getMessage(), "INVALID_GRADE");
    }

    @ExceptionHandler(GradeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleGradeAlreadyExists(
            GradeAlreadyExistsException ex
    ) {
        return buildResponse(HttpStatus.CONFLICT, "Grado duplicado",
                ex.getMessage(), "GRADE_ALREADY_EXISTS");
    }

    @ExceptionHandler(LevelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLevelNotFound(LevelNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Nivel no encontrado",
                ex.getMessage(), "LEVEL_NOT_FOUND");
    }

    @ExceptionHandler(SchoolNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSchoolNotFound(SchoolNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Colegio no encontrado",
                ex.getMessage(), "SCHOOL_NOT_FOUND");
    }

    @ExceptionHandler(ProductDailyCapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handleProductDailyCapacity(
            ProductDailyCapacityExceededException ex
    ) {
        LOGGER.error("ProductDailyCapacityExceededException caught: {}",
                ex.getMessage(), ex);
        return buildResponse(HttpStatus.CONFLICT, "Cupo diario de producto agotado",
                ex.getMessage(), "PRODUCT_DAILY_CAPACITY_EXCEEDED");
    }

    @ExceptionHandler(BuffetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBuffetNotFound(BuffetNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Buffet no encontrado",
                ex.getMessage(), "BUFFET_NOT_FOUND");
    }

    @ExceptionHandler(BuffetAlreadyExistsForSchoolException.class)
    public ResponseEntity<ErrorResponse> handleBuffetAlreadyExistsForSchool(
            BuffetAlreadyExistsForSchoolException ex
    ) {
        return buildResponse(HttpStatus.CONFLICT, "Buffet duplicado",
                ex.getMessage(), "BUFFET_ALREADY_EXISTS_FOR_SCHOOL");
    }

    @ExceptionHandler(InvalidChatMessageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidChatMessage(
            InvalidChatMessageException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Mensaje invalido", ex.getMessage());
    }

    @ExceptionHandler(ChatSessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatSessionNotFound(
            ChatSessionNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Sesion de chat no encontrada",
                ex.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Producto no encontrado",
                ex.getMessage());
    }

    @ExceptionHandler(TutorNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleTutorNoEncontrado(
            TutorNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Tutor no encontrado",
                ex.getMessage());
    }

    @ExceptionHandler(KiosqueroNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleKiosqueroNotFound(
            KiosqueroNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Kiosquero no encontrado",
                ex.getMessage());
    }

    @ExceptionHandler(KiosqueroPayoutConfigNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleKiosqueroPayoutConfigNotFound(
            KiosqueroPayoutConfigNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Configuración no encontrada",
                ex.getMessage());
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotificationNotFound(
            NotificationNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Notificación no encontrada",
                ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex) {
        String message = String.format("El parámetro '%s' recibió un valor inválido: '%s'",
                ex.getName(), ex.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST, "Parámetro inválido", message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>
    handleValidation(MethodArgumentNotValidException ex){
        LOGGER.error("MethodArgumentNotValidException caught: {}",
                ex.getMessage(), ex);
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("Datos inválidos");
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validación", details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        LOGGER.error("IllegalArgumentException caught: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, "Regla de Negocio Inválida",
                ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage());
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleOperationNotAllowed(
            OperationNotAllowedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso no encontrado",
                "El recurso solicitado no existe: " + ex.getResourcePath());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation
            (DataIntegrityViolationException ex) {
        LOGGER.error("Violación de integridad de datos: ", ex);

        String mensaje = "No se pudo guardar la información porque entra en conflicto "
                + "con los datos existentes.";
        return buildResponse(HttpStatus.CONFLICT, "Conflicto de Datos", mensaje);
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ErrorResponse> handleFileValidation(FileValidationException ex) {
        LOGGER.warn("Validación de archivo fallida: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Validación de Archivo", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex
    ) {
        LOGGER.warn("Tamaño de archivo excedido: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validación de Archivo",
                "El archivo supera el límite máximo de peso permitido (5MB)"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        LOGGER.error("Error no controlado: ", ex);
        
        String errorMessage = "Ocurrió un error inesperado. Por favor, intente más tarde.";
        if (ex.getMessage() != null && ex.getMessage().contains("Cognito")) {
            errorMessage = "Error inesperado: " + ex.getMessage();
            if (ex.getCause() != null) {
                errorMessage += " - Causa: " + ex.getCause().getMessage();
            }
        }

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", errorMessage);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                        String error, String message) {
        return buildResponse(status, error, message, defaultCode(status));
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            String code
    ) {
        LOGGER.warn(
                "Devolviendo error {} ({}): {} - RequestId: {}",
                status,
                code,
                message,
                currentRequestId()
        );
        ErrorResponse response = new ErrorResponse(code, error, message,
                LocalDateTime.now(), status.value(), currentRequestId());
        return ResponseEntity.status(status).body(response);
    }

    private String defaultCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case CONFLICT -> "CONFLICT";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case INTERNAL_SERVER_ERROR -> "INTERNAL_ERROR";
            default -> "ERROR";
        };
    }

    private String currentRequestId() {
        return RequestIdContext.currentRequestId().orElse(null);
    }

    private void addRequestId(Map<String, Object> body) {
        RequestIdContext.currentRequestId()
                .ifPresent(requestId -> body.put("requestId", requestId));
    }
}
