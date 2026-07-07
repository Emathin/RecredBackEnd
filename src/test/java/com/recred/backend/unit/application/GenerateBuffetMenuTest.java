package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateBuffetMenu;
import com.recred.backend.application.GenerateBuffetMenuRequest;
import com.recred.backend.application.MenuProductResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateBuffetMenuTest {

    private static final LocalDateTime QUERY_MOMENT =
            LocalDateTime.of(2026, 6, 15, 10, 0);

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PresupuestoRepository presupuestoRepository;
    @Mock
    private PurchaseRepository purchaseRepository;

    private GenerateBuffetMenu useCase;

    private UUID studentId;
    private UUID buffetId;
    private Alumno student;
    private List<MenuProductResponse> menu;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GenerateBuffetMenu(
                alumnoRepository,
                productRepository,
                presupuestoRepository,
                purchaseRepository,
                new CalculadorPeriodoPresupuesto()
        );
    }

    @Test
    void givenAvailableAndUnavailableProducts_whenGeneratingMenu_thenMapsAvailability() {
        givenMenuScenario();
        Producto available = product("Jugo", new BigDecimal("10.00"), 5);
        Producto unavailable = product("Alfajor", new BigDecimal("15.00"), 0);
        givenStudentExists();
        givenBuffetProducts(available, unavailable);
        givenStudentHasNoBudget();

        whenGeneratingMenu();

        thenProductIsAllowed(available);
        thenProductStockIs(available, 5, "DISPONIBLE");
        thenProductIsBlockedWith(unavailable, "Sin disponibilidad");
        thenProductsWereLoadedFromBuffet();
    }

    @Test
    void givenMissingStudent_whenGeneratingMenu_thenFails() {
        givenMenuScenario();
        givenStudentDoesNotExist();

        whenGeneratingMenuFails();

        thenMenuGenerationFailsWith("Alumno no encontrado");
    }

    @Test
    void givenTutorBlockedProduct_whenGeneratingMenu_thenBlocksProduct() {
        givenMenuScenario();
        Producto product = product("Alfajor", BigDecimal.TEN, 10);
        givenStudentBlockedProduct(product);
        givenStudentExists();
        givenBuffetProducts(product);
        givenStudentHasNoBudget();

        whenGeneratingMenu();

        thenProductIsBlockedWith(product, "Bloqueado por el tutor");
    }

    @Test
    void givenExhaustedDailyCapacity_whenGeneratingMenu_thenBlocksProduct() {
        givenMenuScenario();
        Producto product = dailyCapacityProduct("Sandwich", 5);
        givenStudentExists();
        givenBuffetProducts(product);
        givenStudentHasNoBudget();
        givenDailyCapacityReservations(product, 5L);

        whenGeneratingMenu();

        thenProductIsBlockedWith(product, "Cupo diario agotado");
        thenDailyCapacityAvailableIs(product, 0);
    }

    @Test
    void givenHealthConflict_whenGeneratingMenu_thenBlocksProductWithHealthReason() {
        givenMenuScenario();
        ClasificacionSalud sinTacc = classification("Sin TACC");
        Producto product = product("Pan", BigDecimal.TEN, 10);
        product.setClasificacionesSalud(Set.of());
        givenStudentHasHealthRestrictions(sinTacc);
        givenStudentExists();
        givenBuffetProducts(product);
        givenStudentHasNoBudget();

        whenGeneratingMenu();

        thenProductIsBlockedWith(product, "Contiene: Gluten (TACC)");
    }

    @Test
    void givenTimeRestriction_whenGeneratingMenu_thenBlocksProduct() {
        givenMenuScenario();
        Producto product = product("Gaseosa", BigDecimal.TEN, 10);
        givenStudentHasBlockingTimeRestriction();
        givenStudentExists();
        givenBuffetProducts(product);
        givenStudentHasNoBudget();

        whenGeneratingMenu();

        thenProductIsBlockedWith(product, "No permitido en este horario");
    }

    @Test
    void givenBudgetLimits_whenGeneratingMenu_thenBlocksProductsThatExceedBudget() {
        givenMenuScenario();
        CategoriaProducto category = category("Bebidas");
        Producto juice = product("Jugo", new BigDecimal("15.00"), 10, category);
        Producto soda = product("Gaseosa", new BigDecimal("30.00"), 10, category);
        PresupuestoAlumno budget = budget(category, new BigDecimal("50.00"),
                new BigDecimal("20.00"));
        RangoFechas range = budget.getPeriodo().calculateRange(QUERY_MOMENT);
        givenStudentExists();
        givenBuffetProducts(juice, soda);
        givenActiveBudget(budget);
        givenGeneralSpending(range, new BigDecimal("30.00"));
        givenCategorySpending(category, range, new BigDecimal("10.00"));

        whenGeneratingMenu();

        thenProductIsBlockedWith(juice, "Supera limite de su categoria");
        thenProductIsBlockedWith(soda, "Supera el limite de gasto");
    }

    @Test
    void givenProductWithoutRestrictions_whenGeneratingMenu_thenAllowsProduct() {
        givenMenuScenario();
        Producto product = product("Galletitas", BigDecimal.TEN, 10);
        givenStudentExists();
        givenBuffetProducts(product);
        givenStudentHasNoBudget();

        whenGeneratingMenu();

        thenProductIsAllowed(product);
        thenProductHasNoBlockingReason(product);
    }

    @Test
    void givenStudentOptionalCollectionsAreNull_whenGeneratingMenu_thenTreatsThemAsEmpty() {
        givenMenuScenario();
        Producto product = product("Galletitas", BigDecimal.TEN, 10);
        student.setProductosBloqueados(null);
        student.setRestriccionesHorarias(null);
        givenStudentExists();
        givenBuffetProducts(product);
        givenStudentHasNoBudget();

        whenGeneratingMenu();

        thenProductIsAllowed(product);
    }

    @Test
    void givenBudgetWithoutCategoryRules_whenGeneratingMenu_thenUsesGeneralBudgetOnly() {
        givenMenuScenario();
        Producto product = product("Galletitas", new BigDecimal("10.00"), 10);
        PresupuestoAlumno budget = generalBudgetWithoutCategoryRules();
        RangoFechas range = budget.getPeriodo().calculateRange(QUERY_MOMENT);
        givenStudentExists();
        givenBuffetProducts(product);
        givenActiveBudget(budget);
        givenGeneralSpending(range, BigDecimal.ZERO);

        whenGeneratingMenu();

        thenProductIsAllowed(product);
    }

    @Test
    void givenProductWithoutCategory_whenGeneratingMenu_thenDoesNotApplyCategoryBudget() {
        givenMenuScenario();
        Producto product = product("Galletitas", new BigDecimal("10.00"), 10);
        product.setCategoria(null);
        CategoriaProducto category = category("Snacks");
        PresupuestoAlumno budget = budget(category, new BigDecimal("100.00"),
                new BigDecimal("5.00"));
        RangoFechas range = budget.getPeriodo().calculateRange(QUERY_MOMENT);
        givenStudentExists();
        givenBuffetProducts(product);
        givenActiveBudget(budget);
        givenGeneralSpending(range, BigDecimal.ZERO);
        givenCategorySpending(category, range, BigDecimal.ZERO);

        whenGeneratingMenu();

        thenProductIsAllowed(product);
    }

    private void givenMenuScenario() {
        studentId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);
    }

    private void givenStudentExists() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void givenBuffetProducts(Producto... products) {
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(products));
    }

    private void givenStudentHasNoBudget() {
        when(presupuestoRepository.findActivoByAlumnoId(studentId)).thenReturn(Optional.empty());
    }

    private void givenStudentBlockedProduct(Producto product) {
        student.setProductosBloqueados(Set.of(product));
    }

    private void givenDailyCapacityReservations(Producto product, long reservedQuantity) {
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(QUERY_MOMENT.toLocalDate()),
                any()
        )).thenReturn(reservedQuantity);
    }

    private void givenStudentHasHealthRestrictions(ClasificacionSalud... restrictions) {
        student.setRestriccionesSalud(Set.of(restrictions));
    }

    private void givenStudentHasBlockingTimeRestriction() {
        RestriccionHoraria restriction = mock(RestriccionHoraria.class);
        when(restriction.bloquea(any(Producto.class), any(LocalDateTime.class)))
                .thenReturn(true);
        student.setRestriccionesHorarias(Set.of(restriction));
    }

    private void givenActiveBudget(PresupuestoAlumno budget) {
        when(presupuestoRepository.findActivoByAlumnoId(studentId))
                .thenReturn(Optional.of(budget));
    }

    private void givenGeneralSpending(RangoFechas range, BigDecimal amount) {
        when(purchaseRepository.sumarGastosPorAlumnoEnRango(
                studentId,
                range.inicio(),
                range.fin()
        )).thenReturn(amount);
    }

    private void givenCategorySpending(
            CategoriaProducto category,
            RangoFechas range,
            BigDecimal amount) {
        when(purchaseRepository.sumarGastosPorCategoriaEnRango(
                studentId,
                category.getId(),
                range.inicio(),
                range.fin()
        )).thenReturn(amount);
    }

    private void whenGeneratingMenu() {
        menu = useCase.execute(new GenerateBuffetMenuRequest(
                studentId,
                buffetId,
                QUERY_MOMENT
        )).products();
    }

    private void whenGeneratingMenuFails() {
        thrownException = assertThrows(RuntimeException.class, () -> useCase.execute(
                new GenerateBuffetMenuRequest(studentId, buffetId, QUERY_MOMENT)));
    }

    private void thenProductIsAllowed(Producto product) {
        assertFalse(itemFor(product).bloqueado());
    }

    private void thenProductIsBlockedWith(Producto product, String expectedReason) {
        MenuProductResponse item = itemFor(product);
        assertTrue(item.bloqueado());
        assertEquals(expectedReason, item.motivoBloqueo());
    }

    private void thenProductStockIs(
            Producto product,
            int expectedStock,
            String expectedInventoryStatus) {
        MenuProductResponse item = itemFor(product);
        assertEquals(expectedStock, item.stockDisponible());
        assertEquals(expectedInventoryStatus, item.estadoInventario());
        assertNotNull(item.categoria());
    }

    private void thenProductsWereLoadedFromBuffet() {
        verify(productRepository).findAllActiveWithStock(buffetId);
    }

    private void thenMenuGenerationFailsWith(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenDailyCapacityAvailableIs(Producto product, int expectedCapacity) {
        assertEquals(expectedCapacity, itemFor(product).cupoDisponibleDia());
    }

    private void thenProductHasNoBlockingReason(Producto product) {
        assertNull(itemFor(product).motivoBloqueo());
        assertEquals(product.getId(), itemFor(product).id());
    }

    private MenuProductResponse itemFor(Producto product) {
        return menu.stream()
                .filter(item -> item.id().equals(product.getId()))
                .findFirst()
                .orElseThrow();
    }

    private Producto product(String name, BigDecimal price, int stockDisponible) {
        return product(name, price, stockDisponible, category("Snacks"));
    }

    private Producto product(
            String name,
            BigDecimal price,
            int stockDisponible,
            CategoriaProducto category
    ) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setDescripcion("Descripcion");
        product.setPrecio(price);
        product.setCategoria(category);
        product.setStockActual(stockDisponible);
        product.setStockReservado(0);
        product.setStockDisponible(stockDisponible);
        product.setEstadoInventario(
                stockDisponible > 0 ? EstadoInventario.DISPONIBLE : EstadoInventario.SIN_STOCK
        );
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        product.setClasificacionesSalud(Set.of());
        return product;
    }

    private Producto dailyCapacityProduct(String name, int dailyCapacity) {
        Producto product = product(name, BigDecimal.TEN, 10);
        product.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        product.setCupoMaximoDiario(dailyCapacity);
        return product;
    }

    private CategoriaProducto category(String description) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion(description);
        return category;
    }

    private ClasificacionSalud classification(String description) {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(UUID.randomUUID());
        classification.setDescripcion(description);
        return classification;
    }

    private PresupuestoAlumno budget(
            CategoriaProducto category,
            BigDecimal generalLimit,
            BigDecimal categoryLimit
    ) {
        ReglaPresupuestoCategoria rule = new ReglaPresupuestoCategoria();
        rule.setCategoria(category);
        rule.setMontoLimiteCalculado(categoryLimit);

        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setPeriodo(PeriodoPresupuesto.DIARIO);
        budget.setMontoLimiteGeneral(generalLimit);
        budget.setFechaInicio(QUERY_MOMENT.toLocalDate().minusDays(1));
        budget.setReglasCategoria(List.of(rule));
        return budget;
    }

    private PresupuestoAlumno generalBudgetWithoutCategoryRules() {
        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setPeriodo(PeriodoPresupuesto.DIARIO);
        budget.setMontoLimiteGeneral(new BigDecimal("100.00"));
        budget.setFechaInicio(QUERY_MOMENT.toLocalDate().minusDays(1));
        budget.setReglasCategoria(null);
        return budget;
    }
}
