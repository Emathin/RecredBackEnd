package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateBuffetMenu;
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
import com.recred.backend.domain.services.BudgetPeriodCalculator;
import com.recred.backend.infrastructure.web.MenuProductoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PresupuestoRepository presupuestoRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private BudgetPeriodCalculator budgetPeriodCalculator;

    private GenerateBuffetMenu useCase;
    private UUID alumnoId;
    private UUID buffetId;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        useCase = new GenerateBuffetMenu(
                alumnoRepository,
                productRepository,
                presupuestoRepository,
                purchaseRepository,
                budgetPeriodCalculator
        );
        alumnoId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        alumno = new Alumno();
        alumno.setId(alumnoId);
    }

    @Test
    void execute_ShouldUseBuffetAvailabilityInMenu() {
        Producto available = product("Jugo", new BigDecimal("10.00"), 5);
        Producto unavailable = product("Alfajor", new BigDecimal("15.00"), 0);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(available, unavailable));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = useCase.execute(
                alumnoId,
                buffetId,
                LocalDateTime.now()
        );

        assertFalse(result.get(0).bloqueado());
        assertEquals(5, result.get(0).stockDisponible());
        assertEquals("DISPONIBLE", result.get(0).estadoInventario());
        assertNotNull(result.get(0).categoria());
        assertTrue(result.get(1).bloqueado());
        assertEquals("Sin disponibilidad", result.get(1).motivoBloqueo());
        verify(productRepository).findAllActiveWithStock(buffetId);
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(UUID.randomUUID(), null, LocalDateTime.now()));
    }

    @Test
    void execute_WhenAlumnoNotFound_ShouldThrowException() {
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> useCase.execute(alumnoId, buffetId, LocalDateTime.now()));
    }

    @Test
    void execute_ShouldBlockProduct_WhenProductIsManuallyBlockedByTutor() {
        Producto product = product("Alfajor", BigDecimal.TEN, 10);
        alumno.setProductosBloqueados(Set.of(product));

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, LocalDateTime.now());

        assertEquals(1, result.size());
        assertTrue(result.get(0).bloqueado());
        assertEquals("Bloqueado por el tutor", result.get(0).motivoBloqueo());
    }

    @Test
    void execute_ShouldBlockProduct_WhenDailyCapacityIsExhausted() {
        Producto product = product("Sandwich", BigDecimal.TEN, 10);
        product.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        product.setCupoMaximoDiario(5);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                any(LocalDate.class),
                any()
        )).thenReturn(5L);

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, LocalDateTime.now());

        assertTrue(result.get(0).bloqueado());
        assertEquals("Cupo diario agotado", result.get(0).motivoBloqueo());
        assertEquals(0, result.get(0).cupoDisponibleDia());
    }

    @Test
    void execute_ShouldBlockProduct_WhenHealthRestrictionsAreViolated() {
        ClasificacionSalud sinTacc = classification("Sin TACC");
        ClasificacionSalud contieneLacteos = classification("Contiene Lacteos");
        alumno.setRestriccionesSalud(Set.of(sinTacc, contieneLacteos));

        Producto withGluten = product("Pan", BigDecimal.TEN, 10);
        withGluten.setClasificacionesSalud(Set.of());

        Producto withMilk = product("Queso sin TACC", BigDecimal.TEN, 10);
        withMilk.setClasificacionesSalud(Set.of(
                classification(sinTacc.getId(), "Sin TACC"),
                classification(contieneLacteos.getId(), "Contiene Lacteos")
        ));

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(withGluten, withMilk));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, LocalDateTime.now());

        MenuProductoDTO glutenDto = find(result, withGluten.getId());
        MenuProductoDTO milkDto = find(result, withMilk.getId());

        assertTrue(glutenDto.bloqueado());
        assertEquals("Contiene: Gluten (TACC)", glutenDto.motivoBloqueo());
        assertTrue(milkDto.bloqueado());
        assertEquals("Contiene: Lacteos", milkDto.motivoBloqueo());
    }

    @Test
    void execute_ShouldMapSpecificHealthConflictReasons() {
        ClasificacionSalud sinSodio = classification("Sin Sodio");
        ClasificacionSalud sinAzucar = classification("Sin Azucar");
        ClasificacionSalud vegano = classification("Apto Vegano");
        alumno.setRestriccionesSalud(new java.util.LinkedHashSet<>(
                List.of(sinSodio, sinAzucar, vegano)
        ));

        Producto product = product("Combo", BigDecimal.TEN, 10);
        product.setClasificacionesSalud(Set.of());

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, LocalDateTime.now());

        assertTrue(result.get(0).bloqueado());
        assertEquals(
                "Contiene: Alto sodio, Azucar, Ingredientes de origen animal",
                result.get(0).motivoBloqueo()
        );
    }

    @Test
    void execute_WhenHealthRestrictionDoesNotConflict_ShouldAllowProduct() {
        ClasificacionSalud sinTacc = classification("Sin TACC");
        alumno.setRestriccionesSalud(Set.of(sinTacc));
        Producto product = product("Alfajor sin TACC", BigDecimal.TEN, 10);
        product.setClasificacionesSalud(Set.of(classification(sinTacc.getId(), "Sin TACC")));

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, LocalDateTime.now());

        assertFalse(result.get(0).bloqueado());
    }

    @Test
    void execute_ShouldBlockProduct_WhenTimeRestrictionBlocksIt() {
        RestriccionHoraria restriction = mock(RestriccionHoraria.class);
        when(restriction.bloquea(any(Producto.class), any(LocalDateTime.class))).thenReturn(true);
        alumno.setRestriccionesHorarias(Set.of(restriction));

        Producto product = product("Gaseosa", BigDecimal.TEN, 10);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, LocalDateTime.now());

        assertTrue(result.get(0).bloqueado());
        assertEquals("No permitido en este horario", result.get(0).motivoBloqueo());
    }

    @Test
    void execute_ShouldBlockProduct_WhenGeneralBudgetOrCategoryBudgetExceeded() {
        CategoriaProducto category = category("Bebidas");
        Producto juice = product("Jugo", new BigDecimal("15.00"), 10, category);
        Producto soda = product("Gaseosa", new BigDecimal("30.00"), 10, category);
        LocalDateTime moment = LocalDateTime.now();
        RangoFechas range = new RangoFechas(moment.minusHours(1), moment.plusHours(1));
        PresupuestoAlumno budget = budget(category, new BigDecimal("50.00"),
                new BigDecimal("20.00"));

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(juice, soda));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(budget));
        when(budgetPeriodCalculator.calculateRange(any(), eq(moment))).thenReturn(range);
        when(purchaseRepository.sumarGastosPorAlumnoEnRango(alumnoId, range.inicio(), range.fin()))
                .thenReturn(new BigDecimal("30.00"));
        when(purchaseRepository.sumarGastosPorCategoriaEnRango(
                alumnoId,
                category.getId(),
                range.inicio(),
                range.fin()
        )).thenReturn(new BigDecimal("10.00"));

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, moment);

        MenuProductoDTO juiceDto = find(result, juice.getId());
        MenuProductoDTO sodaDto = find(result, soda.getId());

        assertTrue(juiceDto.bloqueado());
        assertEquals("Supera limite de su categoria", juiceDto.motivoBloqueo());
        assertTrue(sodaDto.bloqueado());
        assertEquals("Supera el limite de gasto", sodaDto.motivoBloqueo());
    }

    @Test
    void execute_ShouldReturnAllowedProduct_WhenNoRestrictionApplies() {
        Producto product = product("Galletitas", BigDecimal.TEN, 10);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, LocalDateTime.now());

        assertFalse(result.get(0).bloqueado());
        assertNull(result.get(0).motivoBloqueo());
        assertEquals(product.getId(), result.get(0).id());
    }

    @Test
    void execute_WhenOptionalStudentCollectionsAreNull_ShouldTreatThemAsEmpty() {
        Producto product = product("Galletitas", BigDecimal.TEN, 10);
        alumno.setProductosBloqueados(null);
        alumno.setRestriccionesHorarias(null);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, LocalDateTime.now());

        assertFalse(result.get(0).bloqueado());
    }

    @Test
    void execute_WhenActiveBudgetHasNullCategoryRules_ShouldUseGeneralBudgetOnly() {
        Producto product = product("Galletitas", new BigDecimal("10.00"), 10);
        LocalDateTime moment = LocalDateTime.now();
        RangoFechas range = new RangoFechas(moment.minusHours(1), moment.plusHours(1));
        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setPeriodo(PeriodoPresupuesto.DIARIO);
        budget.setMontoLimiteGeneral(new BigDecimal("100.00"));
        budget.setFechaInicio(LocalDate.now().minusDays(1));
        budget.setReglasCategoria(null);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(budget));
        when(budgetPeriodCalculator.calculateRange(any(), eq(moment))).thenReturn(range);
        when(purchaseRepository.sumarGastosPorAlumnoEnRango(alumnoId, range.inicio(), range.fin()))
                .thenReturn(BigDecimal.ZERO);

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, moment);

        assertFalse(result.get(0).bloqueado());
    }

    @Test
    void execute_WhenProductHasNoCategory_ShouldNotApplyCategoryBudget() {
        Producto product = product("Galletitas", new BigDecimal("10.00"), 10);
        product.setCategoria(null);
        LocalDateTime moment = LocalDateTime.now();
        RangoFechas range = new RangoFechas(moment.minusHours(1), moment.plusHours(1));
        PresupuestoAlumno budget = budget(category("Snacks"), new BigDecimal("100.00"),
                new BigDecimal("5.00"));

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(budget));
        when(budgetPeriodCalculator.calculateRange(any(), eq(moment))).thenReturn(range);
        when(purchaseRepository.sumarGastosPorAlumnoEnRango(alumnoId, range.inicio(), range.fin()))
                .thenReturn(BigDecimal.ZERO);
        when(purchaseRepository.sumarGastosPorCategoriaEnRango(
                eq(alumnoId),
                any(UUID.class),
                eq(range.inicio()),
                eq(range.fin())
        )).thenReturn(BigDecimal.ZERO);

        List<MenuProductoDTO> result = useCase.execute(alumnoId, buffetId, moment);

        assertFalse(result.get(0).bloqueado());
    }

    @Test
    void motivoConflictoSalud_ShouldReturnOriginalDescriptionWhenItHasNoKnownMapping() {
        String result = ReflectionTestUtils.invokeMethod(
                useCase,
                "motivoConflictoSalud",
                "Colorante"
        );

        assertEquals("Colorante", result);
    }

    @Test
    void normalizarDescripcion_WhenDescriptionIsNull_ShouldReturnEmptyString() {
        String result = ReflectionTestUtils.invokeMethod(
                useCase,
                "normalizarDescripcion",
                (String) null
        );

        assertEquals("", result);
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

    private CategoriaProducto category(String description) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion(description);
        return category;
    }

    private ClasificacionSalud classification(String description) {
        return classification(UUID.randomUUID(), description);
    }

    private ClasificacionSalud classification(UUID id, String description) {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(id);
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
        budget.setFechaInicio(LocalDate.now().minusDays(1));
        budget.setReglasCategoria(List.of(rule));
        return budget;
    }

    private MenuProductoDTO find(List<MenuProductoDTO> result, UUID productId) {
        return result.stream()
                .filter(item -> item.id().equals(productId))
                .findFirst()
                .orElseThrow();
    }
}
