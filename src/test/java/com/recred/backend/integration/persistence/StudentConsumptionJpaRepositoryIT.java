package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.StudentConsumptionJpaRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class StudentConsumptionJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private StudentConsumptionJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    private AlumnoEntity cachedStudent;
    private ColegioEntity cachedSchool;
    private KiosqueroEntity cachedVendor;
    private BuffetEntity cachedBuffet;

    @Test
    void mapPurchaseEntitiesWithProductsSortedByProductName() {

        AlumnoEntity student = generateStudent();
        BuffetEntity buffet = generateBuffet();
        CompraPresencialEntity purchase = generateTestPurchaseWithProducts(student, buffet);


        List<StudentConsumptionPurchase> purchases = repository.findPurchasesByStudentIdAndDateRange(
                student.getId(),
                List.of(EstadoCompra.ENTREGADO),
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 31, 23, 59)
        );


        assertPurchaseAndProductsAreMappedAndSorted(purchases, purchase, buffet);
    }

    private CompraPresencialEntity generateTestPurchaseWithProducts(AlumnoEntity student, BuffetEntity buffet) {
        CategoriaProductoEntity drinksCategory = generateCategory("Drinks");
        CategoriaProductoEntity foodCategory = generateCategory("Food");

        ProductoEntity water = generateProduct("Water", drinksCategory);
        ProductoEntity sandwich = generateProduct("Sandwich", foodCategory);

        CompraPresencialEntity purchase = generatePurchase(student, buffet, new BigDecimal("1500.00"), LocalDateTime.of(2026, 5, 20, 10, 30));
        CompraDetalleEntity detail1 = generateDetail(purchase, water, 1, new BigDecimal("300.00"));
        CompraDetalleEntity detail2 = generateDetail(purchase, sandwich, 1, new BigDecimal("1200.00"));
        
        purchase.setDetalles(new java.util.ArrayList<>(List.of(detail1, detail2)));
        entityManager.merge(purchase);
        
        entityManager.flush();
        entityManager.clear();
        
        return purchase;
    }

    private void assertPurchaseAndProductsAreMappedAndSorted(List<StudentConsumptionPurchase> purchases, CompraPresencialEntity purchase, BuffetEntity buffet) {
        assertEquals(1, purchases.size());
        StudentConsumptionPurchase mapped = purchases.get(0);
        
        assertEquals(purchase.getId(), mapped.purchaseId());
        assertEquals(purchase.getFechaHora(), mapped.dateTime());
        assertEquals(EstadoCompra.ENTREGADO, mapped.status());
        assertEquals(MedioPago.CREDITOS, mapped.paymentMethod());
        assertEquals(new BigDecimal("1500.00"), mapped.totalAmount());
        assertEquals(buffet.getId(), mapped.buffetId());
        assertEquals("Main Buffet", mapped.buffetName());
        
        assertEquals("Sandwich", mapped.products().get(0).productName());
        assertEquals("Water", mapped.products().get(1).productName());
        assertEquals("Food", mapped.products().get(0).categoryName());
        assertEquals("Drinks", mapped.products().get(1).categoryName());
        assertEquals(new BigDecimal("1200.00"), mapped.products().get(0).unitPrice());
        assertEquals(new BigDecimal("1200.00"), mapped.products().get(0).subtotal());
        assertEquals(new BigDecimal("300.00"), mapped.products().get(1).unitPrice());
        assertEquals(new BigDecimal("300.00"), mapped.products().get(1).subtotal());
    }

    private CompraPresencialEntity generatePurchase(AlumnoEntity student, BuffetEntity buffet, BigDecimal amount, LocalDateTime date) {
        CompraPresencialEntity purchase = new CompraPresencialEntity();
        purchase.setAlumnoEntity(student);
        purchase.setBuffetEntity(buffet);
        purchase.setFechaHora(date);
        purchase.setEstado(EstadoCompra.ENTREGADO);
        purchase.setMedioPago(MedioPago.CREDITOS);
        purchase.setMontoTotal(amount);
        entityManager.persist(purchase);
        return purchase;
    }

    private CompraDetalleEntity generateDetail(CompraPresencialEntity purchase, ProductoEntity product, int quantity, BigDecimal unitPrice) {
        CompraDetalleEntity detail = new CompraDetalleEntity();
        detail.setCompra(purchase);
        detail.setProductoEntity(product);
        detail.setCantidad(quantity);
        detail.setPrecioUnitario(unitPrice);
        detail.setSubtotal(unitPrice.multiply(new BigDecimal(quantity)));
        entityManager.persist(detail);
        return detail;
    }

    private AlumnoEntity generateStudent() {
        if (this.cachedStudent == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-student-cons");
            user.setNombre("Test");
            user.setApellido("User");
            user.setEmail("user.cons@test.com");
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

    private ColegioEntity getCachedSchool() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("School Cons");
            school.setCue("CUE-CONS");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private BuffetEntity generateBuffet() {
        if (this.cachedBuffet == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setNombre("Main Buffet");
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
            vendorUser.setIdCognito("sub-vendor-cons");
            vendorUser.setNombre("Vendor");
            vendorUser.setApellido("User");
            vendorUser.setEmail("vendor.cons@test.com");
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

    private CategoriaProductoEntity generateCategory(String name) {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setDescripcion(name);
        category.setActivo(true);
        entityManager.persist(category);
        return category;
    }

    private ProductoEntity generateProduct(String name, CategoriaProductoEntity category) {
        ProductoEntity product = new ProductoEntity();
        product.setCategoria(category);
        product.setNombre(name);
        product.setDescripcion("Desc");
        product.setPrecio(new BigDecimal("100"));
        product.setActivo(true);
        product.setRequierePreparacion(false);
        entityManager.persist(product);
        return product;
    }
}
