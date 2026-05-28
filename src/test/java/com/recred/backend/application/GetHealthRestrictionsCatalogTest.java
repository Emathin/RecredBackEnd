package com.recred.backend.application;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
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
    private ClasificacionSaludRepository repository;

    @InjectMocks
    private GetHealthRestrictionsCatalog useCase;

    @Test
    void ejecutar_DeberiaRetornarListaDelRepositorio() {
        ClasificacionSalud item = new ClasificacionSalud();
        when(repository.findAll()).thenReturn(List.of(item));

        List<ClasificacionSalud> resultado = useCase.ejecutar();

        assertEquals(1, resultado.size());
        verify(repository).findAll();
    }
}