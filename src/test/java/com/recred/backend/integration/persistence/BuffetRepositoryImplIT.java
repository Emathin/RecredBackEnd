package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.*;
import com.recred.backend.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class BuffetRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SpringDataBuffetRepository springDataBuffetRepository;

    @Autowired
    private SpringDataColegioRepository colegioRepository;

    @Autowired
    private SpringDataUsuarioRepository usuarioRepository;

    @Autowired
    private SpringDataGradoRepository gradoRepository;

    @Autowired
    private SpringDataAlumnoRepository alumnoRepository;

    @Autowired
    private SpringDataKiosqueroRepository kiosqueroRepository;

    @Autowired
    private BuffetRepositoryImpl repository;

    private UUID buffetId;
    private UUID studentId;
    private UUID schoolId;

    @BeforeEach
    void setUp() {
        prepareRelationalEnvironment();
    }

    @Test
    void findById_WhenBuffetExists_ShouldMapToDomain() {
        Optional<Buffet> result = repository.findById(buffetId);

        assertTrue(result.isPresent());
        assertEquals(buffetId, result.get().getId());
        assertEquals("Buffet central", result.get().getNombre());
        assertEquals(schoolId, result.get().getColegio().getId());
    }

    @Test
    void findByAlumnoId_WhenBuffetExists_ShouldMapToDomain() {
        Optional<Buffet> result = repository.findByAlumnoId(studentId);

        assertTrue(result.isPresent());
        assertEquals(buffetId, result.get().getId());
        assertEquals("Buffet central", result.get().getNombre());
        assertEquals(schoolId, result.get().getColegio().getId());
    }

    @Test
    void save_ShouldSaveInRealDatabaseAndGenerateId() {
        Buffet domainBuffet = new Buffet();
        domainBuffet.setNombre("Nuevo Buffet IT");
        domainBuffet.setActivo(true);
        Colegio colegio = new Colegio();
        colegio.setId(schoolId);
        domainBuffet.setColegio(colegio);

        Buffet savedBuffet = repository.save(domainBuffet);

        assertNotNull(savedBuffet.getId());
        
        Optional<BuffetEntity> dbBuffet = springDataBuffetRepository.findById(savedBuffet.getId());
        assertTrue(dbBuffet.isPresent());
        assertEquals("Nuevo Buffet IT", dbBuffet.get().getNombre());
        assertEquals(schoolId, dbBuffet.get().getColegioEntity().getId());
    }


    private ColegioEntity generateSchool() {
        ColegioEntity school = new ColegioEntity();
        school.setNombre("Colegio " + UUID.randomUUID());
        school.setCue("12345678");
        return colegioRepository.saveAndFlush(school);
    }

    private void prepareRelationalEnvironment() {
        ColegioEntity school = generateSchool();
        this.schoolId = school.getId();
        BuffetEntity buffet = generateBuffet(school);
        KiosqueroEntity vendor = generateVendor(buffet);


        this.buffetId = buffet.getId();

        vendor.setBuffetEntity(buffet);
        kiosqueroRepository.saveAndFlush(vendor);

        GradoEntity grade = generateGrade(school);
        AlumnoEntity student = generateStudent(grade);
        this.studentId = student.getId();
    }

    private KiosqueroEntity generateVendor(BuffetEntity buffet) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("kiosquero_" + UUID.randomUUID());
        user.setNombre("Vendedor");
        user.setApellido("Prueba");
        user.setEmail("vendedor_" + UUID.randomUUID() + "@test.com");
        user.setRol(RolUsuario.VENDEDOR);
        user.setAutenticadoConCognito(true);
        user = usuarioRepository.saveAndFlush(user);

        KiosqueroEntity vendor = new KiosqueroEntity();
        vendor.setUsuario(user);
        vendor.setCuit("20-12345678-9");
        vendor.setBuffetEntity(buffet);
        buffet.setVendedor(vendor);
        return kiosqueroRepository.save(vendor);
    }

    private BuffetEntity generateBuffet(ColegioEntity school) {
        BuffetEntity buffet = new BuffetEntity();
        buffet.setNombre("Buffet central");
        buffet.setColegioEntity(school);
        return springDataBuffetRepository.saveAndFlush(buffet);
    }

    private GradoEntity generateGrade(ColegioEntity school) {
        GradoEntity grade = new GradoEntity();
        grade.setColegioEntity(school);
        grade.setAnio("5to");
        grade.setDivision("A");
        return gradoRepository.saveAndFlush(grade);
    }

    private AlumnoEntity generateStudent(GradoEntity grade) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("alumno_" + UUID.randomUUID());
        user.setNombre("Alumno");
        user.setApellido("Prueba");
        user.setEmail("alumno_" + UUID.randomUUID() + "@test.com");
        user.setRol(RolUsuario.ALUMNO);
        user.setAutenticadoConCognito(true);
        user = usuarioRepository.saveAndFlush(user);

        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(user);
        student.setGradoEntity(grade);
        return alumnoRepository.saveAndFlush(student);
    }
}
