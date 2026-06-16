package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.gemini.GeminiParentSummaryAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GenerateParentSummaryTest {

    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private SummaryRepository summaryRepository;
    @Mock
    private GeminiParentSummaryAnalyzer geminiParentSummaryAnalyzer;

    @InjectMocks
    private GenerateParentSummary generateParentSummary;

    @Test
    void testExecute() {
        UUID usuarioId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        tutor.setUsuario(usuario);
        
        when(tutorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(tutor));
        when(summaryRepository.save(any(Summary.class))).thenReturn(new Summary());
        
        generateParentSummary.execute(usuarioId, LocalDateTime.now().minusDays(7), LocalDateTime.now());
        
        verify(summaryRepository, times(1)).save(any(Summary.class));
    }
}
