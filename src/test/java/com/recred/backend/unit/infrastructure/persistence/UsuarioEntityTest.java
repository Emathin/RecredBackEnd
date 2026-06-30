package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    @Test
    void relationSettersShouldKeepInverseSide() {
        UsuarioEntity user = userEntity();
        AlumnoEntity alumno = new AlumnoEntity();
        TutorEntity tutor = new TutorEntity();
        KiosqueroEntity kiosquero = new KiosqueroEntity();

        user.setAlumnoEntity(alumno);
        user.setTutorEntity(tutor);
        user.setKiosqueroEntity(kiosquero);

        assertSame(user, alumno.getUsuario());
        assertSame(user, tutor.getUsuario());
        assertSame(user, kiosquero.getUsuario());
    }

    @Test
    void toDomainMapsSchoolStudentTutorAndSeller() {
        UUID colegioId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        UsuarioEntity entity = userEntity();
        entity.setColegioEntity(colegioEntity(colegioId));
        AlumnoEntity alumno = new AlumnoEntity();
        alumno.setId(alumnoId);
        TutorEntity tutor = new TutorEntity();
        tutor.setId(tutorId);
        KiosqueroEntity kiosquero = new KiosqueroEntity();
        kiosquero.setId(kiosqueroId);

        entity.setAlumnoEntity(alumno);
        entity.setTutorEntity(tutor);
        entity.setKiosqueroEntity(kiosquero);

        Usuario result = entity.toDomain();

        assertEquals(colegioId, result.getColegio().getId());
        assertEquals(alumnoId, result.getAlumno().getId());
        assertEquals(tutorId, result.getTutor().getId());
        assertEquals(kiosqueroId, result.getKiosquero().getId());
    }

    @Test
    void fromDomainShouldMapAlumnoReferences() {
        UUID alumnoId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID healthId = UUID.randomUUID();
        UUID favoriteId = UUID.randomUUID();
        UUID restrictionId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setActivo(null);
        alumno.setGrado(grade(gradeId));
        alumno.setRestriccionesSalud(Set.of(health(healthId)));
        alumno.setProductosFavoritos(Set.of(product(favoriteId)));
        alumno.setRestriccionesHorarias(Set.of(restriction(restrictionId)));
        alumno.setBilletera(wallet());

        AlumnoEntity result = UsuarioEntity.fromDomain(alumno);

        assertEquals(alumnoId, result.getId());
        assertEquals(true, result.getActivo());
        assertEquals(gradeId, result.getGradoEntity().getId());
        assertEquals(healthId, result.getRestriccionesSalud().iterator().next().getId());
        assertEquals(favoriteId, result.getProductosFavoritos().iterator().next().getId());
        assertEquals(restrictionId, result.getRestriccionesHorarias().iterator().next().getId());
        assertSame(result, result.getRestriccionesHorarias().iterator().next().getAlumnoEntity());
        assertSame(result, result.getBilleteraEntity().getAlumnoEntity());
    }

    @Test
    void fromDomainShouldReturnNullWhenAlumnoIsNull() {
        assertNull(UsuarioEntity.fromDomain(null));
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

    private ColegioEntity colegioEntity(UUID id) {
        ColegioEntity entity = new ColegioEntity();
        entity.setId(id);
        entity.setNombre("Colegio");
        entity.setCue("123");
        entity.setActivo(true);
        return entity;
    }

    private com.recred.backend.domain.models.Grado grade(UUID id) {
        com.recred.backend.domain.models.Grado grade = new com.recred.backend.domain.models.Grado();
        grade.setId(id);
        return grade;
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

    private Billetera wallet() {
        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("50.00"));
        return wallet;
    }
}
