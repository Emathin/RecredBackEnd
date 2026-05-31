package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
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
    private AlumnoRepository alumnoRepository;
    @Mock
    private PreferenceAIRepository preferenceAIRepository;
    @Mock
    private SugerenciaIARepository sugerenciaIARepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    private DetectStudentPreferences detectStudentPreferences;

    @BeforeEach
    void setUp() {
        detectStudentPreferences = new DetectStudentPreferences(
                alumnoRepository,
                preferenceAIRepository,
                sugerenciaIARepository,
                purchaseRepository,
                inventoryRepository
        );
    }

    @Test
    void executeAndSaveAISuggestionWhenValidPreferenceIsDetected() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(studentId);

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        List<Producto> stock = List.of(producto);

        List<PreferenceAIRepository.CompraResumen> historial = List.of(
                new PreferenceAIRepository.CompraResumen("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.CompraResumen("B", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.CompraResumen("C", "C", java.time.LocalDateTime.now())
        );

        PreferenceAIRepository.PreferenciaDetectada preference = new PreferenceAIRepository.PreferenciaDetectada(
                "Title", "Message", producto.getId().toString(), "Reason"
        );

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(purchaseRepository.findLastPurchasesByAlumno(studentId, 20)).thenReturn(historial);
        when(inventoryRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(stock);
        when(preferenceAIRepository.analizarPreferencias(alumno, historial, stock))
                .thenReturn(Optional.of(preference));

        detectStudentPreferences.execute(studentId, buffetId);

        ArgumentCaptor<SugerenciaIAEntity> captor = ArgumentCaptor.forClass(SugerenciaIAEntity.class);
        verify(sugerenciaIARepository).savePreferenceDetection(captor.capture());

        SugerenciaIAEntity saved = captor.getValue();
        assertEquals(TipoSugerenciaIA.PREFERENCIA_DETECTADA, saved.getTipo());
        assertEquals(producto.getId(), saved.getProductoEntity().getId());
        assertEquals(studentId, saved.getAlumnoEntity().getId());
        assertEquals(buffetId, saved.getBuffetEntity().getId());
    }

    @Test
    void abortDetectionWhenInsufficientPurchaseHistory() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(studentId);

        List<PreferenceAIRepository.CompraResumen> historial = List.of(
                new PreferenceAIRepository.CompraResumen("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.CompraResumen("B", "C", java.time.LocalDateTime.now())
        );

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(purchaseRepository.findLastPurchasesByAlumno(studentId, 20)).thenReturn(historial);

        Optional<PreferenceAIRepository.PreferenciaDetectada> result =
                detectStudentPreferences.execute(studentId, buffetId);

        assertTrue(result.isEmpty());
        verify(preferenceAIRepository, never()).analizarPreferencias(any(), any(), any());
    }

    @Test
    void discardAIResultWhenProductIdIsNotInAvailableStock() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(studentId);

        Producto productoEnStock = new Producto();
        productoEnStock.setId(UUID.randomUUID());
        List<Producto> stock = List.of(productoEnStock);

        List<PreferenceAIRepository.CompraResumen> historial = List.of(
                new PreferenceAIRepository.CompraResumen("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.CompraResumen("B", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.CompraResumen("C", "C", java.time.LocalDateTime.now())
        );

        PreferenceAIRepository.PreferenciaDetectada preference = new PreferenceAIRepository.PreferenciaDetectada(
                "Title", "Message", UUID.randomUUID().toString(), "Reason"
        );

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(purchaseRepository.findLastPurchasesByAlumno(studentId, 20)).thenReturn(historial);
        when(inventoryRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(stock);
        when(preferenceAIRepository.analizarPreferencias(alumno, historial, stock))
                .thenReturn(Optional.of(preference));

        Optional<PreferenceAIRepository.PreferenciaDetectada> result =
                detectStudentPreferences.execute(studentId, buffetId);

        assertTrue(result.isEmpty());
        verify(sugerenciaIARepository, never()).savePreferenceDetection(any());
    }

    @Test
    void abortDetectionWhenNoStockIsAvailableInBuffet() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(studentId);

        List<PreferenceAIRepository.CompraResumen> historial = List.of(
                new PreferenceAIRepository.CompraResumen("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.CompraResumen("B", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.CompraResumen("C", "C", java.time.LocalDateTime.now())
        );

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(purchaseRepository.findLastPurchasesByAlumno(studentId, 20)).thenReturn(historial);
        when(inventoryRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(Collections.emptyList());

        detectStudentPreferences.execute(studentId, buffetId);

        verify(preferenceAIRepository, never()).analizarPreferencias(any(), any(), any());
        verify(sugerenciaIARepository, never()).savePreferenceDetection(any());
    }

    @Test
    void doNothingWhenAINoPreferenceDetected() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(studentId);
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        List<Producto> stock = List.of(producto);

        List<PreferenceAIRepository.CompraResumen> historial = List.of(
                new PreferenceAIRepository.CompraResumen("A", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.CompraResumen("B", "C", java.time.LocalDateTime.now()),
                new PreferenceAIRepository.CompraResumen("C", "C", java.time.LocalDateTime.now())
        );

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(purchaseRepository.findLastPurchasesByAlumno(studentId, 20)).thenReturn(historial);
        when(inventoryRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(stock);
        when(preferenceAIRepository.analizarPreferencias(alumno, historial, stock)).thenReturn(Optional.empty());

        detectStudentPreferences.execute(studentId, buffetId);

        verify(sugerenciaIARepository, never()).savePreferenceDetection(any());
    }

    @Test
    void throwExceptionWhenTargetStudentDoesNotExist() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> detectStudentPreferences.execute(studentId, buffetId));
    }

    @Test
    void rethrowExceptionWhenUnexpectedErrorOccursInProcess() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        when(alumnoRepository.findById(studentId)).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> detectStudentPreferences.execute(studentId, buffetId));
    }
}
