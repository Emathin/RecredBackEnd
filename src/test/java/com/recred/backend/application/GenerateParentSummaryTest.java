package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.entities.SummaryEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GenerateParentSummaryTest {

    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private SummaryRepository summaryRepository;
    @Mock
    private SpringDataUsuarioRepository usuarioRepository;

    @InjectMocks
    private GenerateParentSummary generateParentSummary;

    @Test
    void testExecute() {
        UUID tutorId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        tutor.setUsuario(usuario);
        
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(new UsuarioEntity()));
        
        generateParentSummary.execute(tutorId);
        
        verify(summaryRepository, times(1)).save(any(SummaryEntity.class));
    }
}
