package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.infrastructure.entities.ListaPrecioProveedorEntity;
import com.recred.backend.infrastructure.entities.ProveedorEntity;
import com.recred.backend.infrastructure.persistence.ListaPrecioProveedorRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataListaPrecioProveedorRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListaPrecioProveedorRepositoryImplTest {

    @Mock
    private SpringDataListaPrecioProveedorRepository repository;

    private ListaPrecioProveedorRepositoryImpl adapter;

    @BeforeEach
    void setUp() {
        adapter = new ListaPrecioProveedorRepositoryImpl(repository);
    }

    @Test
    void saveShouldMapDomainToEntityAndBack() {
        UUID listId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        when(repository.save(any(ListaPrecioProveedorEntity.class)))
                .thenReturn(entity(listId, supplierId));

        ListaPrecioProveedor result = adapter.save(domain(listId, supplierId));

        ArgumentCaptor<ListaPrecioProveedorEntity> captor =
                ArgumentCaptor.forClass(ListaPrecioProveedorEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(supplierId, captor.getValue().getProveedor().getId());
        assertEquals("lista.pdf", captor.getValue().getNombreOriginal());
        assertEquals(listId, result.getId());
        assertEquals(supplierId, result.getProveedorId());
    }

    @Test
    void findMethodsShouldMapRepositoryResults() {
        UUID listId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        ListaPrecioProveedorEntity entity = entity(listId, supplierId);
        when(repository.findById(listId)).thenReturn(Optional.of(entity));
        when(repository.findByProveedorId(supplierId)).thenReturn(List.of(entity));

        Optional<ListaPrecioProveedor> found = adapter.findById(listId);
        List<ListaPrecioProveedor> bySupplier = adapter.findByProveedorId(supplierId);

        assertTrue(found.isPresent());
        assertEquals(listId, found.orElseThrow().getId());
        assertEquals(1, bySupplier.size());
        assertEquals(supplierId, bySupplier.get(0).getProveedorId());
    }

    private ListaPrecioProveedor domain(UUID listId, UUID supplierId) {
        ListaPrecioProveedor list = new ListaPrecioProveedor();
        list.setId(listId);
        list.setProveedorId(supplierId);
        list.setNombreOriginal("lista.pdf");
        list.setUrlArchivo("https://cdn.test/lista.pdf");
        list.setActiva(true);
        list.setCreadoEn(LocalDateTime.of(2026, 7, 4, 12, 0));
        return list;
    }

    private ListaPrecioProveedorEntity entity(UUID listId, UUID supplierId) {
        ProveedorEntity supplier = new ProveedorEntity();
        supplier.setId(supplierId);
        ListaPrecioProveedorEntity entity = new ListaPrecioProveedorEntity();
        entity.setId(listId);
        entity.setProveedor(supplier);
        entity.setNombreOriginal("lista.pdf");
        entity.setUrlArchivo("https://cdn.test/lista.pdf");
        entity.setActiva(true);
        entity.setFechaCarga(LocalDateTime.of(2026, 7, 4, 12, 0));
        return entity;
    }
}
