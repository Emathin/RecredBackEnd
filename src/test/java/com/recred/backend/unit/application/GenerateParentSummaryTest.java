package com.recred.backend.unit.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.application.GenerateParentSummaryRequest;
import com.recred.backend.application.GenerateParentSummaryResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.services.ParentSummaryAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateParentSummaryTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private SummaryRepository summaryRepository;
    @Mock
    private PresupuestoRepository presupuestoRepository;
    @Mock
    private ParentSummaryAnalyzer parentSummaryAnalyzer;

    private GenerateParentSummary useCase;

    private UUID tutorUserId;
    private LocalDateTime start;
    private LocalDateTime end;
    private Alumno ana;
    private Alumno luis;
    private Tutor tutor;
    private GenerateParentSummaryResponse response;

    @BeforeEach
    void setUp() {
        useCase = new GenerateParentSummary(
                tutorRepository,
                purchaseRepository,
                summaryRepository,
                presupuestoRepository,
                parentSummaryAnalyzer);
    }

    @Test
    void givenPremiumTutorWithChildren_whenGeneratingParentSummary_thenSavesSummaryForTutor() {
        givenParentSummaryPeriod();
        givenPremiumTutorWithChildren();
        givenNoPurchasesForChildren();
        givenNoActiveBudgets();

        whenGeneratingParentSummary();

        thenTutorUserWasProcessed();
        thenSummaryWasSavedForTutor();
    }

    @Test
    void givenChildrenPurchasesAndBudget_whenGeneratingParentSummary_thenSavesAnalyzedSummary()
            throws Exception {
        givenParentSummaryPeriod();
        givenPremiumTutorWithChildren();
        givenAnaPurchasesFruitAndSnack();
        givenLuisPurchaseWithZeroTotal();
        givenAnaHasActiveBudget();
        givenLuisHasNoActiveBudget();
        givenAnalyzerMessage("Mensaje final");

        whenGeneratingParentSummary();

        thenSavedSummaryHasTutorAndPeriod();
        thenSavedSummaryHasAnalyzerMessage("Mensaje final");
        thenSavedSummaryHasAnaSpendingBreakdown();
        thenSavedSummaryHasLuisZeroSpendingBreakdown();
    }

    @Test
    void givenProductsWithSameQuantity_whenGeneratingParentSummary_thenMostExpensiveProductWins()
            throws Exception {
        givenParentSummaryPeriod();
        givenPremiumTutorWithChild("Ana");
        givenAnaBoughtProductsWithSameQuantity();
        givenAnaHasNoActiveBudget();
        givenAnalyzerMessage("Mensaje final");

        whenGeneratingParentSummary();

        thenMostConsumedProductIs("Sandwich", 50, 1);
    }

    @Test
    void givenTutorsWithoutSummaryEligibility_whenGeneratingParentSummary_thenNoSummaryIsSaved() {
        givenParentSummaryPeriod();
        givenOnlyIneligibleTutors();

        whenGeneratingParentSummary();

        thenNoTutorUserWasProcessed();
        thenNoSummaryWasSaved();
    }

    private void givenParentSummaryPeriod() {
        start = LocalDateTime.of(2026, 6, 1, 0, 0);
        end = LocalDateTime.of(2026, 6, 15, 23, 59);
    }

    private void givenPremiumTutorWithChildren() {
        tutorUserId = UUID.randomUUID();
        ana = child("Ana");
        luis = child("Luis");
        tutor = tutor(tutorUserId, TipoPlan.PREMIUM, Set.of(ana, luis));
        when(tutorRepository.findAll()).thenReturn(List.of(tutor));
    }

    private void givenPremiumTutorWithChild(String childName) {
        tutorUserId = UUID.randomUUID();
        ana = child(childName);
        tutor = tutor(tutorUserId, TipoPlan.PREMIUM, Set.of(ana));
        when(tutorRepository.findAll()).thenReturn(List.of(tutor));
    }

    private void givenOnlyIneligibleTutors() {
        Tutor premiumWithoutChildren = tutor(UUID.randomUUID(), TipoPlan.PREMIUM, Set.of());
        Tutor freeTutorWithChildren = tutor(
                UUID.randomUUID(),
                TipoPlan.GRATUITO,
                Set.of(child("Ana")));
        when(tutorRepository.findAll())
                .thenReturn(List.of(premiumWithoutChildren, freeTutorWithChildren));
    }

    private void givenNoPurchasesForChildren() {
        when(purchaseRepository.findByAlumnoAndDateRange(ana.getId(), start, end))
                .thenReturn(List.of());
        when(purchaseRepository.findByAlumnoAndDateRange(luis.getId(), start, end))
                .thenReturn(List.of());
    }

    private void givenNoActiveBudgets() {
        when(presupuestoRepository.findActivoByAlumnoId(ana.getId()))
                .thenReturn(Optional.empty());
        when(presupuestoRepository.findActivoByAlumnoId(luis.getId()))
                .thenReturn(Optional.empty());
    }

    private void givenAnaPurchasesFruitAndSnack() {
        Producto fruit = product("Fruta", "20.00", category("Frutas"));
        Producto snack = product("Snack", "10.00", null);
        when(purchaseRepository.findByAlumnoAndDateRange(ana.getId(), start, end))
                .thenReturn(List.of(
                        purchase(fruit, 2, "40.00"),
                        purchase(snack, 1, "10.00")
                ));
    }

    private void givenLuisPurchaseWithZeroTotal() {
        Producto water = product("Agua", "0.00", category("Bebidas"));
        when(purchaseRepository.findByAlumnoAndDateRange(luis.getId(), start, end))
                .thenReturn(List.of(purchase(water, 1, "0.00")));
    }

    private void givenAnaHasActiveBudget() {
        when(presupuestoRepository.findActivoByAlumnoId(ana.getId()))
                .thenReturn(Optional.of(budget(ana.getId())));
    }

    private void givenLuisHasNoActiveBudget() {
        when(presupuestoRepository.findActivoByAlumnoId(luis.getId()))
                .thenReturn(Optional.empty());
    }

    private void givenAnaBoughtProductsWithSameQuantity() {
        Producto cheap = product("Agua", "10.00", category("Bebidas"));
        Producto expensive = product("Sandwich", "50.00", category("Comida"));
        when(purchaseRepository.findByAlumnoAndDateRange(ana.getId(), start, end))
                .thenReturn(List.of(
                        purchase(cheap, 1, "10.00"),
                        purchase(expensive, 1, "50.00")
                ));
    }

    private void givenAnaHasNoActiveBudget() {
        when(presupuestoRepository.findActivoByAlumnoId(ana.getId()))
                .thenReturn(Optional.empty());
    }

    private void givenAnalyzerMessage(String message) {
        when(parentSummaryAnalyzer.analyze(any())).thenReturn(message);
    }

    private void whenGeneratingParentSummary() {
        response = useCase.execute(new GenerateParentSummaryRequest(start, end));
    }

    private void thenTutorUserWasProcessed() {
        assertEquals(List.of(tutorUserId), response.processedUserIds());
    }

    private void thenNoTutorUserWasProcessed() {
        assertTrue(response.processedUserIds().isEmpty());
    }

    private void thenSummaryWasSavedForTutor() {
        assertEquals(tutor.getUsuario(), savedSummary().getUsuario());
    }

    private void thenSavedSummaryHasTutorAndPeriod() {
        Summary summary = savedSummary();
        assertEquals(tutor.getUsuario(), summary.getUsuario());
        assertEquals(start, summary.getFechaDesde());
        assertEquals(end, summary.getFechaHasta());
    }

    private void thenSavedSummaryHasAnalyzerMessage(String expectedMessage) throws Exception {
        assertEquals(expectedMessage, summaryJson().get("mensaje").asText());
    }

    private void thenSavedSummaryHasAnaSpendingBreakdown() throws Exception {
        JsonNode anaNode = summaryJson().get("hijos").get("Ana");
        assertEquals(50, anaNode.get("totalGastado").asInt());
        assertEquals(100, anaNode.get("LimiteGasto").asInt());
        assertEquals(80, anaNode.get("porCategoria").get("Frutas").asInt());
        assertEquals(20, anaNode.get("porCategoria").get("Sin Categoria").asInt());
        assertEquals("Fruta", anaNode.get("productoMasConsumido").get("nombre").asText());
        assertEquals(2, anaNode.get("productoMasConsumido").get("cantidad").asInt());
    }

    private void thenSavedSummaryHasLuisZeroSpendingBreakdown() throws Exception {
        JsonNode luisNode = summaryJson().get("hijos").get("Luis");
        assertEquals(0, luisNode.get("totalGastado").asInt());
        assertEquals(0, luisNode.get("porCategoria").get("Bebidas").asInt());
    }

    private void thenMostConsumedProductIs(String name, int price, int quantity)
            throws Exception {
        JsonNode productNode = summaryJson()
                .get("hijos")
                .get("Ana")
                .get("productoMasConsumido");
        assertEquals(name, productNode.get("nombre").asText());
        assertEquals(price, productNode.get("precio").asInt());
        assertEquals(quantity, productNode.get("cantidad").asInt());
    }

    private void thenNoSummaryWasSaved() {
        verify(summaryRepository, never()).save(any(Summary.class));
    }

    private Summary savedSummary() {
        ArgumentCaptor<Summary> captor = ArgumentCaptor.forClass(Summary.class);
        verify(summaryRepository).save(captor.capture());
        return captor.getValue();
    }

    private JsonNode summaryJson() throws Exception {
        return OBJECT_MAPPER.readTree(savedSummary().getResumen());
    }

    private Tutor tutor(UUID userId, TipoPlan plan, Set<Alumno> children) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setPlan(plan);
        Tutor tutor = new Tutor();
        tutor.setUsuario(user);
        tutor.setHijos(children);
        return tutor;
    }

    private Alumno child(String name) {
        Usuario user = new Usuario();
        user.setNombre(name);
        Alumno child = new Alumno();
        child.setId(UUID.randomUUID());
        child.setUsuario(user);
        return child;
    }

    private CategoriaProducto category(String name) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion(name);
        return category;
    }

    private Producto product(String name, String price, CategoriaProducto category) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(new BigDecimal(price));
        product.setCategoria(category);
        return product;
    }

    private Purchase purchase(Producto product, int quantity, String total) {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setTotalAmount(new BigDecimal(total));
        purchase.setItems(java.util.Map.of(product, quantity));
        return purchase;
    }

    private PresupuestoAlumno budget(UUID alumnoId) {
        return new PresupuestoAlumno(
                UUID.randomUUID(),
                alumnoId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 6, 1),
                true,
                List.of()
        );
    }
}
