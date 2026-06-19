package com.recred.backend.integration.persistence;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.events.PurchaseRealtimePayload;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.CartProductAffinity;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.CompraAnticipadaEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.CompraPresencialEntity;
import com.recred.backend.infrastructure.persistence.PurchaseRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataPurchaseRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRepositoryImplTest {

    @Mock
    private SpringDataPurchaseRepository springDataRepository;
    @Mock
    private KiosqueroRealtimeEventPublisher realtimeEventPublisher;

    private PurchaseRepositoryImpl repository;
    private UUID purchaseId;
    private UUID studentId;
    private UUID productId;
    private UUID buffetId;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        repository = new PurchaseRepositoryImpl(springDataRepository, realtimeEventPublisher);
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
        assertRealtimeEvent(KiosqueroRealtimeEventType.PURCHASE_CREATED, savedPurchaseId);
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
        assertRealtimeEvent(KiosqueroRealtimeEventType.ORDER_UPDATED, purchaseId);
    }

    private void assertRealtimeEvent(KiosqueroRealtimeEventType type, UUID expectedPurchaseId) {
        ArgumentCaptor<KiosqueroRealtimeEvent> eventCaptor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(realtimeEventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        KiosqueroRealtimeEvent purchaseEvent = eventCaptor.getAllValues()
                .stream()
                .filter(event -> event.type() == type)
                .findFirst()
                .orElseThrow();
        PurchaseRealtimePayload payload = assertInstanceOf(
                PurchaseRealtimePayload.class,
                purchaseEvent.payload()
        );
        assertEquals(expectedPurchaseId, payload.purchaseId());
        assertEquals(buffetId, purchaseEvent.buffetId());
        assertEquals(new BigDecimal("12.00"), payload.purchaseTotal());

        boolean dashboardEventFound = eventCaptor.getAllValues()
                .stream()
                .anyMatch(event -> event.type() == KiosqueroRealtimeEventType.DASHBOARD_CHANGED
                        && event.buffetId().equals(buffetId));
        assertTrue(dashboardEventFound);
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
    void save_WithSingleArgumentConstructor_ShouldPublishToNoopPublisher() {
        PurchaseRepositoryImpl repositoryWithNoopPublisher =
                new PurchaseRepositoryImpl(springDataRepository);
        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        Purchase savedPurchase = repositoryWithNoopPublisher.save(purchase);

        assertTrue(savedPurchase.getId() != null);
    }

    @Test
    void save_WhenPurchaseHasExplicitTutorId_ShouldSetTutorEntity() {
        UUID tutorId = UUID.randomUUID();
        purchase.setTutorId(tutorId);
        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        repository.save(purchase);

        ArgumentCaptor<CompraEntity> entityCaptor = ArgumentCaptor.forClass(CompraEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        assertEquals(tutorId, entityCaptor.getValue().getTutorEntity().getId());
    }

    @Test
    void save_WhenPurchaseHasNoBuffet_ShouldSkipDashboardEvent() {
        purchase.setBuffetId(null);
        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        repository.save(purchase);

        ArgumentCaptor<KiosqueroRealtimeEvent> eventCaptor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(realtimeEventPublisher).publish(eventCaptor.capture());
        assertFalse(eventCaptor.getAllValues().stream()
                .anyMatch(event -> event.type() == KiosqueroRealtimeEventType.DASHBOARD_CHANGED));
    }

    @Test
    void save_WhenPurchaseDateIsNull_ShouldUseCurrentDateForDashboardEvent() {
        purchase.setDate(null);
        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        repository.save(purchase);

        ArgumentCaptor<KiosqueroRealtimeEvent> eventCaptor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(realtimeEventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        KiosqueroRealtimeEvent dashboardEvent = eventCaptor.getAllValues().stream()
                .filter(event -> event.type() == KiosqueroRealtimeEventType.DASHBOARD_CHANGED)
                .findFirst()
                .orElseThrow();
        assertTrue(dashboardEvent.date() != null);
    }

    @Test
    void save_WhenAdvancePurchaseHasNoPickupData_ShouldOnlySetVerificationCode() {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setWithdrawalCode("ABC123");
        purchase.setPickupSlotId(null);
        purchase.setPickupDate(null);
        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        repository.save(purchase);

        ArgumentCaptor<CompraEntity> entityCaptor = ArgumentCaptor.forClass(CompraEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        CompraAnticipadaEntity entity = (CompraAnticipadaEntity) entityCaptor.getValue();
        assertEquals("ABC123", entity.getCodigoVerificacion());
        assertTrue(entity.getRetiroProgramadoEntity() == null);
    }

    @Test
    void save_WhenAdvancePurchaseHasWithdrawalStatus_ShouldUseItInScheduledPickup() {
        UUID pickupSlotId = UUID.randomUUID();
        LocalDate pickupDate = LocalDate.of(2026, 6, 8);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setWithdrawalCode("ABC123");
        purchase.setPickupSlotId(pickupSlotId);
        purchase.setPickupDate(pickupDate);
        purchase.setWithdrawalStatus(EstadoRetiro.LISTO);
        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        repository.save(purchase);

        ArgumentCaptor<CompraEntity> entityCaptor = ArgumentCaptor.forClass(CompraEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        CompraAnticipadaEntity entity = (CompraAnticipadaEntity) entityCaptor.getValue();
        assertEquals(EstadoRetiro.LISTO, entity.getRetiroProgramadoEntity().getEstado());
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
    void findByAlumnoAndDateRange_ShouldMapEntitiesToDomain() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        CompraPresencialEntity entity = compraEntity();
        when(springDataRepository.findAllByAlumnoIdAndFechaHoraBetween(
                studentId,
                start,
                end
        )).thenReturn(List.of(entity));

        List<Purchase> result = repository.findByAlumnoAndDateRange(studentId, start, end);

        assertEquals(1, result.size());
        assertEquals(purchaseId, result.get(0).getId());
    }

    @Test
    void sumarGastosPorAlumnoEnRango_ShouldDelegateToSpringDataRepository() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        when(springDataRepository.sumarGastosPresencialesEnRango(
                studentId,
                EstadoCompra.ENTREGADO,
                start,
                end
        )).thenReturn(new BigDecimal("15.00"));

        List<EstadoCompra> estadosAnticipados = List.of(
                EstadoCompra.ENTREGADO,
                EstadoCompra.PENDIENTE,
                EstadoCompra.EN_PREPARACION,
                EstadoCompra.LISTO
        );
        when(springDataRepository.sumarGastosAnticipadosEnRango(
                studentId,
                estadosAnticipados,
                start.toLocalDate(),
                end.toLocalDate()
        )).thenReturn(new BigDecimal("5.00"));

        BigDecimal result = repository.sumarGastosPorAlumnoEnRango(studentId, start, end);

        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    void sumarGastosPorCategoriaEnRango_ShouldDelegateToSpringDataRepository() {
        UUID categoryId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        when(springDataRepository.sumarGastosPresencialesPorCategoriaEnRango(
                studentId,
                EstadoCompra.ENTREGADO,
                categoryId,
                start,
                end
        )).thenReturn(new BigDecimal("8.00"));

        List<EstadoCompra> estadosAnticipados = List.of(
                EstadoCompra.ENTREGADO,
                EstadoCompra.PENDIENTE,
                EstadoCompra.EN_PREPARACION,
                EstadoCompra.LISTO
        );
        when(springDataRepository.sumarGastosAnticipadosPorCategoriaEnRango(
                studentId,
                estadosAnticipados,
                categoryId,
                start.toLocalDate(),
                end.toLocalDate()
        )).thenReturn(new BigDecimal("2.00"));

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

    @Test
    void save_WhenBuyerIsTutor_ShouldSetTutorReference() {
        UUID tutorId = UUID.randomUUID();
        purchase.setBuyerType(BuyerType.TUTOR);
        purchase.setBuyerId(tutorId);

        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        repository.save(purchase);

        ArgumentCaptor<CompraEntity> entityCaptor = ArgumentCaptor.forClass(CompraEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        assertEquals(tutorId, entityCaptor.getValue().getTutorEntity().getId());
    }

    @Test
    void findHistorialByAlumno_ShouldMapEntitiesToDomain() {
        CompraPresencialEntity entity = compraEntity();
        when(springDataRepository.findAllByAlumnoIdOrderByFechaHoraDesc(studentId))
                .thenReturn(List.of(entity));

        List<Purchase> result = repository.findHistorialByAlumno(studentId);

        assertEquals(1, result.size());
        assertEquals(purchaseId, result.get(0).getId());
    }

    @Test
    void findHistorialByTutor_ShouldMapEntitiesToDomain() {
        UUID tutorId = UUID.randomUUID();
        CompraPresencialEntity entity = compraEntity();
        when(springDataRepository.findAllByTutorIdOrderByFechaHoraDesc(tutorId))
                .thenReturn(List.of(entity));

        List<Purchase> result = repository.findHistorialByTutor(tutorId);

        assertEquals(1, result.size());
        assertEquals(purchaseId, result.get(0).getId());
    }

    @Test
    void findRecentByBuffet_ShouldUseLimitPageRequest() {
        CompraPresencialEntity entity = compraEntity();
        when(springDataRepository.findRecentByBuffetId(eq(buffetId), any(PageRequest.class)))
                .thenReturn(List.of(entity));

        List<Purchase> result = repository.findRecentByBuffet(buffetId, 2);

        assertEquals(1, result.size());
        assertEquals(purchaseId, result.get(0).getId());
        verify(springDataRepository).findRecentByBuffetId(
                eq(buffetId),
                eq(PageRequest.of(0, 2))
        );
    }

    @Test
    void findByBuffetAndStatuses_ShouldUseLimitPageRequest() {
        List<EstadoCompra> statuses = List.of(EstadoCompra.PENDIENTE);
        CompraPresencialEntity entity = compraEntity();
        when(springDataRepository.findByBuffetIdAndStatuses(
                eq(buffetId),
                eq(statuses),
                any(PageRequest.class)
        )).thenReturn(List.of(entity));

        List<Purchase> result = repository.findByBuffetAndStatuses(buffetId, statuses, 4);

        assertEquals(1, result.size());
        verify(springDataRepository).findByBuffetIdAndStatuses(
                buffetId,
                statuses,
                PageRequest.of(0, 4)
        );
    }

    @Test
    void findKiosqueroOrders_WithDate_ShouldBuildDayRange() {
        LocalDate date = LocalDate.of(2026, 6, 8);
        UUID pickupSlotId = UUID.randomUUID();
        List<EstadoCompra> statuses = List.of(EstadoCompra.PENDIENTE);
        CompraPresencialEntity entity = compraEntity();
        when(springDataRepository.findKiosqueroOrders(
                eq(buffetId),
                eq(statuses),
                eq(date),
                eq(date.atStartOfDay()),
                eq(date.plusDays(1).atStartOfDay().minusNanos(1)),
                eq(pickupSlotId),
                any(PageRequest.class)
        )).thenReturn(List.of(entity));

        List<Purchase> result = repository.findKiosqueroOrders(
                buffetId,
                statuses,
                date,
                pickupSlotId,
                10
        );

        assertEquals(1, result.size());
        verify(springDataRepository).findKiosqueroOrders(
                buffetId,
                statuses,
                date,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1),
                pickupSlotId,
                PageRequest.of(0, 10)
        );
    }

    @Test
    void findKiosqueroOrders_WithoutDate_ShouldPassNullRange() {
        List<EstadoCompra> statuses = List.of(EstadoCompra.PENDIENTE);
        when(springDataRepository.findKiosqueroOrders(
                eq(buffetId),
                eq(statuses),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(PageRequest.class)
        )).thenReturn(List.of());

        List<Purchase> result = repository.findKiosqueroOrders(
                buffetId,
                statuses,
                null,
                null,
                5
        );

        assertTrue(result.isEmpty());
        verify(springDataRepository).findKiosqueroOrders(
                buffetId,
                statuses,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 5)
        );
    }

    @Test
    void findByBuffetAndDateRange_ShouldMapResults() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(springDataRepository.findByBuffetIdAndDateRange(buffetId, start, end))
                .thenReturn(List.of(compraEntity()));

        List<Purchase> result = repository.findByBuffetAndDateRange(buffetId, start, end);

        assertEquals(1, result.size());
        assertEquals(purchaseId, result.get(0).getId());
    }

    @Test
    void findExpiredAdvancePurchases_ShouldDelegateBothVariants() {
        LocalDate date = LocalDate.of(2026, 6, 8);
        java.time.LocalTime time = java.time.LocalTime.of(10, 30);
        List<EstadoCompra> statuses = List.of(EstadoCompra.LISTO);
        when(springDataRepository.findExpiredAdvancePurchases(date, time, statuses))
                .thenReturn(List.of(compraEntity()));
        when(springDataRepository.findExpiredAdvancePurchasesByBuffet(
                buffetId,
                date,
                time,
                statuses
        )).thenReturn(List.of(compraEntity()));

        assertEquals(1, repository.findExpiredAdvancePurchases(date, time, statuses).size());
        assertEquals(1, repository.findExpiredAdvancePurchases(
                buffetId,
                date,
                time,
                statuses
        ).size());
    }

    @Test
    void countAndSumScheduledAdvancePurchases_ShouldDelegate() {
        UUID pickupSlotId = UUID.randomUUID();
        LocalDate pickupDate = LocalDate.of(2026, 6, 8);
        List<EstadoCompra> statuses = List.of(EstadoCompra.PENDIENTE);
        when(springDataRepository.countScheduledAdvancePurchases(
                buffetId,
                pickupSlotId,
                pickupDate,
                statuses
        )).thenReturn(3L);
        when(springDataRepository.sumScheduledProductQuantity(
                buffetId,
                productId,
                pickupDate,
                statuses
        )).thenReturn(7L);

        assertEquals(3L, repository.countScheduledAdvancePurchases(
                buffetId,
                pickupSlotId,
                pickupDate,
                statuses
        ));
        assertEquals(7L, repository.sumScheduledProductQuantity(
                buffetId,
                productId,
                pickupDate,
                statuses
        ));
    }

    @Test
    void findCartAffinities_WhenCartIsEmpty_ShouldReturnEmptyWithoutQuerying() {
        assertTrue(repository.findStudentCartAffinities(
                studentId,
                buffetId,
                Set.of()
        ).isEmpty());
        assertTrue(repository.findBuffetCartAffinities(buffetId, null).isEmpty());
    }

    @Test
    void findCartAffinities_WhenCartHasProducts_ShouldDelegate() {
        UUID suggestedProductId = UUID.randomUUID();
        Set<UUID> cartProductIds = Set.of(productId);
        List<CartProductAffinity> affinities = List.of(new CartProductAffinity(
                suggestedProductId,
                2
        ));
        when(springDataRepository.findStudentCartAffinities(
                eq(studentId),
                eq(buffetId),
                eq(EstadoCompra.ENTREGADO),
                eq(List.copyOf(cartProductIds))
        )).thenReturn(affinities);
        when(springDataRepository.findBuffetCartAffinities(
                eq(buffetId),
                eq(EstadoCompra.ENTREGADO),
                eq(List.copyOf(cartProductIds))
        )).thenReturn(affinities);

        assertEquals(affinities, repository.findStudentCartAffinities(
                studentId,
                buffetId,
                cartProductIds
        ));
        assertEquals(affinities, repository.findBuffetCartAffinities(
                buffetId,
                cartProductIds
        ));
    }

    @Test
    void findPurchasesByAlumno_ShouldReturnRepositoryProjection() {
        PurchaseResume resume = new PurchaseResume(
                studentId,
                productId,
                "Alfajor",
                new BigDecimal("12.00"),
                LocalDateTime.now()
        );
        when(springDataRepository.findByAlumno(studentId)).thenReturn(List.of(resume));

        List<PurchaseResume> result = repository.findPurchasesByAlumno(studentId);

        assertEquals(1, result.size());
        assertEquals("Alfajor", result.get(0).getNombreProducto());
    }

    @Test
    void countHealthyPurchasesByAlumno_ShouldDelegateWithDeliveredStatus() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        when(springDataRepository
                .countByAlumnoEntityIdAndSuggestionIdIsNotNullAndEstadoAndFechaHoraAfter(
                        studentId,
                        EstadoCompra.ENTREGADO,
                        since
                )).thenReturn(5L);

        assertEquals(5L, repository.countHealthyPurchasesByAlumno(studentId, since));
    }

    @Test
    void findPendientesByAlumno_ShouldUsePendingStatuses() {
        when(springDataRepository.findAllByAlumnoIdAndEstadoInOrderByFechaHoraDesc(
                eq(studentId),
                eq(List.of(
                        EstadoCompra.PENDIENTE,
                        EstadoCompra.EN_PREPARACION,
                        EstadoCompra.LISTO
                ))
        )).thenReturn(List.of(compraEntity()));

        List<Purchase> result = repository.findPendientesByAlumno(studentId);

        assertEquals(1, result.size());
        assertEquals(purchaseId, result.get(0).getId());
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
