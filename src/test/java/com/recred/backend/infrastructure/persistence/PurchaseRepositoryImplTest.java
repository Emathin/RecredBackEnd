package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.CompraAnticipadaEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.CompraPresencialEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRepositoryImplTest {

    @Mock
    private SpringDataPurchaseRepository springDataRepository;

    private PurchaseRepositoryImpl repository;
    private UUID purchaseId;
    private UUID studentId;
    private UUID productId;
    private UUID buffetId;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        repository = new PurchaseRepositoryImpl(springDataRepository);
        purchaseId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        productId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        purchase = purchase(null);
    }

    @Test
    void save_WhenPurchaseDoesNotHaveId_ShouldCreatePresentialPurchase() {
        UUID savedPurchaseId = UUID.randomUUID();
        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    entity.setId(savedPurchaseId);
                    return entity;
                });

        Purchase savedPurchase = repository.save(purchase);

        ArgumentCaptor<CompraEntity> entityCaptor = ArgumentCaptor.forClass(CompraEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        CompraEntity savedEntity = entityCaptor.getValue();
        assertTrue(savedEntity instanceof CompraPresencialEntity);
        assertEquals(studentId, savedEntity.getAlumnoEntity().getId());
        assertEquals(buffetId, savedEntity.getBuffetEntity().getId());
        assertEquals(EstadoCompra.PENDIENTE, savedEntity.getEstado());
        assertEquals(productId, savedEntity.getDetalles().get(0).getProductoEntity().getId());
        assertEquals(savedPurchaseId, savedPurchase.getId());
    }

    @Test
    void save_WhenPurchaseHasId_ShouldUpdateExistingPurchase() {
        CompraPresencialEntity existingEntity = new CompraPresencialEntity();
        existingEntity.setId(purchaseId);
        existingEntity.addDetalle(new CompraDetalleEntity());
        purchase.setId(purchaseId);

        when(springDataRepository.findById(purchaseId)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(existingEntity)).thenReturn(existingEntity);

        Purchase savedPurchase = repository.save(purchase);

        assertEquals(purchaseId, savedPurchase.getId());
        assertEquals(1, existingEntity.getDetalles().size());
        assertEquals(productId, existingEntity.getDetalles().get(0).getProductoEntity().getId());
        verify(springDataRepository).findById(purchaseId);
        verify(springDataRepository).save(existingEntity);
    }

    @Test
    void save_WhenPurchaseIsAdvance_ShouldCreateAdvancePurchaseWithScheduledPickup() {
        UUID savedPurchaseId = UUID.randomUUID();
        UUID pickupSlotId = UUID.randomUUID();
        LocalDate pickupDate = LocalDate.of(2026, 6, 8);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setWithdrawalCode("ABC123");
        purchase.setPickupSlotId(pickupSlotId);
        purchase.setPickupDate(pickupDate);

        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    entity.setId(savedPurchaseId);
                    return entity;
                });

        repository.save(purchase);

        ArgumentCaptor<CompraEntity> entityCaptor = ArgumentCaptor.forClass(CompraEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        CompraEntity savedEntity = entityCaptor.getValue();

        assertTrue(savedEntity instanceof CompraAnticipadaEntity);
        CompraAnticipadaEntity advanceEntity = (CompraAnticipadaEntity) savedEntity;
        assertEquals("ABC123", advanceEntity.getCodigoVerificacion());
        assertEquals(pickupDate, advanceEntity.getRetiroProgramadoEntity()
                .getFechaProgramada());
        assertEquals(pickupSlotId, advanceEntity.getRetiroProgramadoEntity()
                .getFranja()
                .getId());
    }

    @Test
    void findById_WhenPurchaseExists_ShouldReturnMappedPurchase() {
        CompraPresencialEntity entity = compraEntity();
        when(springDataRepository.findById(purchaseId)).thenReturn(Optional.of(entity));

        Optional<Purchase> result = repository.findById(purchaseId);

        assertTrue(result.isPresent());
        assertEquals(purchaseId, result.get().getId());
        assertEquals(studentId, result.get().getStudent().getId());
        assertEquals(buffetId, result.get().getBuffetId());
    }

    @Test
    void findById_WhenPurchaseDoesNotExist_ShouldReturnEmpty() {
        when(springDataRepository.findById(purchaseId)).thenReturn(Optional.empty());

        Optional<Purchase> result = repository.findById(purchaseId);

        assertTrue(result.isEmpty());
    }

    @Test
    void sumarGastosPorAlumnoEnRango_ShouldDelegateToSpringDataRepository() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(springDataRepository.sumarGastosGeneralesEnRango(
                studentId,
                EstadoCompra.ENTREGADO,
                start,
                end
        )).thenReturn(new BigDecimal("20.00"));

        BigDecimal result = repository.sumarGastosPorAlumnoEnRango(studentId, start, end);

        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    void sumarGastosPorCategoriaEnRango_ShouldDelegateToSpringDataRepository() {
        UUID categoryId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(springDataRepository.sumarGastosPorCategoriaEnRango(
                studentId,
                EstadoCompra.ENTREGADO,
                categoryId,
                start,
                end
        )).thenReturn(new BigDecimal("10.00"));

        BigDecimal result = repository.sumarGastosPorCategoriaEnRango(
                studentId,
                categoryId,
                start,
                end
        );

        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void findLastPurchasesByAlumno_ShouldMapToDomainSummary() {
        com.recred.backend.infrastructure.entities.ProductoEntity productoEntity =
                new com.recred.backend.infrastructure.entities.ProductoEntity();
        productoEntity.setNombre("Alfajor");
        CategoriaProductoEntity categoria = new CategoriaProductoEntity();
        categoria.setDescripcion("Snacks");
        productoEntity.setCategoria(categoria);

        CompraEntity compra = new CompraPresencialEntity();
        compra.setFechaHora(LocalDateTime.now());

        CompraDetalleEntity detalle = new CompraDetalleEntity();
        detalle.setProductoEntity(productoEntity);
        detalle.setCompra(compra);
        compra.setDetalles(List.of(detalle));

        when(springDataRepository.findLastByAlumno(eq(studentId), any(PageRequest.class)))
                .thenReturn(List.of(compra));

        List<PreferenceAIRepository.CompraResumen> result =
                repository.findLastPurchasesByAlumno(studentId, 5);

        assertEquals(1, result.size());
        assertEquals("Alfajor", result.get(0).productoNombre());
        assertEquals("Snacks", result.get(0).categoria());
    }

    @Test
    void findLastPurchasesByAlumno_ShouldHandleNullProductCategory() {
        com.recred.backend.infrastructure.entities.ProductoEntity productoEntity =
                new com.recred.backend.infrastructure.entities.ProductoEntity();
        productoEntity.setNombre("Agua");
        productoEntity.setCategoria(null);

        CompraEntity compra = new CompraPresencialEntity();
        compra.setFechaHora(LocalDateTime.now());

        CompraDetalleEntity detalle = new CompraDetalleEntity();
        detalle.setProductoEntity(productoEntity);
        detalle.setCompra(compra);
        compra.setDetalles(List.of(detalle));

        when(springDataRepository.findLastByAlumno(eq(studentId), any(PageRequest.class)))
                .thenReturn(List.of(compra));

        List<PreferenceAIRepository.CompraResumen> result =
                repository.findLastPurchasesByAlumno(studentId, 5);

        assertEquals("Sin Categoria", result.get(0).categoria());
    }

    private Purchase purchase(UUID id) {
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setStudent(student());
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setTotalAmount(new BigDecimal("12.00"));
        purchase.setDate(LocalDateTime.now());
        purchase.setItems(Map.of(product(), 2));
        purchase.setBuffetId(buffetId);
        return purchase;
    }

    private Alumno student() {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(productId);
        product.setPrecio(new BigDecimal("6.00"));
        return product;
    }

    private CompraPresencialEntity compraEntity() {
        CompraPresencialEntity entity = new CompraPresencialEntity();
        entity.setId(purchaseId);
        entity.setAlumnoEntity(alumnoEntity());
        entity.setMedioPago(MedioPago.CREDITOS);
        entity.setEstado(EstadoCompra.PENDIENTE);
        entity.setMontoTotal(new BigDecimal("12.00"));
        entity.setFechaHora(LocalDateTime.now());
        entity.setBuffetEntity(buffetEntity());
        return entity;
    }

    private AlumnoEntity alumnoEntity() {
        AlumnoEntity entity = new AlumnoEntity();
        entity.setId(studentId);
        return entity;
    }

    private BuffetEntity buffetEntity() {
        BuffetEntity entity = new BuffetEntity();
        entity.setId(buffetId);
        return entity;
    }

    @Test
    void findScheduledPickups_ShouldCallSpringDataRepository() {
        LocalDate fecha = LocalDate.of(2026, 6, 8);
        CompraAnticipadaEntity compraEntity = new CompraAnticipadaEntity();
        compraEntity.setId(purchaseId);
        compraEntity.setAlumnoEntity(alumnoEntity());
        compraEntity.setMedioPago(MedioPago.CREDITOS);
        compraEntity.setEstado(EstadoCompra.PENDIENTE);
        compraEntity.setMontoTotal(new BigDecimal("12.00"));
        compraEntity.setFechaHora(LocalDateTime.now());
        compraEntity.setBuffetEntity(buffetEntity());

        when(springDataRepository.findScheduledPickupsWithFilters(
                buffetId, fecha, EstadoRetiro.PROGRAMADO, null, "search"
        )).thenReturn(List.of(compraEntity));

        List<Purchase> result = repository.findScheduledPickups(
                buffetId, fecha, EstadoRetiro.PROGRAMADO, null, "search"
        );

        assertEquals(1, result.size());
        assertEquals(purchaseId, result.get(0).getId());
        verify(springDataRepository).findScheduledPickupsWithFilters(
                buffetId, fecha, EstadoRetiro.PROGRAMADO, null, "search"
        );
    }
}
