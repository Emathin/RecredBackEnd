package com.recred.backend.unit.application;

import com.recred.backend.application.GetHealthRestrictionsCatalog;
import com.recred.backend.application.GetHealthRestrictionsCatalogRequest;
import com.recred.backend.application.GetHealthRestrictionsCatalogResponse;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHealthRestrictionsCatalogTest {

    @Mock
    private ClasificacionSaludRepository repository;

    private GetHealthRestrictionsCatalog useCase;
    private UUID restrictionId;

    @BeforeEach
    void setUp() {
        useCase = new GetHealthRestrictionsCatalog(repository);
    }

    @Test
    void givenHealthRestrictionsExist_whenGettingCatalog_thenReturnsRestrictions() {
        givenHealthRestrictionsExist();

        final GetHealthRestrictionsCatalogResponse response = whenGetHealthRestrictionsCatalog();

        thenHealthRestrictionsWereReturned(response);
        thenHealthRestrictionsWereRequested();
    }

    private void givenHealthRestrictionsExist() {
        restrictionId = randomUUID();
        when(repository.findAll()).thenReturn(List.of(
                healthRestriction(restrictionId, "Sin TACC", true)
        ));
    }

    private GetHealthRestrictionsCatalogResponse whenGetHealthRestrictionsCatalog() {
        return useCase.execute(new GetHealthRestrictionsCatalogRequest());
    }

    private void thenHealthRestrictionsWereReturned(
            GetHealthRestrictionsCatalogResponse response
    ) {
        assertEquals(1, response.restrictions().size());
        assertEquals(restrictionId, response.restrictions().get(0).id());
        assertEquals("Sin TACC", response.restrictions().get(0).descripcion());
        assertEquals(true, response.restrictions().get(0).activo());
    }

    private void thenHealthRestrictionsWereRequested() {
        verify(repository).findAll();
    }

    private ClasificacionSalud healthRestriction(
            UUID requestedRestrictionId,
            String description,
            boolean active
    ) {
        ClasificacionSalud restriction = new ClasificacionSalud();
        restriction.setId(requestedRestrictionId);
        restriction.setDescripcion(description);
        restriction.setActivo(active);
        return restriction;
    }
}
