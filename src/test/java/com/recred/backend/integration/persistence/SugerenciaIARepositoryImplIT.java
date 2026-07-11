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

import static com.recred.backend.domain.models.TipoSugerenciaIA.*;
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

    private UsuarioEntity cachedTutor;

    @Test
    void guardar_ShouldPersistEntity() {
        SugerenciaIAEntity entity = givenNewSuggestionEntity();
        
        whenSavingSuggestion(entity);
        
        thenSuggestionIsPersisted(entity.getId());
    }

    @Test
    void findByAlumnoId_ShouldReturnEntities() {
        SugerenciaIAEntity entity = givenExistingSuggestion();
        
        List<SugerenciaIAEntity> result = whenFindingSuggestionsByStudentId(entity.getAlumnoEntity().getId());
        
        thenSuggestionIsReturnedCorrectly(result, entity.getId());
    }

    @Test
    void savePriceAlert_ShouldPersistEventSuggestionAndNotification() {
        PriceAlertSuggestion suggestion = givenPriceAlertContext();

        whenSavingPriceAlert(suggestion);

        thenEventSuggestionAndNotificationArePersisted();
    }

    @Test
    void hasRecentPriceAlert_ShouldReturnTrueIfRecent() {
        SugerenciaIAEntity entity = givenExistingSuggestionWithProduct();
        
        boolean result = whenCheckingRecentPriceAlert(
                entity.getAlumnoEntity().getId(),
                entity.getProductoEntity().getId(),
                LocalDateTime.now().minusMinutes(5)
        );

        thenResultIsTrue(result);
    }
    
    @Test
    void findById_ShouldReturnOptionalEntity() {
        SugerenciaIAEntity entity = givenExistingSuggestion();
        
        Optional<SugerenciaIAEntity> result = whenFindingSuggestionById(entity.getId());
        
        thenSuggestionIsFound(result, entity.getId());
    }

    @Test
    void findByAlumnoIdAndTipo_ShouldReturnMatchingEntities() {
        SugerenciaIAEntity entity = givenExistingSuggestion();
        
        List<SugerenciaIAEntity> result = whenFindingSuggestionsByStudentIdAndType(
                entity.getAlumnoEntity().getId(),
                ALERTA_PRECIO
        );
        
        thenSuggestionListHasSize(result, 1);
    }

    @Test
    void findProductIdsByAlumnoIdAndTipo_ShouldReturnProductIdsFromEntityAndMetadata() {
        SugerenciaContext ctx = givenSuggestionsWithEntityAndMetadataProduct();

        List<UUID> result = whenFindingProductIdsByStudentIdAndType(ctx.student().getId(), ALERTA_PRECIO);

        thenContainsBothProductIds(result, ctx.productId(), ctx.metadataProductId());
    }

    private SugerenciaIAEntity givenNewSuggestionEntity() {
        AlumnoEntity student = generateStudent();
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setTipo(ALERTA_PRECIO);
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        return entity;
    }

    private SugerenciaIAEntity givenExistingSuggestion() {
        SugerenciaIAEntity entity = givenNewSuggestionEntity();
        springDataRepository.save(entity);
        entityManager.flush();
        return entity;
    }

    private PriceAlertSuggestion givenPriceAlertContext() {
        AlumnoEntity student = generateStudent();
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct();
        this.cachedTutor = generateUser(RolUsuario.PADRE);
        
        generateTutorWithStudent(this.cachedTutor, student);

        return createPriceAlertSuggestion(student, product, buffet, this.cachedTutor);
    }

    private SugerenciaIAEntity givenExistingSuggestionWithProduct() {
        AlumnoEntity student = generateStudent();
        ProductoEntity product = generateProduct();
        
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setProductoEntity(product);
        entity.setTipo(ALERTA_PRECIO);
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        springDataRepository.save(entity);
        entityManager.flush();
        return entity;
    }

    private SugerenciaContext givenSuggestionsWithEntityAndMetadataProduct() {
        AlumnoEntity student = generateStudent();
        ProductoEntity product = generateProduct();
        UUID metadataProductId = UUID.randomUUID();
        
        generateSuggestionWithProduct(student, product);
        generateSuggestionWithMetadataProduct(student, metadataProductId);

        entityManager.flush();
        return new SugerenciaContext(student, product.getId(), metadataProductId);
    }

    private void whenSavingSuggestion(SugerenciaIAEntity entity) {
        repository.guardar(entity);
        entityManager.flush();
        entityManager.clear();
    }

    private List<SugerenciaIAEntity> whenFindingSuggestionsByStudentId(UUID studentId) {
        return repository.findByAlumnoId(studentId);
    }

    private void whenSavingPriceAlert(PriceAlertSuggestion suggestion) {
        repository.savePriceAlert(suggestion);
        entityManager.flush();
        entityManager.clear();
    }

    private boolean whenCheckingRecentPriceAlert(UUID studentId, UUID productId, LocalDateTime since) {
        return repository.hasRecentPriceAlert(studentId, productId, since);
    }

    private Optional<SugerenciaIAEntity> whenFindingSuggestionById(UUID id) {
        return repository.findById(id);
    }

    private List<SugerenciaIAEntity> whenFindingSuggestionsByStudentIdAndType(UUID studentId, TipoSugerenciaIA type) {
        return repository.findByAlumnoIdAndTipo(studentId, type);
    }

    private List<UUID> whenFindingProductIdsByStudentIdAndType(UUID studentId, TipoSugerenciaIA type) {
        return repository.findProductIdsByAlumnoIdAndTipo(studentId, type);
    }

    private void thenSuggestionIsPersisted(UUID id) {
        assertNotNull(id);
        assertTrue(springDataRepository.findById(id).isPresent());
    }

    private void thenSuggestionIsReturnedCorrectly(List<SugerenciaIAEntity> result, UUID expectedId) {
        assertEquals(1, result.size());
        assertEquals(expectedId, result.get(0).getId());
    }

    private void thenEventSuggestionAndNotificationArePersisted() {
        assertEventWasPersisted();
        assertSuggestionWasPersisted();
        assertNotificationWasPersisted(this.cachedTutor);
    }

    private void thenResultIsTrue(boolean result) {
        assertTrue(result);
    }

    private void thenSuggestionIsFound(Optional<SugerenciaIAEntity> result, UUID expectedId) {
        assertTrue(result.isPresent());
        assertEquals(expectedId, result.get().getId());
    }

    private void thenSuggestionListHasSize(List<SugerenciaIAEntity> result, int expectedSize) {
        assertEquals(expectedSize, result.size());
    }

    private void thenContainsBothProductIds(List<UUID> result, UUID productId, UUID metadataProductId) {
        assertEquals(2, result.size());
        assertTrue(result.contains(productId));
        assertTrue(result.contains(metadataProductId));
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
        SugerenciaIAEntity savedSuggestion = suggestions.stream().filter(s -> s.getTipo() == ALERTA_PRECIO).findFirst().orElseThrow();
        assertEquals(ALERTA_PRECIO, savedSuggestion.getTipo());
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
        entity.setTipo(ALERTA_PRECIO);
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        springDataRepository.save(entity);
    }

    private void generateSuggestionWithMetadataProduct(AlumnoEntity student, UUID metadataProductId) {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAlumnoEntity(student);
        entity.setTipo(ALERTA_PRECIO);
        entity.setMetadata(Map.of("productoId", metadataProductId.toString()));
        entity.setMensaje("Mensaje de prueba");
        entity.setTitulo("Titulo de prueba");
        springDataRepository.save(entity);
    }

    private record SugerenciaContext(AlumnoEntity student, UUID productId, UUID metadataProductId) {}
}
