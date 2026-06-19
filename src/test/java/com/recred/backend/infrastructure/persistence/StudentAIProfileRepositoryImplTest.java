package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.GradoEntity;
import com.recred.backend.infrastructure.entities.PerfilConsumoIAEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAIProfileRepositoryImplTest {

    @Mock
    private SpringDataPerfilConsumoIARepository springDataRepository;

    private StudentAIProfileRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new StudentAIProfileRepositoryImpl(springDataRepository);
    }

    @Test
    void findStudentsWithFavoriteProductShouldMergeSourcesAndKeepActiveTutors() {
        UUID productId = UUID.randomUUID();
        AlumnoEntity profileStudent = studentEntity("Ana", true);
        AlumnoEntity manualDuplicate = profileStudent;
        AlumnoEntity manualStudent = studentEntity("Luis", false);
        PerfilConsumoIAEntity profile = new PerfilConsumoIAEntity();
        profile.setAlumnoEntity(profileStudent);

        when(springDataRepository.findByProductoEntityFavoritoId(productId))
                .thenReturn(List.of(profile));
        when(springDataRepository.findStudentsWithManualFavoriteProduct(productId))
                .thenReturn(List.of(manualDuplicate, manualStudent));

        List<Alumno> result = repository.findStudentsWithFavoriteProduct(productId);

        assertEquals(2, result.size());
        Alumno first = result.get(0);
        Alumno second = result.get(1);
        assertEquals(profileStudent.getId(), first.getId());
        assertEquals("Ana", first.getUsuario().getNombre());
        assertEquals(profileStudent.getGradoEntity().getId(), first.getGrado().getId());
        assertEquals("1", first.getGrado().getAnio());
        assertEquals(1, first.getTutores().size());
        assertEquals("Tutor Activo", first.getTutores().iterator().next().getUsuario().getNombre());
        assertEquals(manualStudent.getId(), second.getId());
        assertNull(second.getGrado());
        assertEquals(1, second.getTutores().size());
    }

    private AlumnoEntity studentEntity(String name, boolean withGrade) {
        AlumnoEntity student = new AlumnoEntity();
        student.setId(UUID.randomUUID());
        student.setUsuario(user(name, RolUsuario.ALUMNO));
        if (withGrade) {
            student.setGradoEntity(grade());
        }
        student.getTutores().add(tutor("Tutor Activo", true));
        student.getTutores().add(tutor("Tutor Inactivo", false));
        return student;
    }

    private GradoEntity grade() {
        GradoEntity grade = new GradoEntity();
        grade.setId(UUID.randomUUID());
        grade.setAnio("1");
        grade.setDivision("A");
        grade.setActivo(true);
        return grade;
    }

    private TutorEntity tutor(String name, boolean active) {
        TutorEntity tutor = new TutorEntity();
        tutor.setId(UUID.randomUUID());
        tutor.setActiva(active);
        tutor.setUsuario(user(name, RolUsuario.PADRE));
        return tutor;
    }

    private UsuarioEntity user(String name, RolUsuario role) {
        UsuarioEntity user = new UsuarioEntity();
        user.setId(UUID.randomUUID());
        user.setIdCognito(name + "-sub");
        user.setNombre(name);
        user.setApellido("Apellido");
        user.setEmail(name + "@example.com");
        user.setRol(role);
        user.setActivo(true);
        user.setAutenticadoConCognito(true);
        return user;
    }
}
