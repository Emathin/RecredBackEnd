package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.SpringDataPerfilConsumoIARepository;
import com.recred.backend.infrastructure.persistence.StudentAIProfileRepositoryImpl;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class StudentAIProfileRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private StudentAIProfileRepositoryImpl repository;

    @Autowired
    private SpringDataPerfilConsumoIARepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findStudentsWithFavoriteProductShouldMergeSourcesAndKeepActiveTutors() {
        ProductoEntity product = generateProduct();
        
        AlumnoEntity profileStudent = generateStudent("Ana", true);
        TutorEntity activeTutor1 = generateTutor("Active Tutor 1", true);
        TutorEntity inactiveTutor1 = generateTutor("Inactive Tutor 1", false);
        
        activeTutor1.addHijo(profileStudent);
        entityManager.merge(activeTutor1);
        
        inactiveTutor1.addHijo(profileStudent);
        entityManager.merge(inactiveTutor1);
        
        PerfilConsumoIAEntity profile = generateProfile(profileStudent, product);

        profileStudent.getProductosFavoritos().add(product);
        entityManager.merge(profileStudent);
        
        AlumnoEntity manualStudent = generateStudent("Luis", false);
        TutorEntity activeTutor2 = generateTutor("Active Tutor 2", true);
        
        activeTutor2.addHijo(manualStudent);
        entityManager.merge(activeTutor2);
        
        manualStudent.getProductosFavoritos().add(product);
        entityManager.merge(manualStudent);
        
        entityManager.flush();
        entityManager.clear();

        List<Alumno> result = repository.findStudentsWithFavoriteProduct(product.getId());

        assertStudentsAndTutorsMerged(result, profileStudent, manualStudent);
    }

    private void assertStudentsAndTutorsMerged(List<Alumno> result, AlumnoEntity profileStudent, AlumnoEntity manualStudent) {
        assertEquals(2, result.size());
        
        Alumno ana = result.stream().filter(a -> a.getUsuario().getNombre().equals("Ana")).findFirst().orElseThrow();
        Alumno luis = result.stream().filter(a -> a.getUsuario().getNombre().equals("Luis")).findFirst().orElseThrow();

        assertEquals(profileStudent.getId(), ana.getId());
        assertEquals("Ana", ana.getUsuario().getNombre());
        assertEquals(profileStudent.getGradoEntity().getId(), ana.getGrado().getId());
        assertEquals("1", ana.getGrado().getAnio());
        assertEquals(1, ana.getTutores().size());
        assertEquals("Active Tutor 1", ana.getTutores().iterator().next().getUsuario().getNombre());
        
        assertEquals(manualStudent.getId(), luis.getId());
        assertNull(luis.getGrado());
        assertEquals(1, luis.getTutores().size());
        assertEquals("Active Tutor 2", luis.getTutores().iterator().next().getUsuario().getNombre());
    }

    private ProductoEntity generateProduct() {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setDescripcion("Snacks");
        category.setActivo(true);
        entityManager.persist(category);

        ProductoEntity product = new ProductoEntity();
        product.setCategoria(category);
        product.setNombre("Test Product");
        product.setDescripcion("Desc");
        product.setPrecio(new BigDecimal("100"));
        product.setActivo(true);
        product.setRequierePreparacion(false);
        entityManager.persist(product);
        return product;
    }

    private AlumnoEntity generateStudent(String name, boolean withGrade) {
        UsuarioEntity user = generateUser(name, RolUsuario.ALUMNO);
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(user);
        student.setActivo(true);
        
        if (withGrade) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("School Test " + UUID.randomUUID().toString().substring(0, 5));
            school.setCue("CUE-" + UUID.randomUUID().toString().substring(0, 5));
            school.setActivo(true);
            entityManager.persist(school);

            GradoEntity grade = new GradoEntity();
            grade.setAnio("1");
            grade.setDivision("A");
            grade.setActivo(true);
            grade.setColegioEntity(school);
            entityManager.persist(grade);
            student.setGradoEntity(grade);
        }
        
        entityManager.persist(student);
        return student;
    }

    private TutorEntity generateTutor(String name, boolean active) {
        UsuarioEntity user = generateUser(name, RolUsuario.PADRE);
        TutorEntity tutor = new TutorEntity();
        tutor.setUsuario(user);
        tutor.setActiva(active);
        entityManager.persist(tutor);
        return tutor;
    }

    private UsuarioEntity generateUser(String name, RolUsuario role) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito(name + "-sub-" + UUID.randomUUID().toString().substring(0, 5));
        user.setNombre(name);
        user.setApellido("Surname");
        user.setEmail(name + UUID.randomUUID().toString().substring(0, 5) + "@test.com");
        user.setRol(role);
        user.setActivo(true);
        user.setAutenticadoConCognito(true);
        entityManager.persist(user);
        return user;
    }

    private PerfilConsumoIAEntity generateProfile(AlumnoEntity student, ProductoEntity product) {
        PerfilConsumoIAEntity profile = new PerfilConsumoIAEntity();
        profile.setAlumnoEntity(student);
        profile.setProductoEntityFavorito(product);
        entityManager.persist(profile);
        return profile;
    }
}
