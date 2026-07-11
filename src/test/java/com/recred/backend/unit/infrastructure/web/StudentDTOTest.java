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

    private Alumno student;
    private StudentDTO dto;
    private UUID schoolId;
    private UUID studentId;
    private StudentDTO fullGradeDto;
    private StudentDTO onlyYearDto;
    private StudentDTO onlyDivisionDto;

    @Test
    void givenStudentWithCompleteUserWhenMappingToDTOThenExtractsNameAndLastName() {
        givenStudentWithCompleteUser();

        whenMapStudentToDTO();

        thenNameAndLastNameAreMapped();
    }

    @Test
    void givenStudentWithoutUserWhenMappingToDTOThenAssignsDefaultValues() {
        givenStudentWithoutUser();

        whenMapStudentToDTO();

        thenDefaultNameAndLastNameAreAssigned();
    }

    @Test
    void givenStudentWithGradeSchoolWalletAndPhotoWhenMappingToDTOThenMapsAllFields() {
        givenStudentWithGradeSchoolWalletAndPhoto();

        whenMapStudentToDTO();

        thenGradeSchoolWalletAndPhotoAreMapped();
    }

    @Test
    void givenStudentDataWithPartialGradeValuesWhenMappingToDTOThenAppliesFallbacks() {
        givenStudentDataWithPartialGradeValues();

        whenMapStudentDataToDTOs();

        thenFallbacksAndFormattedGradesAreApplied();
    }

    @Test
    void givenGradeWithoutDivisionWhenMappingToDTOThenDoesNotRenderNullText() {
        givenStudentWithGradeWithoutDivision();

        whenMapStudentToDTO();

        thenGradeDoesNotRenderNullText();
    }

    private void givenStudentWithCompleteUser() {
        student = new Alumno();
        student.setId(UUID.randomUUID());
        student.setUsuario(user(STUDENT_NAME, STUDENT_LAST_NAME));
        student.setActivo(true);
    }

    private void givenStudentWithoutUser() {
        student = new Alumno();
        student.setId(UUID.randomUUID());
        student.setUsuario(null);
        student.setActivo(true);
    }

    private void givenStudentWithGradeSchoolWalletAndPhoto() {
        schoolId = UUID.randomUUID();
        student = new Alumno();
        student.setId(UUID.randomUUID());
        student.setUsuario(userWithPhoto("Ana", "Perez", "foto.png"));
        student.setGrado(grade("5", "B", schoolId));
        student.setBilletera(wallet("1234.50"));
        student.setActivo(true);
    }

    private void givenStudentDataWithPartialGradeValues() {
        studentId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
    }

    private void givenStudentWithGradeWithoutDivision() {
        student = new Alumno();
        student.setId(UUID.randomUUID());
        student.setUsuario(user("Ana", "Perez"));
        student.setGrado(grade("5", null, null));
    }

    private void whenMapStudentToDTO() {
        dto = StudentDTO.fromDomain(student);
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
        assertEquals(student.getId(), dto.id());
        assertEquals(STUDENT_NAME, dto.nombre());
        assertEquals(STUDENT_LAST_NAME, dto.apellido());
        assertTrue(dto.activo());
    }

    private void thenDefaultNameAndLastNameAreAssigned() {
        assertEquals(student.getId(), dto.id());
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

    private Usuario user(String name, String lastName) {
        Usuario user = new Usuario();
        user.setNombre(name);
        user.setApellido(lastName);
        return user;
    }

    private Usuario userWithPhoto(String name, String lastName, String profilePictureUrl) {
        Usuario user = user(name, lastName);
        user.setUrlFotoPerfil(profilePictureUrl);
        return user;
    }

    private Grado grade(String year, String division, UUID schoolId) {
        Grado grade = new Grado();
        grade.setAnio(year);
        grade.setDivision(division);
        if (schoolId != null) {
            Colegio school = new Colegio();
            school.setId(schoolId);
            grade.setColegio(school);
        }
        return grade;
    }

    private Billetera wallet(String balance) {
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(new BigDecimal(balance));
        return wallet;
    }
}
