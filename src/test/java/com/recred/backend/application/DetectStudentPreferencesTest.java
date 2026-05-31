package com.recred.backend.application;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.AISuggestionRepository;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectStudentPreferencesTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private PreferenceAIRepository preferenceAIRepository;
    @Mock
    private AISuggestionRepository AISuggestionRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    private DetectStudentPreferences detectStudentPreferences;

    @BeforeEach
    void setUp() {
        detectStudentPreferences = new DetectStudentPreferences(
                studentRepository,
                preferenceAIRepository,
                AISuggestionRepository,
                purchaseRepository,
                inventoryRepository
        );
    }

    @Test
    void executeAndSaveAISuggestionWhenValidPreferenceIsDetected() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Student student = new Student();
        student.setId(studentId);

        Product product = new Product();
        product.setId(UUID.randomUUID());
        List<Product> stock = List.of(product);

        List<PreferenceAIRepository.PurchaseSummary> historial = List.of(
                new PreferenceAIRepository.PurchaseSummary("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.PurchaseSummary("B", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.PurchaseSummary("C", "C", java.time.LocalDateTime.now())
        );

        PreferenceAIRepository.DetectedPreference preference = new PreferenceAIRepository.DetectedPreference(
                "Title", "Message", product.getId().toString(), "Reason"
        );

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(purchaseRepository.findLastPurchasesByStudent(studentId, 20)).thenReturn(historial);
        when(inventoryRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(stock);
        when(preferenceAIRepository.analizarPreferencias(student, historial, stock))
                .thenReturn(Optional.of(preference));

        detectStudentPreferences.execute(studentId, buffetId);

        ArgumentCaptor<AISuggestionEntity> captor = ArgumentCaptor.forClass(AISuggestionEntity.class);
        verify(AISuggestionRepository).save(captor.capture());

        AISuggestionEntity saved = captor.getValue();
        assertEquals(IaSuggestionType.PREFERENCIA_DETECTADA, saved.getType());
        assertEquals(product.getId(), saved.getProductEntity().getId());
        assertEquals(studentId, saved.getStudentEntity().getId());
        assertEquals(buffetId, saved.getBuffetEntity().getId());
    }

    @Test
    void abortDetectionWhenInsufficientPurchaseHistory() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Student student = new Student();
        student.setId(studentId);

        List<PreferenceAIRepository.PurchaseSummary> historial = List.of(
                new PreferenceAIRepository.PurchaseSummary("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.PurchaseSummary("B", "C", java.time.LocalDateTime.now())
        );

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(purchaseRepository.findLastPurchasesByStudent(studentId, 20)).thenReturn(historial);

        Optional<PreferenceAIRepository.DetectedPreference> result =
                detectStudentPreferences.execute(studentId, buffetId);

        assertTrue(result.isEmpty());
        verify(preferenceAIRepository, never()).analizarPreferencias(any(), any(), any());
    }

    @Test
    void discardAIResultWhenProductIdIsNotInAvailableStock() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Student student = new Student();
        student.setId(studentId);

        Product productEnStock = new Product();
        productEnStock.setId(UUID.randomUUID());
        List<Product> stock = List.of(productEnStock);

        List<PreferenceAIRepository.PurchaseSummary> historial = List.of(
                new PreferenceAIRepository.PurchaseSummary("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.PurchaseSummary("B", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.PurchaseSummary("C", "C", java.time.LocalDateTime.now())
        );

        PreferenceAIRepository.DetectedPreference preference = new PreferenceAIRepository.DetectedPreference(
                "Title", "Message", UUID.randomUUID().toString(), "Reason"
        );

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(purchaseRepository.findLastPurchasesByStudent(studentId, 20)).thenReturn(historial);
        when(inventoryRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(stock);
        when(preferenceAIRepository.analizarPreferencias(student, historial, stock))
                .thenReturn(Optional.of(preference));

        Optional<PreferenceAIRepository.DetectedPreference> result =
                detectStudentPreferences.execute(studentId, buffetId);

        assertTrue(result.isEmpty());
        verify(AISuggestionRepository, never()).save(any());
    }

    @Test
    void abortDetectionWhenNoStockIsAvailableInBuffet() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Student student = new Student();
        student.setId(studentId);

        List<PreferenceAIRepository.PurchaseSummary> historial = List.of(
                new PreferenceAIRepository.PurchaseSummary("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.PurchaseSummary("B", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.PurchaseSummary("C", "C", java.time.LocalDateTime.now())
        );

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(purchaseRepository.findLastPurchasesByStudent(studentId, 20)).thenReturn(historial);
        when(inventoryRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(Collections.emptyList());

        detectStudentPreferences.execute(studentId, buffetId);

        verify(preferenceAIRepository, never()).analizarPreferencias(any(), any(), any());
        verify(AISuggestionRepository, never()).save(any());
    }

    @Test
    void doNothingWhenAINoPreferenceDetected() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Student student = new Student();
        student.setId(studentId);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        List<Product> stock = List.of(product);

        List<PreferenceAIRepository.PurchaseSummary> historial = List.of(
                new PreferenceAIRepository.PurchaseSummary("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.PurchaseSummary("B", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.PurchaseSummary("C", "C", java.time.LocalDateTime.now())
        );

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(purchaseRepository.findLastPurchasesByStudent(studentId, 20)).thenReturn(historial);
        when(inventoryRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(stock);
        when(preferenceAIRepository.analizarPreferencias(student, historial, stock)).thenReturn(Optional.empty());

        detectStudentPreferences.execute(studentId, buffetId);

        verify(AISuggestionRepository, never()).save(any());
    }

    @Test
    void throwExceptionWhenTargetStudentDoesNotExist() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> detectStudentPreferences.execute(studentId, buffetId));
    }

    @Test
    void rethrowExceptionWhenUnexpectedErrorOccursInProcess() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        when(studentRepository.findById(studentId)).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> detectStudentPreferences.execute(studentId, buffetId));
    }
}
