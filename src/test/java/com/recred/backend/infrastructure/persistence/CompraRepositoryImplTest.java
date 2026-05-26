package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.CompraEntityPresencialEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraRepositoryImplTest {

    @Mock
    private SpringDataCompraRepository springDataRepository;

    private CompraRepositoryImpl compraRepository;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryImpl(springDataRepository);
    }

    @Test
    void recuperaUltimasCompras_Correctamente_MapeandoADominio() {
        UUID studentId = UUID.randomUUID();
        
        ProductoEntity productoEntity = new ProductoEntity();
        productoEntity.setNombre("Alfajor");
        CategoriaProductoEntity categoria = new CategoriaProductoEntity();
        categoria.setDescripcion("Snacks");
        productoEntity.setCategoria(categoria);

        CompraEntity compra = new CompraEntityPresencialEntity();
        compra.setFechaHora(LocalDateTime.now());
        
        CompraDetalleEntity detalle = new CompraDetalleEntity();
        detalle.setProductoEntity(productoEntity);
        detalle.setCompra(compra);
        compra.setDetalles(List.of(detalle));

        when(springDataRepository.findLastByAlumno(eq(studentId), any(PageRequest.class)))
                .thenReturn(List.of(compra));

        List<PreferenceAIRepository.CompraResumen> result = 
                compraRepository.findLastPurchasesByAlumno(studentId, 5);

        assertEquals(1, result.size());
        assertEquals("Alfajor", result.get(0).productoNombre());
        assertEquals("Snacks", result.get(0).categoria());
    }

    @Test
    void manejaCategoriaNula_Correctamente() {
        UUID studentId = UUID.randomUUID();
        
        ProductoEntity productoEntity = new ProductoEntity();
        productoEntity.setNombre("Agua");
        productoEntity.setCategoria(null);

        CompraEntity compra = new CompraEntityPresencialEntity();
        compra.setFechaHora(LocalDateTime.now());
        
        CompraDetalleEntity detalle = new CompraDetalleEntity();
        detalle.setProductoEntity(productoEntity);
        detalle.setCompra(compra);
        compra.setDetalles(List.of(detalle));

        when(springDataRepository.findLastByAlumno(eq(studentId), any(PageRequest.class)))
                .thenReturn(List.of(compra));

        List<PreferenceAIRepository.CompraResumen> result = 
                compraRepository.findLastPurchasesByAlumno(studentId, 5);

        assertEquals("Sin Categoria", result.get(0).categoria());
    }
}
