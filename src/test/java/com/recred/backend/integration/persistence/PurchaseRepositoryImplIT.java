package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class PurchaseRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private PurchaseRepositoryImpl repository;

    @Autowired
    private SpringDataPurchaseRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private ColegioEntity cachedSchool;
    private KiosqueroEntity cachedVendor;
    private BuffetEntity cachedBuffet;
    private AlumnoEntity cachedStudent;
    private TutorEntity cachedTutor;
    private final String fixtureSuffix = UUID.randomUUID().toString();

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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findScheduledPickups_ShouldMapPickupSlotOutsideCallerTransaction() {
        LocalDate pickupDate = LocalDate.now().plusDays(1);
        ScheduledPickupFixture fixture = transactionTemplate.execute(status ->
                generateScheduledPickup(pickupDate));

        List<Purchase> result = repository.findScheduledPickups(
                fixture.buffetId(),
                pickupDate,
                EstadoRetiro.PROGRAMADO,
                fixture.slotId(),
                "test"
        );

        assertEquals(1, result.size());
        Purchase purchase = result.get(0);
        assertEquals(fixture.purchaseId(), purchase.getId());
        assertEquals(fixture.slotId(), purchase.getPickupSlotId());
        assertEquals("Morning (09:00 - 10:00)", purchase.getPickupSlotDescription());
        assertEquals(1, purchase.getItems().size());
        assertEquals("Test User", purchase.getStudent().getNombre() + " "
                + purchase.getStudent().getApellido());

        List<Purchase> resultWithoutFilters = repository.findScheduledPickups(
                fixture.buffetId(),
                null,
                null,
                null,
                null
        );

        assertEquals(1, resultWithoutFilters.size());
        assertEquals(fixture.purchaseId(), resultWithoutFilters.get(0).getId());
    }

    private ColegioEntity getCachedSchool() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("Test School");
            school.setCue("CUE-TEST-P-" + fixtureSuffix);
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
            vendorUser.setIdCognito("sub-vendor-test-" + fixtureSuffix);
            vendorUser.setNombre("Vendor");
            vendorUser.setApellido("User");
            vendorUser.setEmail("vendor-" + fixtureSuffix + "@test.com");
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
            user.setIdCognito("sub-student-test-" + fixtureSuffix);
            user.setNombre("Test");
            user.setApellido("User");
            user.setEmail("student-" + fixtureSuffix + "@test.com");
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
            user.setIdCognito("sub-tutor-test-" + fixtureSuffix);
            user.setNombre("Tutor");
            user.setApellido("Parent");
            user.setEmail("tutor-" + fixtureSuffix + "@test.com");
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

    private ScheduledPickupFixture generateScheduledPickup(LocalDate pickupDate) {
        AlumnoEntity student = generateStudent();
        BuffetEntity buffet = getCachedBuffet();
        ProductoEntity product = generateProduct();
        FranjaHorariaEntity slot = generateTimeSlot();

        CompraAnticipadaEntity purchase = new CompraAnticipadaEntity();
        purchase.setAlumnoEntity(student);
        purchase.setBuffetEntity(buffet);
        purchase.setMedioPago(MedioPago.CREDITOS);
        purchase.setEstado(EstadoCompra.PENDIENTE);
        purchase.setMontoTotal(new BigDecimal("500.00"));
        purchase.setFechaHora(LocalDateTime.now());
        purchase.setCodigoVerificacion("TEST-123");
        entityManager.persist(purchase);

        CompraDetalleEntity detail = new CompraDetalleEntity();
        detail.setProductoEntity(product);
        detail.setCantidad(1);
        detail.setPrecioUnitario(new BigDecimal("500.00"));
        detail.setSubtotal(new BigDecimal("500.00"));
        purchase.addDetalle(detail);

        RetiroProgramadoEntity pickup = new RetiroProgramadoEntity();
        pickup.setCompraEntity(purchase);
        pickup.setFranja(slot);
        pickup.setFechaProgramada(pickupDate);
        pickup.setEstado(EstadoRetiro.PROGRAMADO);
        purchase.setRetiroProgramadoEntity(pickup);
        entityManager.persist(pickup);
        entityManager.flush();
        entityManager.clear();

        return new ScheduledPickupFixture(buffet.getId(), purchase.getId(), slot.getId());
    }

    private ProductoEntity generateProduct() {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setDescripcion("Cat Scheduled");
        category.setActivo(true);
        entityManager.persist(category);

        ProductoEntity product = new ProductoEntity();
        product.setNombre("Scheduled Product");
        product.setDescripcion("Desc");
        product.setPrecio(new BigDecimal("500.00"));
        product.setRequierePreparacion(false);
        product.setActivo(true);
        product.setCategoria(category);
        entityManager.persist(product);

        return product;
    }

    private FranjaHorariaEntity generateTimeSlot() {
        FranjaHorariaEntity slot = new FranjaHorariaEntity();
        slot.setColegioEntity(getCachedSchool());
        slot.setDescripcion("Morning");
        slot.setHoraInicio(LocalTime.of(9, 0));
        slot.setHoraFin(LocalTime.of(10, 0));
        slot.setActivo(true);
        entityManager.persist(slot);

        return slot;
    }

    private record ScheduledPickupFixture(UUID buffetId, UUID purchaseId, UUID slotId) {
    }
}
