package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KiosqueroEntityTest {

    @Test
    void fromDomainShouldMapBuffetProxy() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 15, 11, 0);
        Kiosquero kiosquero = new Kiosquero();
        kiosquero.setId(kiosqueroId);
        kiosquero.setCuit("20304050607");
        kiosquero.setCreadoEn(createdAt);
        kiosquero.setActualizadoEn(updatedAt);
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        kiosquero.setBuffet(buffet);

        KiosqueroEntity result = KiosqueroEntity.fromDomain(kiosquero);

        assertEquals(kiosqueroId, result.getId());
        assertEquals("20304050607", result.getCuit());
        assertEquals(createdAt, result.getCreadoEn());
        assertEquals(updatedAt, result.getActualizadoEn());
        assertEquals(buffetId, result.getBuffetEntity().getId());
    }

    @Test
    void fromDomainShouldHandleNulls() {
        Kiosquero kiosquero = new Kiosquero();
        kiosquero.setId(UUID.randomUUID());

        KiosqueroEntity result = KiosqueroEntity.fromDomain(kiosquero);

        assertNull(result.getBuffetEntity());
        assertNull(KiosqueroEntity.fromDomain(null));
    }

    @Test
    void toDomainShouldMapUserAndBuffet() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        KiosqueroEntity entity = new KiosqueroEntity();
        entity.setId(kiosqueroId);
        entity.setCuit("20304050607");
        entity.setUsuario(userEntity());
        entity.setBuffetEntity(buffetEntity(buffetId));

        Kiosquero result = entity.toDomain();

        assertEquals(kiosqueroId, result.getId());
        assertEquals("20304050607", result.getCuit());
        assertEquals("Kiosquero", result.getUsuario().getNombre());
        assertEquals(buffetId, result.getBuffet().getId());
    }

    @Test
    void toDomainShouldHandleMissingRelations() {
        KiosqueroEntity entity = new KiosqueroEntity();
        entity.setId(UUID.randomUUID());

        Kiosquero result = entity.toDomain();

        assertNull(result.getUsuario());
        assertNull(result.getBuffet());
    }

    private UsuarioEntity userEntity() {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdCognito("sub");
        entity.setNombre("Kiosquero");
        entity.setApellido("Buffet");
        entity.setEmail("kiosquero@example.com");
        entity.setRol(RolUsuario.VENDEDOR);
        entity.setActivo(true);
        entity.setAutenticadoConCognito(true);
        return entity;
    }

    private BuffetEntity buffetEntity(UUID id) {
        BuffetEntity entity = new BuffetEntity();
        entity.setId(id);
        entity.setNombre("Buffet");
        entity.setActivo(true);
        return entity;
    }
}
