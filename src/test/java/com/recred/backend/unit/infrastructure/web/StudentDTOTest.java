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

    private static final String STUDENT_NAME = "Martina";
    private static final String STUDENT_LAST_NAME = "Gomez";

    private Alumno alumno;
    private StudentDTO dto;
    private UUID schoolId;
    private UUID studentId;
    private StudentDTO fullGradeDto;
    private StudentDTO onlyYearDto;
    private StudentDTO onlyDivisionDto;

    @Test
    void givenStudentWithCompleteUser_whenMappingToDTO_thenExtractsNameAndLastName() {
        givenStudentWithCompleteUser();

        whenMapStudentToDTO();

        thenNameAndLastNameAreMapped();
    }

    @Test
    void givenStudentWithoutUser_whenMappingToDTO_thenAssignsDefaultValues() {
        givenStudentWithoutUser();

        whenMapStudentToDTO();

        thenDefaultNameAndLastNameAreAssigned();
    }

    @Test
    void givenStudentWithGradeSchoolWalletAndPhoto_whenMappingToDTO_thenMapsAllFields() {
        givenStudentWithGradeSchoolWalletAndPhoto();

        whenMapStudentToDTO();

        thenGradeSchoolWalletAndPhotoAreMapped();
    }

    @Test
    void givenStudentDataWithPartialGradeValues_whenMappingToDTO_thenAppliesFallbacks() {
        givenStudentDataWithPartialGradeValues();

        whenMapStudentDataToDTOs();

        thenFallbacksAndFormattedGradesAreApplied();
    }

    @Test
    void givenGradeWithoutDivision_whenMappingToDTO_thenDoesNotRenderNullText() {
        givenStudentWithGradeWithoutDivision();

        whenMapStudentToDTO();

        thenGradeDoesNotRenderNullText();
    }

    private void givenStudentWithCompleteUser() {
        alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(user(STUDENT_NAME, STUDENT_LAST_NAME));
        alumno.setActivo(true);
    }

    private void givenStudentWithoutUser() {
        alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(null);
        alumno.setActivo(true);
    }

    private void givenStudentWithGradeSchoolWalletAndPhoto() {
        schoolId = UUID.randomUUID();
        alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(userWithPhoto("Ana", "Perez", "foto.png"));
        alumno.setGrado(grade("5", "B", schoolId));
        alumno.setBilletera(wallet("1234.50"));
        alumno.setActivo(true);
    }

    private void givenStudentDataWithPartialGradeValues() {
        studentId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
    }

    private void givenStudentWithGradeWithoutDivision() {
        alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(user("Ana", "Perez"));
        alumno.setGrado(grade("5", null, null));
    }

    private void whenMapStudentToDTO() {
        dto = StudentDTO.fromDomain(alumno);
    }

    private void whenMapStudentDataToDTOs() {
        fullGradeDto = StudentDTO.fromStudentData(
                studentId,
                null,
                null,
                true,
                "6",
                "A",
                schoolId,
                null,
                "avatar.png"
        );
        onlyYearDto = StudentDTO.fromStudentData(
                studentId,
                "Juan",
                "Lopez",
                true,
                "6",
                null,
                schoolId,
                new BigDecimal("50.00"),
                null
        );
        onlyDivisionDto = StudentDTO.fromStudentData(
                studentId,
                "Juan",
                "Lopez",
                true,
                null,
                "B",
                schoolId,
                new BigDecimal("50.00"),
                null
        );
    }

    private void thenNameAndLastNameAreMapped() {
        assertEquals(alumno.getId(), dto.id());
        assertEquals(STUDENT_NAME, dto.nombre());
        assertEquals(STUDENT_LAST_NAME, dto.apellido());
        assertTrue(dto.activo());
    }

    private void thenDefaultNameAndLastNameAreAssigned() {
        assertEquals(alumno.getId(), dto.id());
        assertEquals("Sin nombre", dto.nombre());
        assertEquals("Sin apellido", dto.apellido());
    }

    private void thenGradeSchoolWalletAndPhotoAreMapped() {
        assertEquals("5 B", dto.grado());
        assertEquals(schoolId, dto.colegioId());
        assertEquals(new BigDecimal("1234.50"), dto.saldo());
        assertEquals("foto.png", dto.urlFotoPerfil());
    }

    private void thenFallbacksAndFormattedGradesAreApplied() {
        assertEquals("Sin nombre", fullGradeDto.nombre());
        assertEquals("Sin apellido", fullGradeDto.apellido());
        assertEquals("6 A", fullGradeDto.grado());
        assertEquals(BigDecimal.ZERO, fullGradeDto.saldo());
        assertEquals("avatar.png", fullGradeDto.urlFotoPerfil());
        assertEquals("6", onlyYearDto.grado());
        assertEquals("B", onlyDivisionDto.grado());
    }

    private void thenGradeDoesNotRenderNullText() {
        assertEquals("5", dto.grado());
    }

    private Usuario user(String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        return usuario;
    }

    private Usuario userWithPhoto(String nombre, String apellido, String urlFotoPerfil) {
        Usuario usuario = user(nombre, apellido);
        usuario.setUrlFotoPerfil(urlFotoPerfil);
        return usuario;
    }

    private Grado grade(String anio, String division, UUID schoolId) {
        Grado grado = new Grado();
        grado.setAnio(anio);
        grado.setDivision(division);
        if (schoolId != null) {
            Colegio colegio = new Colegio();
            colegio.setId(schoolId);
            grado.setColegio(colegio);
        }
        return grado;
    }

    private Billetera wallet(String saldo) {
        Billetera billetera = new Billetera();
        billetera.setSaldoCreditos(new BigDecimal(saldo));
        return billetera;
    }
}
