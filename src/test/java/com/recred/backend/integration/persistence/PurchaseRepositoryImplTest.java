package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.PurchaseRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataPurchaseRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class PurchaseRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private PurchaseRepositoryImpl repository;

    @Autowired
    private SpringDataPurchaseRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private ColegioEntity cachedSchool;
    private KiosqueroEntity cachedVendor;
    private BuffetEntity cachedBuffet;
    private AlumnoEntity cachedStudent;
    private TutorEntity cachedTutor;

    @Test
    void findAllByAlumno_ShouldReturnPurchases_WhenStudentExists() {
        AlumnoEntity student = generateStudent();
        CompraPresencialEntity purchase = generatePurchase(student, EstadoCompra.ENTREGADO);
        List<PurchaseResume> result = repository.findPurchasesByAlumno(student.getId());

        assertEquals(1, result.size());
        assertEquals(student.getId(), result.get(0).getIdAlumno());
    }

    @Test
    void findAllByTutor_ShouldReturnPurchases_WhenTutorExists() {
        AlumnoEntity student = generateStudent();
        TutorEntity tutor = generateTutor();
        tutor.addHijo(student);
        entityManager.merge(tutor);
        
        CompraPresencialEntity purchase = generatePurchase(student, EstadoCompra.ENTREGADO);

        List<Purchase> result = repository.findHistorialByTutor(tutor.getId());

        assertEquals(1, result.size());
        assertEquals(purchase.getId(), result.get(0).getId());
    }

    private ColegioEntity getCachedSchool() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("Test School");
            school.setCue("CUE-TEST-P");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private BuffetEntity getCachedBuffet() {
        if (this.cachedBuffet == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setNombre("Test Buffet");
            buffet.setColegioEntity(getCachedSchool());
            buffet.setActivo(true);
            entityManager.persist(buffet);
            
            KiosqueroEntity vendor = getCachedVendor(buffet);
            buffet.setVendedor(vendor);
            entityManager.merge(buffet);
            
            this.cachedBuffet = buffet;
        }
        return this.cachedBuffet;
    }

    private KiosqueroEntity getCachedVendor(BuffetEntity buffet) {
        if (this.cachedVendor == null) {
            UsuarioEntity vendorUser = new UsuarioEntity();
            vendorUser.setIdCognito("sub-vendor-test");
            vendorUser.setNombre("Vendor");
            vendorUser.setApellido("User");
            vendorUser.setEmail("vendor@test.com");
            vendorUser.setRol(RolUsuario.VENDEDOR);
            vendorUser.setActivo(true);
            entityManager.persist(vendorUser);

            KiosqueroEntity vendor = new KiosqueroEntity();
            vendor.setUsuario(vendorUser);
            vendor.setBuffetEntity(buffet);
            vendor.setCuit("123456789");
            entityManager.persist(vendor);
            this.cachedVendor = vendor;
        }
        return this.cachedVendor;
    }

    private AlumnoEntity generateStudent() {
        if (this.cachedStudent == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-student-test");
            user.setNombre("Test");
            user.setApellido("User");
            user.setEmail("student@test.com");
            user.setRol(RolUsuario.ALUMNO);
            user.setActivo(true);
            entityManager.persist(user);

            AlumnoEntity student = new AlumnoEntity();
            student.setUsuario(user);
            student.setActivo(true);
            entityManager.persist(student);
            this.cachedStudent = student;
        }
        return this.cachedStudent;
    }

    private TutorEntity generateTutor() {
        if (this.cachedTutor == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-tutor-test");
            user.setNombre("Tutor");
            user.setApellido("Parent");
            user.setEmail("tutor@test.com");
            user.setRol(RolUsuario.PADRE);
            user.setActivo(true);
            entityManager.persist(user);

            TutorEntity tutor = new TutorEntity();
            tutor.setUsuario(user);
            tutor.setActiva(true);
            entityManager.persist(tutor);
            this.cachedTutor = tutor;
        }
        return this.cachedTutor;
    }

    private CompraPresencialEntity generatePurchase(AlumnoEntity student, EstadoCompra status) {
        CompraPresencialEntity purchase = new CompraPresencialEntity();
        purchase.setAlumnoEntity(student);
        purchase.setBuffetEntity(getCachedBuffet());
        purchase.setMedioPago(MedioPago.CREDITOS);
        purchase.setEstado(status);
        purchase.setMontoTotal(new BigDecimal("500.00"));
        purchase.setFechaHora(LocalDateTime.now());
        entityManager.persist(purchase);

        CompraDetalleEntity detail = new CompraDetalleEntity();
        ProductoEntity product = new ProductoEntity();
        product.setNombre("Test Product");
        product.setDescripcion("Desc");
        product.setPrecio(new BigDecimal("500.00"));
        product.setRequierePreparacion(false);
        product.setActivo(true);
        CategoriaProductoEntity cat = new CategoriaProductoEntity();
        cat.setDescripcion("Cat");
        cat.setActivo(true);
        entityManager.persist(cat);
        product.setCategoria(cat);
        entityManager.persist(product);

        detail.setProductoEntity(product);
        detail.setCantidad(1);
        detail.setPrecioUnitario(new BigDecimal("500.00"));
        detail.setSubtotal(new BigDecimal("500.00"));
        purchase.addDetalle(detail);
        entityManager.merge(purchase);

        return purchase;
    }
}
