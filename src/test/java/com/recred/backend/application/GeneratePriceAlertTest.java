package com.recred.backend.application;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.User;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.domain.repositories.PriceAlertAIRepository.PriceAlertResult;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.domain.repositories.AISuggestionRepository;
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
    private AISuggestionRepository AISuggestionRepository;

    private GeneratePriceAlert generatePriceAlert;

    @BeforeEach
    void setUp() {
        generatePriceAlert = new GeneratePriceAlert(
                studentAIProfileRepository,
                priceAlertAIRepository,
                AISuggestionRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void executeAndSavePriceAlertsForAffectedStudentsWhenPriceChanges() {
        Product product = product();
        BigDecimal oldPrice = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("120.00");
        UUID buffetId = UUID.randomUUID();
        UUID tutorUserId = UUID.randomUUID();
        Student student = studentWithTutorUser(tutorUserId);
        givenAffectedStudentWithoutRecentAlert(product, student);
        givenAiResult(student, product, oldPrice, newPrice);

        generatePriceAlert.execute(product, oldPrice, newPrice, buffetId);

        PriceAlertSuggestion saved = capturedSuggestion();

        assertEquals("Aumento de price", saved.title());
        assertEquals("El alfajor subio", saved.message());
        assertEquals(product.getId(), saved.productId());
        assertEquals("Alfajor", saved.productName());
        assertEquals(student.getId(), saved.studentId());
        assertEquals(buffetId, saved.buffetId());
        assertEquals(oldPrice, saved.oldPrice());
        assertEquals(newPrice, saved.newPrice());
        assertEquals("Impacto inflacionario", saved.aiReason());
        assertTrue(saved.tutorUserIds().contains(tutorUserId));
    }

    @Test
    void doNothingWhenNoStudentsHaveTheProductAsFavorite() {
        Product product = product();
        BigDecimal oldPrice = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("120.00");

        when(studentAIProfileRepository.findStudentsWithFavoriteProduct(product.getId()))
                .thenReturn(List.of());

        generatePriceAlert.execute(product, oldPrice, newPrice, null);

        verify(priceAlertAIRepository, never()).analyzeImpact(any(), any(), any(), any());
        verify(AISuggestionRepository, never()).savePriceAlert(any());
    }

    @Test
    void skipStudentWhenRecentPriceAlertAlreadyExists() {
        Product product = product();
        Student student = studentWithTutorUser(UUID.randomUUID());

        when(studentAIProfileRepository.findStudentsWithFavoriteProduct(product.getId()))
                .thenReturn(List.of(student));
        when(AISuggestionRepository.hasRecentPriceAlert(
                student.getId(),
                product.getId(),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(24)
        ))
                .thenReturn(true);

        generatePriceAlert.execute(
                product,
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                null
        );

        verify(priceAlertAIRepository, never()).analyzeImpact(any(), any(), any(), any());
        verify(AISuggestionRepository, never()).savePriceAlert(any());
    }

    private Product product() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Alfajor");
        return product;
    }

    private void givenAffectedStudentWithoutRecentAlert(Product product, Student student) {
        when(studentAIProfileRepository.findStudentsWithFavoriteProduct(product.getId()))
                .thenReturn(List.of(student));
        when(AISuggestionRepository.hasRecentPriceAlert(
                student.getId(),
                product.getId(),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(24)
        ))
                .thenReturn(false);
    }

    private void givenAiResult(
            Student student,
            Product product,
            BigDecimal oldPrice,
            BigDecimal newPrice
    ) {
        when(priceAlertAIRepository.analyzeImpact(
                eq(student),
                eq(product),
                eq(oldPrice),
                eq(newPrice)
        ))
                .thenReturn(Optional.of(new PriceAlertResult(
                        "Aumento de price",
                        "El alfajor subio",
                        "Impacto inflacionario"
                )));
    }

    private PriceAlertSuggestion capturedSuggestion() {
        ArgumentCaptor<PriceAlertSuggestion> captor =
                ArgumentCaptor.forClass(PriceAlertSuggestion.class);
        verify(AISuggestionRepository).savePriceAlert(captor.capture());
        return captor.getValue();
    }

    private Student studentWithTutorUser(UUID tutorUserId) {
        User user = new User();
        user.setId(tutorUserId);

        Tutor tutor = new Tutor();
        tutor.setUser(user);

        Student student = new Student();
        student.setId(UUID.randomUUID());
        student.getTutors().add(tutor);
        return student;
    }
}
