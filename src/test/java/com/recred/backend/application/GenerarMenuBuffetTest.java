package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.infrastructure.web.MenuProductoDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerarMenuBuffetTest {

    private final AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final PresupuestoRepository presupuestoRepository = mock(PresupuestoRepository.class);
    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final GenerarMenuBuffet useCase = new GenerarMenuBuffet(
            alumnoRepository,
            productRepository,
            presupuestoRepository,
            purchaseRepository,
            new CalculadorPeriodoPresupuesto()
    );

    @Test
    void execute_ShouldUseBuffetAvailabilityInMenu() {
        UUID alumnoId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        Producto available = product(5);
        Producto unavailable = product(0);

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
        assertTrue(result.get(1).bloqueado());
        assertEquals("Sin disponibilidad", result.get(1).motivoBloqueo());
        verify(productRepository).findAllActiveWithStock(buffetId);
    }

    @Test
    void execute_WhenBuffetIdIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(UUID.randomUUID(), null, LocalDateTime.now()));
    }

    private Producto product(int stockDisponible) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Producto");
        product.setDescripcion("Descripcion");
        product.setPrecio(BigDecimal.TEN);
        product.setStockActual(stockDisponible);
        product.setStockReservado(0);
        product.setStockDisponible(stockDisponible);
        product.setEstadoInventario(
                stockDisponible > 0 ? EstadoInventario.DISPONIBLE : EstadoInventario.SIN_STOCK
        );
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        return product;
    }
}
