package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.AIEventType;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.ProductSaleStatistics;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.AIEventJpaRepository;
import com.recred.backend.infrastructure.persistence.AIEventPersistenceRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class AIEventPersistenceRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private AIEventPersistenceRepository repository;

    @Autowired
    private AIEventJpaRepository aiEventJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void registerPrediction_ShouldPersistEventWithInputAndResultInRealDatabase() {
        AlumnoEntity student = persistStudent();

        repository.registerPrediction(predictionResult(student.getId()));
        flushAndClear();

        AIEventEntity event = onlyEventForStudent(AIEventType.PREDICCION_GASTO, student.getId());
        assertEquals(AIEventType.PREDICCION_GASTO, event.getType());
        assertEquals(student.getId(), event.getAlumnoEntity().getId());
        assertEquals("BACKEND_PREDICCION_GASTO", event.getInputOrigin());
        assertNumberEquals(new BigDecimal("0.8000"), event.getConfidence());
        assertEquals("MENSUAL", event.getInputData().get("periodo"));
        assertNumberEquals(new BigDecimal("120.00"), event.getResultData().get("gastoPredicho"));
        assertNumberEquals(
                new BigDecimal("80.00"),
                event.getResultData().get("porcentajePresupuesto")
        );

        List<Map<String, Object>> categories = categories(event);
        assertEquals("Bebidas", categories.get(0).get("descripcion"));
        assertNumberEquals(new BigDecimal("40.00"), categories.get(0).get("montoTotal"));

        Map<String, Object> aiAnalysis = aiAnalysis(event);
        assertEquals("Sube el gasto.", aiAnalysis.get("resumen"));
        assertEquals("gemini-test", aiAnalysis.get("modelo"));
    }

    @Test
    void findAllProductChangeRecommendations_ShouldMapEventsFromRealDatabase() {
        BuffetEntity buffet = persistBuffet();
        ProductoEntity product = persistProduct();
        persistProductChangeEvent(buffet, product, Map.of(), Map.of(
                "nombre", "Producto 1",
                "precio", 10.0
        ));
        flushAndClear();

        List<ProductRecommendationResult> results =
                repository.findAllProductChangeRecommendations(buffet.getId());

        assertEquals(1, results.size());
        ProductChangeRecommendation recommendation =
                results.get(0).getProductChangeRecommendation();
        assertEquals(product.getId(), recommendation.getId());
        assertEquals(buffet.getId(), recommendation.getIdBuffet());
        assertEquals("Producto 1", recommendation.getNombre());
    }

    @Test
    void findLatestProductChangeRecommendation_ShouldMapAnalysisAndStatisticsFromRealDatabase() {
        BuffetEntity buffet = persistBuffet();
        ProductoEntity product = persistProduct();
        Map<String, Object> statsMap = Map.of("ventasPeriodo", 5);
        persistProductChangeEvent(buffet, product, Map.of(
                "analisisIa", Map.of(
                        "resumen", "Conviene ajustar stock.",
                        "alertas", List.of("Baja rotacion"),
                        "recomendaciones", List.of("Promocionar"),
                        "modelo", "gemini-test"
                )
        ), Map.of(
                "nombre", "Producto 1",
                "precio", 10.0,
                "saleStatistics", statsMap
        ));
        flushAndClear();

        Optional<ProductRecommendationResult> result =
                repository.findLatestProductChangeRecommendation(buffet.getId());

        assertTrue(result.isPresent());
        assertTrue(result.orElseThrow().getAiAnalysis().isPresent());
        assertEquals(
                "Conviene ajustar stock.",
                result.orElseThrow().getAiAnalysis().orElseThrow().summary()
        );
        assertEquals(
                5L,
                result.orElseThrow()
                        .getProductChangeRecommendation()
                        .getSaleStatistics()
                        .getVentasPeriodo()
        );
    }

    @Test
    void registerProductChangeRecommendation_ShouldPersistInputAndAnalysisInRealDatabase() {
        BuffetEntity buffet = persistBuffet();
        ProductoEntity product = persistProduct();

        repository.registerProductChangeRecommendation(productChangeResult(
                buffet.getId(),
                product.getId(),
                Optional.of(new ProductChangeAnalysis(
                        "Subio la demanda.",
                        List.of("Poco stock"),
                        List.of("Reponer"),
                        "gemini-test"
                ))
        ));
        flushAndClear();

        AIEventEntity event = onlyEventForProduct(AIEventType.PRODUCTO_CAMBIO, product.getId());
        assertEquals(buffet.getId(), event.getBuffetEntity().getId());
        assertEquals(product.getId(), event.getProductoEntity().getId());
        assertEquals("Backend Product Reco", event.getInputOrigin());
        assertEquals("Alfajor", event.getInputData().get("nombre"));
        assertNumberEquals(new BigDecimal("10.00"), event.getInputData().get("precio"));
        assertEquals("Subio la demanda.", aiAnalysis(event).get("resumen"));
    }

    @Test
    void registerProductChangeRecommendation_WhenStatisticsExist_ShouldPersistThemInRealDatabase() {
        BuffetEntity buffet = persistBuffet();
        ProductoEntity product = persistProduct();
        ProductSaleStatistics statistics = new ProductSaleStatistics();
        statistics.setVentasPeriodo(7L);
        ProductChangeRecommendation recommendation = productChangeRecommendation(
                buffet.getId(),
                product.getId()
        );
        recommendation.setSaleStatistics(statistics);

        repository.registerProductChangeRecommendation(
                new ProductRecommendationResult(recommendation, Optional.empty())
        );
        flushAndClear();

        AIEventEntity event = onlyEventForProduct(AIEventType.PRODUCTO_CAMBIO, product.getId());
        Map<String, Object> savedStatistics = mapValue(
                event.getInputData().get("saleStatistics")
        );
        assertNumberEquals(new BigDecimal("7"), savedStatistics.get("ventasPeriodo"));
    }

    @Test
    void registerProductAdditionRecommendation_ShouldPersistSuggestionInRealDatabase() {
        BuffetEntity buffet = persistBuffet();
        ProductoEntity productEntity = persistProduct();
        Producto product = new Producto();
        product.setId(productEntity.getId());
        product.setNombre("Barrita");

        repository.registerProductAdditionRecommendation(product, new Kiosquero(), buffet.getId());
        flushAndClear();

        AIEventEntity event = onlyEventForProduct(AIEventType.AGREGAR_PRODUCTO, product.getId());
        assertEquals(buffet.getId(), event.getBuffetEntity().getId());
        assertEquals(product.getId(), event.getProductoEntity().getId());
        assertTrue(event.getResultData().get("resumen").toString().contains("Barrita"));
    }

    @Test
    void registerPurchaseRecommendation_ShouldPersistAnalysisAndReturnSavedIdFromRealDatabase() {
        AlumnoEntity student = persistStudent();
        ProductoEntity product = persistProduct();

        UUID eventId = repository.registerPurchaseRecommendation(new PurchaseRecommendationResult(
                student.getId(),
                List.of(new PurchaseResume()),
                Optional.of(new PurchasePatternAnalysis(
                        "Compra habitual.",
                        product.getId(),
                        "Yogur",
                        "lunes"
                ))
        ));
        flushAndClear();

        assertNotNull(eventId);
        AIEventEntity event = aiEventJpaRepository.findById(eventId).orElseThrow();
        assertEquals(AIEventType.ANALISIS_CONSUMO, event.getType());
        assertEquals(student.getId(), event.getAlumnoEntity().getId());
        assertEquals(product.getId(), event.getProductoEntity().getId());
        Map<String, Object> analysis = aiAnalysis(event);
        assertEquals("Compra habitual.", analysis.get("resumen"));
        assertEquals(product.getId().toString(), analysis.get("Id_Producto").toString());
    }

    private AlumnoEntity persistStudent() {
        String suffix = UUID.randomUUID().toString();
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("student-" + suffix);
        user.setNombre("Test");
        user.setApellido("Student");
        user.setEmail("student-" + suffix + "@test.com");
        user.setRol(RolUsuario.ALUMNO);
        user.setActivo(true);
        entityManager.persist(user);

        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(user);
        student.setActivo(true);
        entityManager.persist(student);
        return student;
    }

    private BuffetEntity persistBuffet() {
        ColegioEntity school = new ColegioEntity();
        school.setNombre("AI Event School");
        school.setCue("AI-" + UUID.randomUUID());
        school.setActivo(true);
        entityManager.persist(school);

        BuffetEntity buffet = new BuffetEntity();
        buffet.setNombre("AI Event Buffet");
        buffet.setColegioEntity(school);
        buffet.setActivo(true);
        entityManager.persist(buffet);
        return buffet;
    }

    private ProductoEntity persistProduct() {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setDescripcion("AI Event Category " + UUID.randomUUID());
        category.setActivo(true);
        entityManager.persist(category);

        ProductoEntity product = new ProductoEntity();
        product.setNombre("AI Event Product");
        product.setDescripcion("Desc");
        product.setPrecio(new BigDecimal("500.00"));
        product.setRequierePreparacion(false);
        product.setActivo(true);
        product.setCategoria(category);
        entityManager.persist(product);
        return product;
    }

    private void persistProductChangeEvent(
            BuffetEntity buffet,
            ProductoEntity product,
            Map<String, Object> resultData,
            Map<String, Object> inputData
    ) {
        AIEventEntity event = new AIEventEntity();
        event.setType(AIEventType.PRODUCTO_CAMBIO);
        event.setBuffetEntity(buffet);
        event.setProductoEntity(product);
        event.setInputData(inputData);
        event.setResultData(resultData);
        entityManager.persist(event);
    }

    private SpendingPredictionResult predictionResult(UUID studentId) {
        return new SpendingPredictionResult(
                prediction(studentId),
                Optional.of(new SpendingPredictionAnalysis(
                        "Sube el gasto.",
                        List.of("Alerta."),
                        List.of("Revisar presupuesto."),
                        "gemini-test"
                ))
        );
    }

    private SpendingPrediction prediction(UUID studentId) {
        return new SpendingPrediction(
                studentId,
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("50.00"),
                new BigDecimal("120.00"),
                new BigDecimal("10.00"),
                new BigDecimal("150.00"),
                new BigDecimal("80.00"),
                new BigDecimal("0.8000"),
                60,
                7,
                List.of(new ConsumptionCategory("Bebidas", new BigDecimal("40.00")))
        );
    }

    private ProductRecommendationResult productChangeResult(
            UUID buffetId,
            UUID productId,
            Optional<ProductChangeAnalysis> analysis
    ) {
        return new ProductRecommendationResult(
                productChangeRecommendation(buffetId, productId),
                analysis
        );
    }

    private ProductChangeRecommendation productChangeRecommendation(
            UUID buffetId,
            UUID productId
    ) {
        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setId(productId);
        recommendation.setIdBuffet(buffetId);
        recommendation.setNombre("Alfajor");
        recommendation.setPrecio(new BigDecimal("10.00"));
        return recommendation;
    }

    private AIEventEntity onlyEventForStudent(AIEventType type, UUID studentId) {
        List<AIEventEntity> events = entityManager.createQuery("""
                select event
                from AIEventEntity event
                where event.type = :type
                and event.alumnoEntity.id = :studentId
                """, AIEventEntity.class)
                .setParameter("type", type)
                .setParameter("studentId", studentId)
                .getResultList();
        assertEquals(1, events.size());
        return events.get(0);
    }

    private AIEventEntity onlyEventForProduct(AIEventType type, UUID productId) {
        List<AIEventEntity> events = entityManager.createQuery("""
                select event
                from AIEventEntity event
                where event.type = :type
                and event.productoEntity.id = :productId
                """, AIEventEntity.class)
                .setParameter("type", type)
                .setParameter("productId", productId)
                .getResultList();
        assertEquals(1, events.size());
        return events.get(0);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void assertNumberEquals(BigDecimal expected, Object actual) {
        assertEquals(0, expected.compareTo(new BigDecimal(actual.toString())));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> categories(AIEventEntity event) {
        return (List<Map<String, Object>>) event.getResultData()
                .get("categoriasMasConsumidas");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> aiAnalysis(AIEventEntity event) {
        return (Map<String, Object>) event.getResultData().get("analisisIa");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return (Map<String, Object>) value;
    }
}
