package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.*;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.*;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class SugerenciaIARepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SugerenciaIARepositoryImpl repository;

    @Autowired
    private SugerenciaIAJpaRepository springDataRepository;

    @Autowired
    private AIEventJpaRepository aiEventJpaRepository;

    @Autowired
    private NotificacionJpaRepository notificationJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardar_ShouldPersistEntity() {
        AlumnoEntity student = generateStudent();
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setTipo(TipoSugerenciaIA.ALERTA_PRECIO);
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        
        repository.guardar(entity);
        entityManager.flush();
        entityManager.clear();
        
        assertNotNull(entity.getId());
        assertTrue(springDataRepository.findById(entity.getId()).isPresent());
    }

    @Test
    void findByAlumnoId_ShouldReturnEntities() {
        AlumnoEntity student = generateStudent();
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setTipo(TipoSugerenciaIA.ALERTA_PRECIO);
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        springDataRepository.save(entity);
        entityManager.flush();

        List<SugerenciaIAEntity> result = repository.findByAlumnoId(student.getId());

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
    }

    @Test
    void savePriceAlert_ShouldPersistEventSuggestionAndNotification() {

        AlumnoEntity student = generateStudent();
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct();
        UsuarioEntity tutorUser = generateUser(RolUsuario.PADRE);
        
        generateTutorWithStudent(tutorUser, student);

        PriceAlertSuggestion suggestion = createPriceAlertSuggestion(student, product, buffet, tutorUser);

        repository.savePriceAlert(suggestion);
        entityManager.flush();
        entityManager.clear();

        assertEventWasPersisted();
        assertSuggestionWasPersisted();
        assertNotificationWasPersisted(tutorUser);
    }

    @Test
    void hasRecentPriceAlert_ShouldReturnTrueIfRecent() {
        AlumnoEntity student = generateStudent();
        ProductoEntity product = generateProduct();
        
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setProductoEntity(product);
        entity.setTipo(TipoSugerenciaIA.ALERTA_PRECIO);
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        springDataRepository.save(entity);
        entityManager.flush();

        boolean result = repository.hasRecentPriceAlert(student.getId(), product.getId(), LocalDateTime.now().minusMinutes(5));

        assertTrue(result);
    }
    
    @Test
    void findById_ShouldReturnOptionalEntity() {
        AlumnoEntity student = generateStudent();
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setTipo(TipoSugerenciaIA.ALERTA_PRECIO);
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        springDataRepository.save(entity);
        entityManager.flush();

        Optional<SugerenciaIAEntity> result = repository.findById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
    }

    @Test
    void findByAlumnoIdAndTipo_ShouldReturnMatchingEntities() {
        AlumnoEntity student = generateStudent();
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setTipo(TipoSugerenciaIA.ALERTA_PRECIO);
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        springDataRepository.save(entity);
        entityManager.flush();

        List<SugerenciaIAEntity> result = repository.findByAlumnoIdAndTipo(student.getId(), TipoSugerenciaIA.ALERTA_PRECIO);

        assertEquals(1, result.size());
    }

    @Test
    void findProductIdsByAlumnoIdAndTipo_ShouldReturnProductIdsFromEntityAndMetadata() {
        // Arrange
        AlumnoEntity student = generateStudent();
        ProductoEntity product = generateProduct();
        UUID metadataProductId = UUID.randomUUID();
        
        generateSuggestionWithProduct(student, product);
        generateSuggestionWithMetadataProduct(student, metadataProductId);

        entityManager.flush();

        // Act
        List<UUID> result = repository.findProductIdsByAlumnoIdAndTipo(student.getId(), TipoSugerenciaIA.ALERTA_PRECIO);

        // Assert
        assertContainsProductIds(result, product.getId(), metadataProductId);
    }

    private AlumnoEntity generateStudent() {
        UsuarioEntity user = generateUser(RolUsuario.ALUMNO);
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(user);
        student.setActivo(true);
        entityManager.persist(student);
        return student;
    }

    private UsuarioEntity generateUser(RolUsuario role) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Test");
        user.setApellido("User");
        user.setEmail("user" + UUID.randomUUID().toString().substring(0, 5) + "@test.com");
        user.setRol(role);
        user.setActivo(true);
        entityManager.persist(user);
        return user;
    }

    private BuffetEntity generateBuffet() {
        ColegioEntity school = new ColegioEntity();
        school.setNombre("School " + UUID.randomUUID().toString().substring(0, 5));
        school.setCue("CUE-" + UUID.randomUUID().toString().substring(0, 5));
        school.setActivo(true);
        entityManager.persist(school);
        
        BuffetEntity buffet = new BuffetEntity();
        buffet.setNombre("Test Buffet");
        buffet.setColegioEntity(school);
        buffet.setActivo(true);
        entityManager.persist(buffet);
        
        KiosqueroEntity vendor = new KiosqueroEntity();
        vendor.setUsuario(generateUser(RolUsuario.VENDEDOR));
        vendor.setBuffetEntity(buffet);
        vendor.setCuit("123456789");
        entityManager.persist(vendor);
        
        buffet.setVendedor(vendor);
        entityManager.merge(buffet);
        return buffet;
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
        product.setPrecio(new BigDecimal("100.00"));
        product.setActivo(true);
        product.setRequierePreparacion(false);
        entityManager.persist(product);
        return product;
    }

    private void generateTutorWithStudent(UsuarioEntity tutorUser, AlumnoEntity student) {
        TutorEntity tutor = new TutorEntity();
        tutor.setUsuario(tutorUser);
        tutor.setActiva(true);
        entityManager.persist(tutor);
        
        tutor.addHijo(student);
        entityManager.merge(tutor);
    }

    private PriceAlertSuggestion createPriceAlertSuggestion(AlumnoEntity student, ProductoEntity product, BuffetEntity buffet, UsuarioEntity tutorUser) {
        return new PriceAlertSuggestion(
                student.getId(),
                student.getUsuario().getId(),
                product.getId(),
                buffet.getId(),
                Set.of(tutorUser.getId()),
                "Alfajor",
                "Ajuste de precio",
                "El producto cambio de precio",
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                "Cambio del 20%"
        );
    }

    private void assertEventWasPersisted() {
        List<AIEventEntity> events = aiEventJpaRepository.findAll();
        assertFalse(events.isEmpty());
        AIEventEntity event = events.stream().filter(e -> e.getType() == AIEventType.ALERTA_PRECIO).findFirst().orElseThrow();
        assertEquals(AIEventType.ALERTA_PRECIO, event.getType());
    }

    private void assertSuggestionWasPersisted() {
        List<SugerenciaIAEntity> suggestions = springDataRepository.findAll();
        assertFalse(suggestions.isEmpty());
        SugerenciaIAEntity savedSuggestion = suggestions.stream().filter(s -> s.getTipo() == TipoSugerenciaIA.ALERTA_PRECIO).findFirst().orElseThrow();
        assertEquals(TipoSugerenciaIA.ALERTA_PRECIO, savedSuggestion.getTipo());
        assertEquals("Ajuste de precio", savedSuggestion.getTitulo());
    }

    private void assertNotificationWasPersisted(UsuarioEntity tutorUser) {
        List<NotificacionEntity> notifications = notificationJpaRepository.findAll();
        assertFalse(notifications.isEmpty());
        NotificacionEntity notification = notifications.stream().filter(n -> n.getTipo() == TipoNotificacion.SUGERENCIA_IA).findFirst().orElseThrow();
        assertEquals(TipoNotificacion.SUGERENCIA_IA, notification.getTipo());
        assertEquals(tutorUser.getId(), notification.getUsuarioEntity().getId());
    }

    private void generateSuggestionWithProduct(AlumnoEntity student, ProductoEntity product) {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setProductoEntity(product);
        entity.setTipo(TipoSugerenciaIA.ALERTA_PRECIO);
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        springDataRepository.save(entity);
    }

    private void generateSuggestionWithMetadataProduct(AlumnoEntity student, UUID metadataProductId) {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setTipo(TipoSugerenciaIA.ALERTA_PRECIO);
        entity.setMetadata(Map.of("productoId", metadataProductId.toString()));
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        springDataRepository.save(entity);
    }

    private void assertContainsProductIds(List<UUID> result, UUID productId, UUID metadataProductId) {
        assertEquals(2, result.size());
        assertTrue(result.contains(productId));
        assertTrue(result.contains(metadataProductId));
    }
}
