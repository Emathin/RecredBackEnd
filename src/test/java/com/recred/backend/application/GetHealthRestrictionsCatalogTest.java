package com.recred.backend.application;

import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.repositories.HealthClassificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetHealthRestrictionsCatalogTest {

    @Mock
    private HealthClassificationRepository repository;

    @InjectMocks
    private GetHealthRestrictionsCatalog useCase;

    @Test
    void ejecutar_DeberiaRetornarListaDelRepositorio() {
        HealthClassification item = new HealthClassification();
        when(repository.findAll()).thenReturn(List.of(item));

        List<HealthClassification> resultado = useCase.ejecutar();

        assertEquals(1, resultado.size());
        verify(repository).findAll();
    }
}