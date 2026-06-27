package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CarritoFavorito;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.*;
import com.recred.backend.integration.AbstractIntegrationTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class CarritoFavoritoRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SpringDataCarritoFavoritoRepository springDataRepository;

    @Autowired
    private SpringDataTutorRepository springDataTutorRepository;

    @Autowired
    private SpringDataAlumnoRepository springDataAlumnoRepository;

    @Autowired
    private SpringDataProductRepository springDataProductRepository;

    @Autowired
    private SpringDataUsuarioRepository usuarioRepository;

    @Autowired
    private SpringDataColegioRepository colegioRepository;

    @Autowired
    private SpringDataGradoRepository gradoRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private CarritoFavoritoRepositoryImpl repository;

    private UUID tutorId;
    private UUID alumnoId;
    private UUID productId;
    private UUID cartId;
    private CategoriaProductoEntity cachedCategory;

    @BeforeEach
    void setUp() {
        prepareRelationalEnvironment();
    }

    @Test
    void saveShouldCreateEntityWithReferencesAndItems() {
        CarritoFavorito cart = buildCartDomain(null, tutorId, alumnoId, productId, 2);

        CarritoFavorito result = repository.save(cart);

        Optional<CarritoFavoritoEntity> savedEntity = springDataRepository.findById(result.getId());
        assertTrue(savedEntity.isPresent());
        CarritoFavoritoEntity saved = savedEntity.get();
        assertEquals("Merienda", saved.getNombre());
        assertEquals(tutorId, saved.getTutor().getId());
        assertEquals(alumnoId, saved.getAlumno().getId());
        assertEquals(1, saved.getItems().size());
        assertEquals(productId, saved.getItems().get(0).getProductoEntity().getId());
        assertEquals(2, saved.getItems().get(0).getCantidad());
    }

    @Test
    void saveShouldUpdateExistingEntityAndClearOldItems() {
        UUID newProductId = generateProduct().getId();
        CarritoFavorito cart = buildCartDomain(cartId, tutorId, alumnoId, newProductId, 3);

        CarritoFavorito result = repository.save(cart);

        Optional<CarritoFavoritoEntity> updatedEntity = springDataRepository.findById(cartId);
        assertTrue(updatedEntity.isPresent());
        CarritoFavoritoEntity existing = updatedEntity.get();
        assertEquals("Merienda", existing.getNombre());
        assertEquals(1, existing.getItems().size());
        assertEquals(newProductId, existing.getItems().get(0).getProductoEntity().getId());
        assertEquals(3, existing.getItems().get(0).getCantidad());
    }

    @Test
    void saveShouldIgnoreItemsWithoutProductId() {

        CarritoFavorito result = repository.save(createFavCart());

        Optional<CarritoFavoritoEntity> savedEntity = springDataRepository.findById(result.getId());
        assertTrue(savedEntity.isPresent());
        assertTrue(savedEntity.get().getItems().isEmpty());
    }


    @Test
    void findByIdShouldMapEntityWhenPresent() {
        Optional<CarritoFavorito> result = repository.findById(cartId);

        assertTrue(result.isPresent());
        assertEquals(cartId, result.get().getId());
    }

    @Test
    void findByTutorIdAndFiltersShouldMapEntities() {
        List<CarritoFavorito> result = repository.findByTutorIdAndFilters(
                tutorId,
                alumnoId,
                productId
        );

        assertEquals(1, result.size());
        assertEquals(cartId, result.get(0).getId());
    }

    @Test
    void deleteShouldDelegateToSpringDataRepository() {
        repository.delete(cartId);

        Optional<CarritoFavoritoEntity> deletedEntity = springDataRepository.findById(cartId);
        assertTrue(deletedEntity.isEmpty());
    }

    @NotNull
    private CarritoFavorito createFavCart() {
        CarritoFavorito cart = new CarritoFavorito();
        cart.setNombre("Merienda");
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        cart.setTutor(tutor);
        cart.setAlumno(alumno);
        cart.setItems(Map.of(new Producto(), 1));
        return cart;
    }

    private void prepareRelationalEnvironment() {
        TutorEntity tutor = generateTutor();
        this.tutorId = tutor.getId();

        ColegioEntity school = generateSchool();
        GradoEntity grade = generateGrade(school);
        AlumnoEntity student = generateStudent(grade);
        this.alumnoId = student.getId();

        ProductoEntity product = generateProduct();
        this.productId = product.getId();

        CarritoFavoritoEntity cartEntity = generateCart(tutor, student, product);
        this.cartId = cartEntity.getId();
    }

    private TutorEntity generateTutor() {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("tutor_" + UUID.randomUUID());
        user.setNombre("Tutor");
        user.setApellido("Test");
        user.setEmail("tutor_" + UUID.randomUUID() + "@test.com");
        user.setRol(RolUsuario.PADRE);
        user.setAutenticadoConCognito(true);
        user = usuarioRepository.saveAndFlush(user);

        TutorEntity tutor = new TutorEntity();
        tutor.setUsuario(user);
        return springDataTutorRepository.saveAndFlush(tutor);
    }

    private ColegioEntity generateSchool() {
        ColegioEntity school = new ColegioEntity();
        school.setNombre("Colegio " + UUID.randomUUID());
        school.setCue("12345678");
        return colegioRepository.saveAndFlush(school);
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
        user.setApellido("Test");
        user.setEmail("alumno_" + UUID.randomUUID() + "@test.com");
        user.setRol(RolUsuario.ALUMNO);
        user.setAutenticadoConCognito(true);
        user = usuarioRepository.saveAndFlush(user);

        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(user);
        student.setGradoEntity(grade);
        return springDataAlumnoRepository.saveAndFlush(student);
    }

    private CategoriaProductoEntity getCategory() {
        if (cachedCategory == null) {
            cachedCategory = new CategoriaProductoEntity();
            cachedCategory.setDescripcion("Snacks");
            cachedCategory = categoryRepository.saveAndFlush(cachedCategory);
        }
        return cachedCategory;
    }

    private ProductoEntity generateProduct() {
        CategoriaProductoEntity category = getCategory();

        ProductoEntity product = new ProductoEntity();
        product.setNombre("Alfajor");
        product.setDescripcion("Alfajor simple");
        product.setPrecio(new BigDecimal("40.00"));
        product.setActivo(true);
        product.setRequierePreparacion(false);
        product.setCategoria(category);
        return springDataProductRepository.saveAndFlush(product);
    }

    private CarritoFavoritoEntity generateCart(TutorEntity tutor, AlumnoEntity student, ProductoEntity product) {
        CarritoFavoritoEntity cart = new CarritoFavoritoEntity();
        cart.setNombre("Viejo");
        cart.setTutor(tutor);
        cart.setAlumno(student);

        CarritoFavoritoItemEntity item = new CarritoFavoritoItemEntity();
        item.setProductoEntity(product);
        item.setCantidad(1);
        cart.addItem(item);

        return springDataRepository.saveAndFlush(cart);
    }

    private CarritoFavorito buildCartDomain(UUID cartId, UUID tutorId, UUID alumnoId, UUID productId, int quantity) {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Producto product = new Producto();
        product.setId(productId);

        CarritoFavorito cart = new CarritoFavorito();
        cart.setId(cartId);
        cart.setNombre("Merienda");
        cart.setTutor(tutor);
        cart.setAlumno(alumno);
        cart.setItems(Map.of(product, quantity));
        return cart;
    }
}
