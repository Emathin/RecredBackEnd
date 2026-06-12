package com.recred.backend.application;

import com.recred.backend.domain.models.*;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.infrastructure.web.MenuProductoDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GenerarMenuBuffetTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PresupuestoRepository presupuestoRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private CalculadorPeriodoPresupuesto calculadorPeriodo;

    @InjectMocks
    private GenerarMenuBuffet generarMenuBuffet;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void execute_shouldThrowException_whenAlumnoNotFound() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                generarMenuBuffet.execute(alumnoId, LocalDateTime.now()));
    }

    @Test
    void execute_shouldReturnAllowedProducts_whenNoRestrictionsOrBudget() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        Producto prod = new Producto();
        prod.setId(UUID.randomUUID());
        prod.setNombre("Galletitas");
        prod.setPrecio(BigDecimal.TEN);
        
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        cat.setDescripcion("Snacks");
        prod.setCategoria(cat);

        when(productRepository.findAllActive()).thenReturn(List.of(prod));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = generarMenuBuffet.execute(alumnoId, LocalDateTime.now());

        assertEquals(1, result.size());
        MenuProductoDTO dto = result.get(0);
        assertFalse(dto.bloqueado());
        assertEquals(prod.getId(), dto.id());
        assertNull(dto.motivoBloqueo());
    }

    @Test
    void execute_shouldReturnBlockedProduct_whenProductIsManuallyBlockedByTutor() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        
        Producto prod = new Producto();
        prod.setId(UUID.randomUUID());
        prod.setNombre("Alfajor");
        prod.setPrecio(BigDecimal.TEN);
        
        alumno.setProductosBloqueados(Set.of(prod));
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(productRepository.findAllActive()).thenReturn(List.of(prod));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = generarMenuBuffet.execute(alumnoId, LocalDateTime.now());

        assertEquals(1, result.size());
        MenuProductoDTO dto = result.get(0);
        assertTrue(dto.bloqueado());
        assertEquals("Bloqueado por el tutor", dto.motivoBloqueo());
    }

    @Test
    void execute_shouldBlockProduct_whenHealthRestrictionsViolated() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);

        // Restriction 1: Sin TACC (starts with "Sin")
        ClasificacionSalud sinTacc = new ClasificacionSalud();
        sinTacc.setId(UUID.randomUUID());
        sinTacc.setDescripcion("Sin TACC");

        // Restriction 2: Contiene Lácteos (does not start with "Sin")
        ClasificacionSalud contieneLacteos = new ClasificacionSalud();
        contieneLacteos.setId(UUID.randomUUID());
        contieneLacteos.setDescripcion("Contiene Lácteos");

        alumno.setRestriccionesSalud(Set.of(sinTacc, contieneLacteos));
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        // Product 1: Has gluten (doesn't have Sin TACC classification), has no lacteos.
        Producto prodGluten = new Producto();
        prodGluten.setId(UUID.randomUUID());
        prodGluten.setNombre("Pan");
        prodGluten.setPrecio(BigDecimal.TEN);
        prodGluten.setClasificacionesSalud(Set.of());

        // Product 2: Apto Gluten (has Sin TACC) but has Lacteos (has classification).
        Producto prodLacteos = new Producto();
        prodLacteos.setId(UUID.randomUUID());
        prodLacteos.setNombre("Queso sin TACC");
        prodLacteos.setPrecio(BigDecimal.TEN);
        
        ClasificacionSalud cTacc = new ClasificacionSalud();
        cTacc.setId(sinTacc.getId());
        cTacc.setDescripcion("Sin TACC");
        
        ClasificacionSalud cLacteos = new ClasificacionSalud();
        cLacteos.setId(contieneLacteos.getId());
        cLacteos.setDescripcion("Contiene Lácteos");
        
        prodLacteos.setClasificacionesSalud(Set.of(cTacc, cLacteos));

        when(productRepository.findAllActive()).thenReturn(List.of(prodGluten, prodLacteos));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = generarMenuBuffet.execute(alumnoId, LocalDateTime.now());

        assertEquals(2, result.size());
        
        MenuProductoDTO dtoGluten = result.stream()
                .filter(r -> r.id().equals(prodGluten.getId()))
                .findFirst().orElseThrow();
        assertTrue(dtoGluten.bloqueado());
        assertEquals("Contiene: Gluten (TACC)", dtoGluten.motivoBloqueo());

        MenuProductoDTO dtoLacteos = result.stream()
                .filter(r -> r.id().equals(prodLacteos.getId()))
                .findFirst().orElseThrow();
        assertTrue(dtoLacteos.bloqueado());
        assertEquals("Contiene: Lácteos", dtoLacteos.motivoBloqueo());
    }

    @Test
    void execute_shouldBlockProduct_whenTimeRestrictionBlocksIt() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);

        RestriccionHoraria rhMock = mock(RestriccionHoraria.class);
        when(rhMock.bloquea(any(Producto.class), any(LocalDateTime.class))).thenReturn(true);
        alumno.setRestriccionesHorarias(Set.of(rhMock));

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        Producto prod = new Producto();
        prod.setId(UUID.randomUUID());
        prod.setNombre("Coca Cola");
        prod.setPrecio(BigDecimal.TEN);
        prod.setClasificacionesSalud(Set.of());

        when(productRepository.findAllActive()).thenReturn(List.of(prod));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        List<MenuProductoDTO> result = generarMenuBuffet.execute(alumnoId, LocalDateTime.now());

        assertEquals(1, result.size());
        MenuProductoDTO dto = result.get(0);
        assertTrue(dto.bloqueado());
        assertEquals("No permitido en este horario", dto.motivoBloqueo());
    }

    @Test
    void execute_shouldBlockProduct_whenGeneralBudgetOrCategoryBudgetExceeded() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        // Category
        CategoriaProducto cat = new CategoriaProducto();
        cat.setId(UUID.randomUUID());
        cat.setDescripcion("Bebidas");

        // Product 1: Price 15, Category "Bebidas"
        Producto prod1 = new Producto();
        prod1.setId(UUID.randomUUID());
        prod1.setNombre("Jugo");
        prod1.setPrecio(new BigDecimal("15.0"));
        prod1.setCategoria(cat);
        prod1.setClasificacionesSalud(Set.of());

        // Product 2: Price 30, Category "Bebidas" (supera presupuesto general de 20)
        Producto prod2 = new Producto();
        prod2.setId(UUID.randomUUID());
        prod2.setNombre("Gaseosa");
        prod2.setPrecio(new BigDecimal("30.0"));
        prod2.setCategoria(cat);
        prod2.setClasificacionesSalud(Set.of());

        when(productRepository.findAllActive()).thenReturn(List.of(prod1, prod2));

        // Active Budget
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();
        presupuesto.setPeriodo(PeriodoPresupuesto.DIARIO);
        presupuesto.setMontoLimiteGeneral(new BigDecimal("50.0")); // general limit is 50
        
        // Category rule
        ReglaPresupuestoCategoria regla = new ReglaPresupuestoCategoria();
        regla.setCategoria(cat);
        regla.setMontoLimiteCalculado(new BigDecimal("20.0")); // category limit is 20
        presupuesto.setReglasCategoria(List.of(regla));

        // Setup validity date
        LocalDate today = LocalDate.now();
        presupuesto.setFechaInicio(today.minusDays(1));

        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(presupuesto));

        // Mock CalculadorPeriodoPresupuesto and historical spending
        LocalDateTime momento = LocalDateTime.now();
        RangoFechas rango = new RangoFechas(momento.minusHours(1), momento.plusHours(1));
        when(calculadorPeriodo.calcularRango(any(), any())).thenReturn(rango);

        // Sumar gastos
        when(purchaseRepository.sumarGastosPorAlumnoEnRango(
                eq(alumnoId), any(), any())).thenReturn(new BigDecimal("30.0"));
        when(purchaseRepository.sumarGastosPorCategoriaEnRango(
                eq(alumnoId), eq(cat.getId()), any(), any())).thenReturn(new BigDecimal("10.0"));

        List<MenuProductoDTO> result = generarMenuBuffet.execute(alumnoId, momento);

        assertEquals(2, result.size());

        // prod1 (Jugo): price 15. Supera limit de categoria (10 available), general (20 available).
        MenuProductoDTO dto1 = result.stream()
                .filter(r -> r.id().equals(prod1.getId()))
                .findFirst().orElseThrow();
        assertTrue(dto1.bloqueado());
        assertEquals("Supera límite de su categoría", dto1.motivoBloqueo());

        // prod2 (Gaseosa): price 30. Supera general limit (20 available).
        MenuProductoDTO dto2 = result.stream()
                .filter(r -> r.id().equals(prod2.getId()))
                .findFirst().orElseThrow();
        assertTrue(dto2.bloqueado());
        assertEquals("Supera el límite de gasto", dto2.motivoBloqueo());
    }
}
