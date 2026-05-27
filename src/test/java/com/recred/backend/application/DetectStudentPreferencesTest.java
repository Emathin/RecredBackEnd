package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.CompraRepository;
import com.recred.backend.domain.repositories.InventarioRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private CompraRepository compraRepository;
    @Mock
    private InventarioRepository inventarioRepository;

    private DetectStudentPreferences detectStudentPreferences;

    @BeforeEach
    void setUp() {
        detectStudentPreferences = new DetectStudentPreferences(
                alumnoRepository,
                preferenceAIRepository,
                sugerenciaIARepository,
                compraRepository,
                inventarioRepository
        );
    }

    @Test
    void ejecuta_Correctamente_CuandoSeDetectanPreferencias() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(studentId);

        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        List<Producto> stock = List.of(producto);

        PreferenceAIRepository.PreferenciaDetectada preference = new PreferenceAIRepository.PreferenciaDetectada(
                "Title", "Message", producto.getId().toString(), "Reason"
        );

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(compraRepository.findLastPurchasesByAlumno(studentId, 20)).thenReturn(Collections.emptyList());
        when(inventarioRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(stock);
        when(preferenceAIRepository.analizarPreferencias(alumno, Collections.emptyList(), stock))
                .thenReturn(Optional.of(preference));

        detectStudentPreferences.execute(studentId, buffetId);

        ArgumentCaptor<SugerenciaIAEntity> captor = ArgumentCaptor.forClass(SugerenciaIAEntity.class);
        verify(sugerenciaIARepository).guardar(captor.capture());

        SugerenciaIAEntity saved = captor.getValue();
        assertEquals(TipoSugerenciaIA.PREFERENCIA_DETECTADA, saved.getTipo());
        assertEquals("Title", saved.getTitulo());
        assertEquals("Message", saved.getMensaje());
        assertNull(saved.getAceptada());
        assertEquals(producto.getId().toString(), saved.getMetadata().get("productoId"));
        assertEquals(studentId, saved.getAlumnoEntity().getId());
    }

    @Test
    void aborta_Temprano_SiNoHayStockDisponible() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(studentId);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(compraRepository.findLastPurchasesByAlumno(studentId, 20)).thenReturn(Collections.emptyList());
        when(inventarioRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(Collections.emptyList());

        detectStudentPreferences.execute(studentId, buffetId);

        verify(preferenceAIRepository, never()).analizarPreferencias(alumno, Collections.emptyList(), Collections.emptyList());
        verify(sugerenciaIARepository, never()).guardar(any());
    }

    @Test
    void noHaceNada_SiLaIA_NoDetectaPreferencias() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(studentId);
        Producto producto = new Producto();
        List<Producto> stock = List.of(producto);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(compraRepository.findLastPurchasesByAlumno(studentId, 20)).thenReturn(Collections.emptyList());
        when(inventarioRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(stock);
        when(preferenceAIRepository.analizarPreferencias(alumno, Collections.emptyList(), stock)).thenReturn(Optional.empty());

        detectStudentPreferences.execute(studentId, buffetId);

        verify(sugerenciaIARepository, never()).guardar(any());
    }

    @Test
    void lanzaExcepcion_SiElAlumnoNoExiste() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> detectStudentPreferences.execute(studentId, buffetId));
    }
}
