package com.recred.backend.unit.application;

import com.recred.backend.application.GeneratePriceAlert;
import com.recred.backend.application.GeneratePriceAlertRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.domain.repositories.PriceAlertAIRepository.PriceAlertResult;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratePriceAlertTest {

    private static final Instant NOW = Instant.parse("2026-05-29T12:00:00Z");
    private static final BigDecimal OLD_PRICE = new BigDecimal("100.00");
    private static final BigDecimal NEW_PRICE = new BigDecimal("120.00");
    private static final String ALERT_TITLE = "Aumento de precio";
    private static final String ALERT_MESSAGE = "El alfajor subio";
    private static final String AI_REASON = "Impacto inflacionario";
    private static final String PRODUCT_NAME = "Alfajor";

    @Mock
    private StudentAIProfileRepository studentAIProfileRepository;
    @Mock
    private PriceAlertAIRepository priceAlertAIRepository;
    @Mock
    private SugerenciaIARepository sugerenciaIARepository;

    private GeneratePriceAlert generatePriceAlert;

    @BeforeEach
    void setUp() {
        generatePriceAlert = new GeneratePriceAlert(
                studentAIProfileRepository,
                priceAlertAIRepository,
                sugerenciaIARepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void executeAndSavePriceAlertsForAffectedStudentsWhenPriceChanges() {
        Producto producto = product();
        UUID buffetId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        Alumno alumno = studentWithTutorUser(tutorUserId, studentUserId);
        givenAffectedStudentWithoutRecentAlert(producto, alumno);
        givenAiResult(alumno, producto);

        generatePriceAlert.execute(request(producto, buffetId));

        PriceAlertSuggestion saved = capturedSuggestion();
        assertSavedPriceAlert(saved, producto, alumno, studentUserId, buffetId);
        assertTrue(saved.tutorUserIds().contains(tutorUserId));
    }

    @Test
    void doNothingWhenNoStudentsHaveTheProductAsFavorite() {
        Producto producto = product();
        givenNoAffectedStudents(producto);

        generatePriceAlert.execute(requestWithoutBuffet(producto));

        assertNoAlertAnalyzed();
        assertNoPriceAlertSaved();
    }

    @Test
    void skipStudentWhenRecentPriceAlertAlreadyExists() {
        Producto producto = product();
        Alumno alumno = studentWithTutorUser(UUID.randomUUID(), UUID.randomUUID());
        givenAffectedStudentWithRecentAlert(producto, alumno);

        generatePriceAlert.execute(requestWithoutBuffet(producto));

        assertNoAlertAnalyzed();
        assertNoPriceAlertSaved();
    }

    @Test
    void execute_WhenAiResultIsEmpty_ShouldNotSaveAlert() {
        Producto producto = product();
        Alumno alumno = studentWithTutorUser(UUID.randomUUID(), UUID.randomUUID());
        givenAffectedStudentWithoutRecentAlert(producto, alumno);
        givenAiResultIsEmpty(alumno, producto);

        generatePriceAlert.execute(requestWithoutBuffet(producto));

        assertNoPriceAlertSaved();
    }

    @Test
    void execute_WhenTutorsHaveNoUserId_ShouldSaveAlertWithoutTutorUsers() {
        Producto producto = product();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = studentWithTutorsWithoutUserIds();
        givenAffectedStudentWithoutRecentAlert(producto, alumno);
        givenAiResult(alumno, producto);

        generatePriceAlert.execute(request(producto, buffetId));

        PriceAlertSuggestion saved = capturedSuggestion();
        assertTrue(saved.tutorUserIds().isEmpty());
    }

    @Test
    void execute_WhenProductIsMissing_ShouldFailBeforeReadingStudents() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generatePriceAlert.execute(requestWithoutBuffet(null))
        );

        assertEquals("El producto es obligatorio", exception.getMessage());
        assertNoAffectedStudentsRead();
        assertNoAlertAnalyzed();
        assertNoPriceAlertSaved();
    }

    private Producto product() {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre(PRODUCT_NAME);
        return producto;
    }

    private GeneratePriceAlertRequest requestWithoutBuffet(Producto producto) {
        return request(producto, null);
    }

    private GeneratePriceAlertRequest request(Producto producto, UUID buffetId) {
        return request(producto, OLD_PRICE, NEW_PRICE, buffetId);
    }

    private GeneratePriceAlertRequest request(
            Producto producto,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            UUID buffetId
    ) {
        return new GeneratePriceAlertRequest(producto, oldPrice, newPrice, buffetId);
    }

    private void givenNoAffectedStudents(Producto producto) {
        when(studentAIProfileRepository.findStudentsWithFavoriteProduct(producto.getId()))
                .thenReturn(List.of());
    }

    private void givenAffectedStudentWithoutRecentAlert(Producto producto, Alumno alumno) {
        when(studentAIProfileRepository.findStudentsWithFavoriteProduct(producto.getId()))
                .thenReturn(List.of(alumno));
        when(sugerenciaIARepository.hasRecentPriceAlert(
                alumno.getId(),
                producto.getId(),
                recentAlertLimit()
        ))
                .thenReturn(false);
    }

    private void givenAffectedStudentWithRecentAlert(Producto producto, Alumno alumno) {
        when(studentAIProfileRepository.findStudentsWithFavoriteProduct(producto.getId()))
                .thenReturn(List.of(alumno));
        when(sugerenciaIARepository.hasRecentPriceAlert(
                alumno.getId(),
                producto.getId(),
                recentAlertLimit()
        ))
                .thenReturn(true);
    }

    private void givenAiResult(Alumno alumno, Producto producto) {
        when(priceAlertAIRepository.analyzeImpact(
                eq(alumno),
                eq(producto),
                eq(OLD_PRICE),
                eq(NEW_PRICE)
        ))
                .thenReturn(Optional.of(new PriceAlertResult(
                        ALERT_TITLE,
                        ALERT_MESSAGE,
                        AI_REASON
                )));
    }

    private void givenAiResultIsEmpty(Alumno alumno, Producto producto) {
        when(priceAlertAIRepository.analyzeImpact(alumno, producto, OLD_PRICE, NEW_PRICE))
                .thenReturn(Optional.empty());
    }

    private PriceAlertSuggestion capturedSuggestion() {
        ArgumentCaptor<PriceAlertSuggestion> captor =
                ArgumentCaptor.forClass(PriceAlertSuggestion.class);
        verify(sugerenciaIARepository).savePriceAlert(captor.capture());
        return captor.getValue();
    }

    private void assertSavedPriceAlert(
            PriceAlertSuggestion saved,
            Producto producto,
            Alumno alumno,
            UUID studentUserId,
            UUID buffetId
    ) {
        assertEquals(ALERT_TITLE, saved.title());
        assertEquals(ALERT_MESSAGE, saved.message());
        assertEquals(producto.getId(), saved.productId());
        assertEquals(PRODUCT_NAME, saved.productName());
        assertEquals(alumno.getId(), saved.studentId());
        assertEquals(studentUserId, saved.studentUserId());
        assertEquals(buffetId, saved.buffetId());
        assertEquals(OLD_PRICE, saved.oldPrice());
        assertEquals(NEW_PRICE, saved.newPrice());
        assertEquals(AI_REASON, saved.aiReason());
    }

    private void assertNoAffectedStudentsRead() {
        verify(studentAIProfileRepository, never()).findStudentsWithFavoriteProduct(any());
    }

    private void assertNoAlertAnalyzed() {
        verify(priceAlertAIRepository, never()).analyzeImpact(any(), any(), any(), any());
    }

    private void assertNoPriceAlertSaved() {
        verify(sugerenciaIARepository, never()).savePriceAlert(any());
    }

    private LocalDateTime recentAlertLimit() {
        return LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(24);
    }

    private Alumno studentWithTutorUser(UUID tutorUserId, UUID studentUserId) {
        Usuario usuario = new Usuario();
        usuario.setId(tutorUserId);

        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);

        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuarioId(studentUserId);
        alumno.getTutores().add(tutor);
        return alumno;
    }

    private Alumno studentWithTutorsWithoutUserIds() {
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.getTutores().add(new Tutor());

        Tutor tutorWithoutUserId = new Tutor();
        tutorWithoutUserId.setUsuario(new Usuario());
        alumno.getTutores().add(tutorWithoutUserId);

        return alumno;
    }
}
