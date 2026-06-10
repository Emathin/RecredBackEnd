package com.recred.backend.infrastructure.entities;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsuarioEntityTest {

    @Test
    void toDomainMapsSellerBuffetId() {
        UUID buffetId = UUID.randomUUID();
        UsuarioEntity entity = userEntity();
        KiosqueroEntity kiosquero = new KiosqueroEntity();
        kiosquero.setId(UUID.randomUUID());
        kiosquero.setBuffetEntity(buffetEntity(buffetId));
        entity.setKiosqueroEntity(kiosquero);

        Usuario usuario = entity.toDomain();

        assertEquals(kiosquero.getId(), usuario.getKiosquero().getId());
        assertEquals(buffetId, usuario.getKiosquero().getBuffet().getId());
    }

    private UsuarioEntity userEntity() {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdCognito("cognito-sub");
        entity.setNombre("Vendedor");
        entity.setApellido("Buffet");
        entity.setEmail("vendedor@example.com");
        entity.setRol(RolUsuario.VENDEDOR);
        entity.setAutenticadoConCognito(true);
        entity.setActivo(true);
        return entity;
    }

    private BuffetEntity buffetEntity(UUID buffetId) {
        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(buffetId);
        return buffet;
    }
}
