package com.recred.backend.unit.application;

import com.recred.backend.application.GeneratePriceAlert;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratePriceAlertTest {

    private static final Instant NOW = Instant.parse("2026-05-29T12:00:00Z");

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
        BigDecimal oldPrice = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("120.00");
        UUID buffetId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        Alumno alumno = studentWithTutorUser(tutorUserId, studentUserId);
        givenAffectedStudentWithoutRecentAlert(producto, alumno);
        givenAiResult(alumno, producto, oldPrice, newPrice);

        generatePriceAlert.execute(producto, oldPrice, newPrice, buffetId);

        PriceAlertSuggestion saved = capturedSuggestion();

        assertEquals("Aumento de precio", saved.title());
        assertEquals("El alfajor subio", saved.message());
        assertEquals(producto.getId(), saved.productId());
        assertEquals("Alfajor", saved.productName());
        assertEquals(alumno.getId(), saved.studentId());
        assertEquals(studentUserId, saved.studentUserId());
        assertEquals(buffetId, saved.buffetId());
        assertEquals(oldPrice, saved.oldPrice());
        assertEquals(newPrice, saved.newPrice());
        assertEquals("Impacto inflacionario", saved.aiReason());
        assertTrue(saved.tutorUserIds().contains(tutorUserId));
    }

    @Test
    void doNothingWhenNoStudentsHaveTheProductAsFavorite() {
        Producto producto = product();
        BigDecimal oldPrice = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("120.00");

        when(studentAIProfileRepository.findStudentsWithFavoriteProduct(producto.getId()))
                .thenReturn(List.of());

        generatePriceAlert.execute(producto, oldPrice, newPrice, null);

        verify(priceAlertAIRepository, never()).analyzeImpact(any(), any(), any(), any());
        verify(sugerenciaIARepository, never()).savePriceAlert(any());
    }

    @Test
    void skipStudentWhenRecentPriceAlertAlreadyExists() {
        Producto producto = product();
        Alumno alumno = studentWithTutorUser(UUID.randomUUID(), UUID.randomUUID());

        when(studentAIProfileRepository.findStudentsWithFavoriteProduct(producto.getId()))
                .thenReturn(List.of(alumno));
        when(sugerenciaIARepository.hasRecentPriceAlert(
                alumno.getId(),
                producto.getId(),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(24)
        ))
                .thenReturn(true);

        generatePriceAlert.execute(
                producto,
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                null
        );

        verify(priceAlertAIRepository, never()).analyzeImpact(any(), any(), any(), any());
        verify(sugerenciaIARepository, never()).savePriceAlert(any());
    }

    @Test
    void execute_WhenAiResultIsEmpty_ShouldNotSaveAlert() {
        Producto producto = product();
        Alumno alumno = studentWithTutorUser(UUID.randomUUID(), UUID.randomUUID());
        BigDecimal oldPrice = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("120.00");
        givenAffectedStudentWithoutRecentAlert(producto, alumno);
        when(priceAlertAIRepository.analyzeImpact(alumno, producto, oldPrice, newPrice))
                .thenReturn(Optional.empty());

        generatePriceAlert.execute(producto, oldPrice, newPrice, null);

        verify(sugerenciaIARepository, never()).savePriceAlert(any());
    }

    @Test
    void execute_WhenTutorsHaveNoUserId_ShouldSaveAlertWithoutTutorUsers() {
        Producto producto = product();
        BigDecimal oldPrice = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("120.00");
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.getTutores().add(new Tutor());
        Tutor tutorWithoutUserId = new Tutor();
        tutorWithoutUserId.setUsuario(new Usuario());
        alumno.getTutores().add(tutorWithoutUserId);
        givenAffectedStudentWithoutRecentAlert(producto, alumno);
        givenAiResult(alumno, producto, oldPrice, newPrice);

        generatePriceAlert.execute(producto, oldPrice, newPrice, buffetId);

        PriceAlertSuggestion saved = capturedSuggestion();
        assertTrue(saved.tutorUserIds().isEmpty());
    }

    private Producto product() {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Alfajor");
        return producto;
    }

    private void givenAffectedStudentWithoutRecentAlert(Producto producto, Alumno alumno) {
        when(studentAIProfileRepository.findStudentsWithFavoriteProduct(producto.getId()))
                .thenReturn(List.of(alumno));
        when(sugerenciaIARepository.hasRecentPriceAlert(
                alumno.getId(),
                producto.getId(),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(24)
        ))
                .thenReturn(false);
    }

    private void givenAiResult(
            Alumno alumno,
            Producto producto,
            BigDecimal oldPrice,
            BigDecimal newPrice
    ) {
        when(priceAlertAIRepository.analyzeImpact(
                eq(alumno),
                eq(producto),
                eq(oldPrice),
                eq(newPrice)
        ))
                .thenReturn(Optional.of(new PriceAlertResult(
                        "Aumento de precio",
                        "El alfajor subio",
                        "Impacto inflacionario"
                )));
    }

    private PriceAlertSuggestion capturedSuggestion() {
        ArgumentCaptor<PriceAlertSuggestion> captor =
                ArgumentCaptor.forClass(PriceAlertSuggestion.class);
        verify(sugerenciaIARepository).savePriceAlert(captor.capture());
        return captor.getValue();
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
}
