package com.recred.backend.unit.application;

import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.application.GetHealthRestrictionsCatalogRequest;
import com.recred.backend.application.GetHealthRestrictionsCatalogResponse;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHealthRestrictionsCatalogTest {

    @Mock
    private ClasificacionSaludRepository repository;

    @InjectMocks
    private GetHealthRestrictionsCatalog useCase;

    @Test
    void execute_DeberiaRetornarListaDelRepositorio() {
        ClasificacionSalud item = new ClasificacionSalud();
        item.setDescripcion("Sin TACC");
        when(repository.findAll()).thenReturn(List.of(item));

        GetHealthRestrictionsCatalogResponse resultado =
                useCase.execute(new GetHealthRestrictionsCatalogRequest());

        assertEquals(1, resultado.restrictions().size());
        assertEquals("Sin TACC", resultado.restrictions().get(0).descripcion());
        verify(repository).findAll();
    }
}
