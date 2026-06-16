package com.recred.backend.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.services.ParentSummaryAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GenerateParentSummaryTest {

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

    @InjectMocks
    private GenerateParentSummary generateParentSummary;

    @Test
    void testExecute() {
        UUID usuarioId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        tutor.setUsuario(usuario);
        
        when(tutorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(tutor));
        when(summaryRepository.save(any(Summary.class))).thenReturn(new Summary());
        
        generateParentSummary.execute(usuarioId, LocalDateTime.now().minusDays(7), LocalDateTime.now());
        
        verify(summaryRepository, times(1)).save(any(Summary.class));
    }

    @Test
    void executeBuildsSummaryWithChildrenPurchasesBudgetAndAnalyzerMessage() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 6, 15, 23, 59);
        Usuario parentUser = new Usuario();
        parentUser.setId(usuarioId);
        Tutor tutor = new Tutor();
        tutor.setUsuario(parentUser);
        Alumno ana = child("Ana");
        Alumno luis = child("Luis");
        tutor.setHijos(Set.of(ana, luis));
        Producto fruta = product("Fruta", "20.00", category("Frutas"));
        Producto snack = product("Snack", "10.00", null);
        Producto agua = product("Agua", "0.00", category("Bebidas"));

        when(tutorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(tutor));
        when(purchaseRepository.findByAlumnoAndDateRange(ana.getId(), inicio, fin))
                .thenReturn(List.of(
                        purchase(fruta, 2, "40.00"),
                        purchase(snack, 1, "10.00")
                ));
        when(purchaseRepository.findByAlumnoAndDateRange(luis.getId(), inicio, fin))
                .thenReturn(List.of(purchase(agua, 1, "0.00")));
        when(presupuestoRepository.findActivoByAlumnoId(ana.getId()))
                .thenReturn(Optional.of(budget(ana.getId())));
        when(presupuestoRepository.findActivoByAlumnoId(luis.getId()))
                .thenReturn(Optional.empty());
        when(parentSummaryAnalyzer.analyze(any())).thenReturn("Mensaje final");
        when(summaryRepository.save(any(Summary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Summary result = generateParentSummary.execute(usuarioId, inicio, fin);

        JsonNode json = OBJECT_MAPPER.readTree(result.getResumen());
        assertEquals(parentUser, result.getUsuario());
        assertEquals(inicio, result.getFechaDesde());
        assertEquals(fin, result.getFechaHasta());
        assertEquals("Mensaje final", json.get("mensaje").asText());
        JsonNode anaNode = json.get("hijos").get("Ana");
        assertEquals(50, anaNode.get("totalGastado").asInt());
        assertEquals(100, anaNode.get("LimiteGasto").asInt());
        assertEquals(80, anaNode.get("porCategoria").get("Frutas").asInt());
        assertEquals(20, anaNode.get("porCategoria").get("Sin Categoria").asInt());
        assertEquals("Fruta", anaNode.get("productoMasConsumido").get("nombre").asText());
        assertEquals(2, anaNode.get("productoMasConsumido").get("cantidad").asInt());
        JsonNode luisNode = json.get("hijos").get("Luis");
        assertEquals(0, luisNode.get("totalGastado").asInt());
        assertEquals(0, luisNode.get("porCategoria").get("Bebidas").asInt());
    }

    @Test
    void execute_WhenMostConsumedProductsHaveSameQuantity_ShouldChooseHighestPrice()
            throws Exception {
        UUID usuarioId = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 6, 15, 23, 59);
        Usuario parentUser = new Usuario();
        parentUser.setId(usuarioId);
        Tutor tutor = new Tutor();
        tutor.setUsuario(parentUser);
        Alumno child = child("Ana");
        tutor.setHijos(Set.of(child));
        Producto cheap = product("Agua", "10.00", category("Bebidas"));
        Producto expensive = product("Sandwich", "50.00", category("Comida"));

        when(tutorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(tutor));
        when(purchaseRepository.findByAlumnoAndDateRange(child.getId(), inicio, fin))
                .thenReturn(List.of(
                        purchase(cheap, 1, "10.00"),
                        purchase(expensive, 1, "50.00")
                ));
        when(presupuestoRepository.findActivoByAlumnoId(child.getId()))
                .thenReturn(Optional.empty());
        when(parentSummaryAnalyzer.analyze(any())).thenReturn("Mensaje final");
        when(summaryRepository.save(any(Summary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Summary result = generateParentSummary.execute(usuarioId, inicio, fin);

        JsonNode json = OBJECT_MAPPER.readTree(result.getResumen());
        JsonNode productNode = json.get("hijos").get("Ana").get("productoMasConsumido");
        assertEquals("Sandwich", productNode.get("nombre").asText());
        assertEquals(50, productNode.get("precio").asInt());
    }

    @Test
    void executeThrowsWhenTutorDoesNotExist() {
        UUID usuarioId = UUID.randomUUID();
        when(tutorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> generateParentSummary.execute(
                usuarioId,
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        ));
    }

    private Alumno child(String name) {
        Usuario usuario = new Usuario();
        usuario.setNombre(name);
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setUsuario(usuario);
        return alumno;
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
