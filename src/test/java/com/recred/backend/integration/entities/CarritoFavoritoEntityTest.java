package com.recred.backend.integration.entities;

import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarritoFavoritoEntityTest {

    @Test
    void addAndRemoveItemShouldKeepBidirectionalRelation() {
        CarritoFavoritoEntity cart = new CarritoFavoritoEntity();
        CarritoFavoritoItemEntity item = new CarritoFavoritoItemEntity();

        cart.addItem(item);

        assertTrue(cart.getItems().contains(item));
        assertSame(cart, item.getCarritoFavorito());

        cart.removeItem(item);

        assertTrue(cart.getItems().isEmpty());
        assertNull(item.getCarritoFavorito());
    }

    @Test
    void toDomainShouldMapTutorStudentAndItems() {
        UUID cartId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        ProductoEntity product = productEntity();
        CarritoFavoritoItemEntity item = new CarritoFavoritoItemEntity();
        item.setProductoEntity(product);
        item.setCantidad(2);
        CarritoFavoritoEntity entity = new CarritoFavoritoEntity();
        entity.setId(cartId);
        entity.setNombre("Merienda");
        entity.setTutor(tutorEntity(tutorId));
        entity.setAlumno(alumnoEntity(studentId));
        entity.addItem(item);

        CarritoFavorito result = entity.toDomain();

        assertEquals(cartId, result.getId());
        assertEquals("Merienda", result.getNombre());
        assertEquals(tutorId, result.getTutor().getId());
        assertEquals(studentId, result.getAlumno().getId());
        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getItems().values().iterator().next());
        assertEquals(product.getId(), result.getItems().keySet().iterator().next().getId());
    }

    @Test
    void toDomainShouldIgnoreItemsWithoutProductAndHandleNullRelations() {
        CarritoFavoritoItemEntity item = new CarritoFavoritoItemEntity();
        item.setCantidad(1);
        CarritoFavoritoEntity entity = new CarritoFavoritoEntity();
        entity.setId(UUID.randomUUID());
        entity.setNombre("Merienda");
        entity.addItem(item);

        CarritoFavorito result = entity.toDomain();

        assertNull(result.getTutor());
        assertNull(result.getAlumno());
        assertTrue(result.getItems().isEmpty());
    }

    private TutorEntity tutorEntity(UUID id) {
        TutorEntity entity = new TutorEntity();
        entity.setId(id);
        return entity;
    }

    private AlumnoEntity alumnoEntity(UUID id) {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(id);
        entity.setUsuario(userEntity());
        return entity;
    }

    private ProductoEntity productEntity() {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(UUID.randomUUID());
        entity.setNombre("Alfajor");
        entity.setDescripcion("Alfajor simple");
        entity.setPrecio(new BigDecimal("40.00"));
        entity.setPeso(new BigDecimal("50.00"));
        entity.setRequierePreparacion(false);
        entity.setActivo(true);
        return entity;
    }

    private UsuarioEntity userEntity() {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdCognito("sub");
        entity.setNombre("Alumno");
        entity.setApellido("Uno");
        entity.setEmail("alumno@example.com");
        entity.setRol(RolUsuario.ALUMNO);
        entity.setActivo(true);
        entity.setAutenticadoConCognito(true);
        return entity;
    }
}
