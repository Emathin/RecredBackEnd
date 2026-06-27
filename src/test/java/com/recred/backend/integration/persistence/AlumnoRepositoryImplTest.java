package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.AlumnoRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataAlumnoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataCategoryRepository;
import com.recred.backend.infrastructure.persistence.SpringDataClasificacionSaludRepository;
import com.recred.backend.infrastructure.persistence.SpringDataProductRepository;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class AlumnoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private SpringDataAlumnoRepository springDataRepository;

    @Autowired
    private SpringDataUsuarioRepository usuarioRepository;

    @Autowired
    private SpringDataClasificacionSaludRepository clasificacionSaludRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private SpringDataProductRepository productoRepository;

    @Autowired
    private AlumnoRepositoryImpl alumnoRepositoryImpl;

    private UsuarioEntity cachedUser;
    private ClasificacionSaludEntity cachedHealthClassification;
    private CategoriaProductoEntity cachedProductCategory;
    private ProductoEntity cachedFavProduct;
    private ProductoEntity cachedBlockedProduct;

    @BeforeEach
    void setUp() {

        generateTestUser();
        generateHealthClassificationEntity();
        generateProductCategoryEntity();
        generateFavoriteProduct();
        generateBlockedProduct();
    }

    @Test
    void save_ShouldSaveInRealDatabaseAndGenerateId() {
        Alumno domainStudent = prepareDomainModelStudent();

        Alumno savedStudent = alumnoRepositoryImpl.save(domainStudent);

        assertNotNull(savedStudent.getId(), "Database should have generated the Student UUID");

        Optional<AlumnoEntity> studentInDb = springDataRepository.findById(savedStudent.getId());
        assertTrue(studentInDb.isPresent(), "Student should exist in database");

        AlumnoEntity realEntity = studentInDb.get();

        assertEquals(cachedUser.getId(), realEntity.getUsuario().getId(), "Foreign key to User must match");
        assertEquals(1, realEntity.getRestriccionesSalud().size(), "Should have inserted the ManyToMany relationship for health classifications");
        assertEquals(1, realEntity.getProductosFavoritos().size(), "Should have inserted the ManyToMany relationship for favorite products");
        assertEquals(1, realEntity.getProductosBloqueados().size(), "Should have inserted the ManyToMany relationship for blocked products");
    }

    @Test
    void findById_ShouldMapToDomainFromRealDb() {
        AlumnoEntity entity = springDataRepository.saveAndFlush(createStudentEntityWithHealthClassificationAndFavProduct());

        Optional<Alumno> foundStudent = alumnoRepositoryImpl.findById(entity.getId());

        assertTrue(foundStudent.isPresent());
        Alumno mappedStudent = foundStudent.get();
        assertEquals(entity.getId(), mappedStudent.getId());
        assertEquals(1, mappedStudent.getRestriccionesSalud().size());
        assertEquals(cachedHealthClassification.getId(), mappedStudent.getRestriccionesSalud().iterator().next().getId());
    }

    @Test
    void findStudentIdByUserId_ShouldSelectByUserIdAndReturnStudentId() {
        AlumnoEntity studentEntity = springDataRepository.saveAndFlush(createStudentEntityWithHealthClassificationAndFavProduct());

        Optional<UUID> result = alumnoRepositoryImpl.findStudentIdByUserId(cachedUser.getId());

        assertTrue(result.isPresent());
        assertEquals(studentEntity.getId(), result.get());
    }

    @Test
    void findAll_ShouldFetchAllFromDb() {
        AlumnoEntity studentEntity = springDataRepository.saveAndFlush(createStudentEntityWithHealthClassificationAndFavProduct());

        List<Alumno> result = alumnoRepositoryImpl.findAll();

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(a -> a.getId().equals(studentEntity.getId())));
    }

    @Test
    void existsById_ShouldExecuteExistsQueryInDatabase() {
        AlumnoEntity studentEntity = springDataRepository.saveAndFlush(createStudentEntityWithHealthClassificationAndFavProduct());

        boolean result = alumnoRepositoryImpl.existsById(studentEntity.getId());

        assertTrue(result);
    }

    private ProductoEntity generateBlockedProduct() {
        if (this.cachedBlockedProduct == null) {
            ProductoEntity blockedProduct = new ProductoEntity();
            blockedProduct.setNombre("Candies");
            blockedProduct.setDescripcion("Candy with sugar");
            blockedProduct.setPrecio(BigDecimal.valueOf(50));
            blockedProduct.setCategoria(generateProductCategoryEntity());
            blockedProduct.setRequierePreparacion(false);
            this.cachedBlockedProduct = productoRepository.saveAndFlush(blockedProduct);
        }
        return this.cachedBlockedProduct;
    }

    private ProductoEntity generateFavoriteProduct() {
        if (this.cachedFavProduct == null) {
            ProductoEntity favProduct = new ProductoEntity();
            favProduct.setNombre("Rice Cookie");
            favProduct.setDescripcion("Gluten-free cookie");
            favProduct.setPrecio(BigDecimal.valueOf(100));
            favProduct.setCategoria(generateProductCategoryEntity());
            favProduct.setRequierePreparacion(false);
            this.cachedFavProduct = productoRepository.saveAndFlush(favProduct);
        }
        return this.cachedFavProduct;
    }

    private CategoriaProductoEntity generateProductCategoryEntity() {
        if (this.cachedProductCategory == null) {
            CategoriaProductoEntity category = new CategoriaProductoEntity();
            category.setDescripcion("Snacks");
            this.cachedProductCategory = categoryRepository.saveAndFlush(category);
        }
        return this.cachedProductCategory;
    }

    private ClasificacionSaludEntity generateHealthClassificationEntity() {
        if (this.cachedHealthClassification == null) {
            ClasificacionSaludEntity classification = new ClasificacionSaludEntity();
            classification.setDescripcion("Celiac");
            this.cachedHealthClassification = clasificacionSaludRepository.saveAndFlush(classification);
        }
        return this.cachedHealthClassification;
    }

    private UsuarioEntity generateTestUser() {
        if (this.cachedUser == null) {
            UsuarioEntity userEntity = new UsuarioEntity();
            userEntity.setIdCognito("cognito_sub_test");
            userEntity.setNombre("John");
            userEntity.setApellido("Doe");
            userEntity.setEmail("john.doe@test.com");
            userEntity.setRol(RolUsuario.ALUMNO);
            userEntity.setAutenticadoConCognito(true);
            this.cachedUser = usuarioRepository.saveAndFlush(userEntity);
        }
        return this.cachedUser;
    }

    private Alumno prepareDomainModelStudent() {
        Alumno domainStudent = new Alumno();

        Usuario domainUser = new Usuario();
        domainUser.setId(cachedUser.getId());
        domainStudent.setUsuario(domainUser);
        domainStudent.setActivo(true);

        ClasificacionSalud domainClassification = new ClasificacionSalud();
        domainClassification.setId(cachedHealthClassification.getId());
        domainStudent.agregarRestriccionSalud(domainClassification);

        Producto domainFavProduct = new Producto();
        domainFavProduct.setId(cachedFavProduct.getId());
        domainStudent.agregarProductoFavorito(domainFavProduct);

        Producto domainBlockedProduct = new Producto();
        domainBlockedProduct.setId(cachedBlockedProduct.getId());
        domainStudent.bloquearProducto(domainBlockedProduct);
        return domainStudent;
    }

    private AlumnoEntity createStudentEntityWithHealthClassificationAndFavProduct() {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setUsuario(usuarioRepository.getReferenceById(cachedUser.getId()));
        entity.getRestriccionesSalud().add(clasificacionSaludRepository.getReferenceById(cachedHealthClassification.getId()));
        entity.getProductosFavoritos().add(productoRepository.getReferenceById(cachedFavProduct.getId()));
        return entity;
    }

}
