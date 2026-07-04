package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.web.StudentDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentDTOTest {

    @Test
    void dadoUnAlumnoConUsuarioCompleto_cuandoMapeaADTO_entoncesExtraeNombreYApellido() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Martina");
        usuario.setApellido("Gómez");

        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(usuario);
        alumno.setActivo(true);

        StudentDTO dto = StudentDTO.fromDomain(alumno);

        assertEquals(alumno.getId(), dto.id());
        assertEquals("Martina", dto.nombre());
        assertEquals("Gómez", dto.apellido());
        assertTrue(dto.activo());
    }

    @Test
    void dadoUnAlumnoConUsuarioNulo_cuandoMapeaADTO_entoncesAsignaValoresPorDefecto() {
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(null);
        alumno.setActivo(true);

        StudentDTO dto = StudentDTO.fromDomain(alumno);

        assertEquals(alumno.getId(), dto.id());
        assertEquals("Sin nombre", dto.nombre());
        assertEquals("Sin apellido", dto.apellido());
    }

    @Test
    void fromDomainShouldMapGradeSchoolWalletAndPhoto() {
        UUID schoolId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Perez");
        usuario.setUrlFotoPerfil("foto.png");
        Colegio colegio = new Colegio();
        colegio.setId(schoolId);
        Grado grado = new Grado();
        grado.setAnio("5");
        grado.setDivision("B");
        grado.setColegio(colegio);
        Billetera billetera = new Billetera();
        billetera.setSaldoCreditos(new BigDecimal("1234.50"));
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(usuario);
        alumno.setGrado(grado);
        alumno.setBilletera(billetera);
        alumno.setActivo(true);

        StudentDTO dto = StudentDTO.fromDomain(alumno);

        assertEquals("5 B", dto.grado());
        assertEquals(schoolId, dto.colegioId());
        assertEquals(new BigDecimal("1234.50"), dto.saldo());
        assertEquals("foto.png", dto.urlFotoPerfil());
    }

    @Test
    void fromStudentDataShouldApplyFallbacksAndFormatGrade() {
        UUID id = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();

        StudentDTO fullGrade = StudentDTO.fromStudentData(
                id,
                null,
                null,
                true,
                "6",
                "A",
                schoolId,
                null,
                "avatar.png"
        );
        StudentDTO onlyYear = StudentDTO.fromStudentData(
                id,
                "Juan",
                "Lopez",
                true,
                "6",
                null,
                schoolId,
                new BigDecimal("50.00"),
                null
        );
        StudentDTO onlyDivision = StudentDTO.fromStudentData(
                id,
                "Juan",
                "Lopez",
                true,
                null,
                "B",
                schoolId,
                new BigDecimal("50.00"),
                null
        );

        assertEquals("Sin nombre", fullGrade.nombre());
        assertEquals("Sin apellido", fullGrade.apellido());
        assertEquals("6 A", fullGrade.grado());
        assertEquals(BigDecimal.ZERO, fullGrade.saldo());
        assertEquals("avatar.png", fullGrade.urlFotoPerfil());
        assertEquals("6", onlyYear.grado());
        assertEquals("B", onlyDivision.grado());
    }
}
