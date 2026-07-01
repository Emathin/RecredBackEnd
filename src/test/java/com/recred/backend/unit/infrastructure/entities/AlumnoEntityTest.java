package com.recred.backend.unit.infrastructure.entities;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlumnoEntityTest {

    @Test
    void toDomainShouldMapFullStudent() {
        UUID alumnoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID healthId = UUID.randomUUID();
        UUID favoriteId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        UUID restrictionId = UUID.randomUUID();
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(alumnoId);
        entity.setActivo(true);
        entity.setUsuario(userEntity(userId));
        entity.setGradoEntity(gradeEntity(gradeId));
        entity.setBilleteraEntity(walletEntity());
        entity.setRestriccionesSalud(Set.of(healthEntity(healthId)));
        entity.setProductosFavoritos(Set.of(productEntity(favoriteId, "Fruta")));
        entity.setProductosBloqueados(Set.of(productEntity(blockedId, "Gaseosa")));
        entity.setRestriccionesHorarias(Set.of(restrictionEntity(entity, restrictionId)));
        entity.setPuntosRecompensa(null);

        Alumno result = entity.toDomain();

        assertEquals(alumnoId, result.getId());
        assertEquals(userId, result.getUsuarioId());
        assertEquals("Alumno", result.getNombre());
        assertEquals("Uno", result.getApellido());
        assertEquals(gradeId, result.getGrado().getId());
        assertEquals(new BigDecimal("100.00"), result.getBilletera().getSaldoCreditos());
        assertEquals(healthId, result.getRestriccionesSalud().iterator().next().getId());
        assertEquals(favoriteId, result.getProductosFavoritos().iterator().next().getId());
        assertEquals(blockedId, result.getProductosBloqueados().iterator().next().getId());
        assertEquals(restrictionId, result.getRestriccionesHorarias().iterator().next().getId());
        assertEquals(0, result.getPuntosRecompensa());
        assertTrue(result.getTutores().isEmpty());
    }

    @Test
    void toDomainShouldHandleMissingOptionalRelations() {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(UUID.randomUUID());
        entity.setActivo(false);
        entity.setRestriccionesSalud(Set.of());
        entity.setProductosFavoritos(Set.of());
        entity.setProductosBloqueados(Set.of());
        entity.setRestriccionesHorarias(Set.of());
        entity.setPuntosRecompensa(12);

        Alumno result = entity.toDomain();

        assertNull(result.getUsuario());
        assertNull(result.getGrado());
        assertNull(result.getBilletera());
        assertTrue(result.getRestriccionesSalud().isEmpty());
        assertTrue(result.getProductosFavoritos().isEmpty());
        assertTrue(result.getProductosBloqueados().isEmpty());
        assertTrue(result.getRestriccionesHorarias().isEmpty());
        assertEquals(12, result.getPuntosRecompensa());
    }

    @Test
    void fromDomainShouldMapReferencesAndDefaults() {
        UUID alumnoId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID healthId = UUID.randomUUID();
        UUID favoriteId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        UUID restrictionId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setActivo(null);
        alumno.setGrado(grade(gradeId));
        alumno.setBilletera(wallet());
        alumno.setRestriccionesSalud(Set.of(health(healthId), health(null)));
        alumno.setProductosFavoritos(Set.of(product(favoriteId), product(null)));
        alumno.setProductosBloqueados(Set.of(product(blockedId), product(null)));
        alumno.setRestriccionesHorarias(Set.of(restriction(restrictionId)));
        alumno.setPuntosRecompensa(null);

        AlumnoEntity result = AlumnoEntity.fromDomain(alumno);

        assertEquals(alumnoId, result.getId());
        assertEquals(true, result.getActivo());
        assertEquals(gradeId, result.getGradoEntity().getId());
        assertEquals(new BigDecimal("100.00"), result.getBilleteraEntity().getSaldoCreditos());
        assertSame(result, result.getBilleteraEntity().getAlumnoEntity());
        assertEquals(1, result.getRestriccionesSalud().size());
        assertEquals(healthId, result.getRestriccionesSalud().iterator().next().getId());
        assertEquals(1, result.getProductosFavoritos().size());
        assertEquals(favoriteId, result.getProductosFavoritos().iterator().next().getId());
        assertEquals(1, result.getProductosBloqueados().size());
        assertEquals(blockedId, result.getProductosBloqueados().iterator().next().getId());
        assertEquals(1, result.getRestriccionesHorarias().size());
        RestriccionHorariaEntity horaria = result.getRestriccionesHorarias().iterator().next();
        assertEquals(restrictionId, horaria.getId());
        assertSame(result, horaria.getAlumnoEntity());
        assertEquals(0, result.getPuntosRecompensa());
    }

    @Test
    void fromDomainShouldReturnNullWhenAlumnoIsNull() {
        assertNull(AlumnoEntity.fromDomain(null));
    }

    private UsuarioEntity userEntity(UUID id) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(id);
        entity.setIdCognito("sub");
        entity.setNombre("Alumno");
        entity.setApellido("Uno");
        entity.setEmail("alumno@example.com");
        entity.setRol(RolUsuario.ALUMNO);
        entity.setActivo(true);
        entity.setAutenticadoConCognito(true);
        return entity;
    }

    private GradoEntity gradeEntity(UUID id) {
        GradoEntity entity = new GradoEntity();
        entity.setId(id);
        entity.setAnio("1");
        entity.setDivision("A");
        entity.setActivo(true);
        return entity;
    }

    private BilleteraEntity walletEntity() {
        BilleteraEntity entity = new BilleteraEntity();
        entity.setId(UUID.randomUUID());
        entity.setSaldoCreditos(new BigDecimal("100.00"));
        return entity;
    }

    private ClasificacionSaludEntity healthEntity(UUID id) {
        ClasificacionSaludEntity entity = new ClasificacionSaludEntity();
        entity.setId(id);
        entity.setDescripcion("Saludable");
        entity.setActivo(true);
        return entity;
    }

    private ProductoEntity productEntity(UUID id, String name) {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(id);
        entity.setNombre(name);
        entity.setPrecio(new BigDecimal("10.00"));
        entity.setActivo(true);
        return entity;
    }

    private RestriccionHorariaEntity restrictionEntity(AlumnoEntity alumno, UUID id) {
        RestriccionHorariaEntity entity = new RestriccionHorariaEntity();
        entity.setId(id);
        entity.setAlumnoEntity(alumno);
        entity.setActiva(true);
        entity.setFranjaHorariaEntity(timeSlotEntity());
        return entity;
    }

    private FranjaHorariaEntity timeSlotEntity() {
        FranjaHorariaEntity entity = new FranjaHorariaEntity();
        entity.setId(UUID.randomUUID());
        entity.setDescripcion("Primer recreo");
        entity.setHoraInicio(LocalTime.of(10, 0));
        entity.setHoraFin(LocalTime.of(10, 15));
        entity.setActivo(true);
        return entity;
    }

    private Grado grade(UUID id) {
        Grado grade = new Grado();
        grade.setId(id);
        return grade;
    }

    private Billetera wallet() {
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("100.00"));
        return wallet;
    }

    private ClasificacionSalud health(UUID id) {
        ClasificacionSalud health = new ClasificacionSalud();
        health.setId(id);
        return health;
    }

    private Producto product(UUID id) {
        Producto product = new Producto();
        product.setId(id);
        return product;
    }

    private RestriccionHoraria restriction(UUID id) {
        RestriccionHoraria restriction = new RestriccionHoraria();
        restriction.setId(id);
        return restriction;
    }
}

