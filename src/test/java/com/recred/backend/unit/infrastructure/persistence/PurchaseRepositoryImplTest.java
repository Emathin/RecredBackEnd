package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.CartProductAffinity;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoRetiro;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.CompraAnticipadaEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.CompraPresencialEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.persistence.PurchaseRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataPurchaseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRepositoryImplTest {

    @Mock
    private SpringDataPurchaseRepository springDataRepository;

    @Mock
    private KiosqueroRealtimeEventPublisher realtimeEventPublisher;

    private PurchaseRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PurchaseRepositoryImpl(
                springDataRepository,
                realtimeEventPublisher,
                null
        );
    }

    @Test
    void saveShouldPersistAdvancePurchaseDetailsAndPublishRealtimeEvents() {
        UUID savedId = UUID.randomUUID();
        UUID promotionId = UUID.randomUUID();
        Purchase purchase = advancePurchase(promotionId);
        when(springDataRepository.save(any(CompraEntity.class))).thenAnswer(invocation -> {
            CompraEntity entity = invocation.getArgument(0);
            completeSavedEntity(entity, savedId);
            return entity;
        });

        Purchase result = repository.save(purchase);

        ArgumentCaptor<CompraEntity> savedCaptor = ArgumentCaptor.forClass(CompraEntity.class);
        verify(springDataRepository).save(savedCaptor.capture());
        CompraEntity savedEntity = savedCaptor.getValue();
        assertInstanceOf(CompraAnticipadaEntity.class, savedEntity);
        assertEquals(purchase.getStudent().getId(), savedEntity.getAlumnoEntity().getId());
        assertEquals(purchase.getBuyerId(), savedEntity.getTutorEntity().getId());
        assertEquals(purchase.getBuffetId(), savedEntity.getBuffetEntity().getId());
        assertEquals(purchase.getSuggestionId(), savedEntity.getSuggestionId());
        assertEquals(1, savedEntity.getDetalles().size());
        assertEquals(promotionId, savedEntity.getDetalles().get(0).getPromotionEntity().getId());

        CompraAnticipadaEntity advanceEntity = (CompraAnticipadaEntity) savedEntity;
        assertEquals("RET-123", advanceEntity.getCodigoVerificacion());
        assertNotNull(advanceEntity.getRetiroProgramadoEntity());
        assertEquals(EstadoRetiro.PROGRAMADO,
                advanceEntity.getRetiroProgramadoEntity().getEstado());

        assertEquals(savedId, result.getId());
        assertEquals(TipoCompra.ANTICIPADA, result.getTipo());

        ArgumentCaptor<KiosqueroRealtimeEvent> eventCaptor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(realtimeEventPublisher, times(2)).publish(eventCaptor.capture());
        assertEquals(KiosqueroRealtimeEventType.PURCHASE_CREATED,
                eventCaptor.getAllValues().get(0).type());
        assertEquals(KiosqueroRealtimeEventType.DASHBOARD_CHANGED,
                eventCaptor.getAllValues().get(1).type());
    }

    @Test
    void saveShouldReuseExistingEntityAndPublishUpdatedOrderEvent() {
        UUID purchaseId = UUID.randomUUID();
        Purchase purchase = presentialPurchase(purchaseId);
        CompraPresencialEntity existingEntity = new CompraPresencialEntity();
        when(springDataRepository.findById(purchaseId)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(any(CompraEntity.class))).thenAnswer(invocation -> {
            CompraEntity entity = invocation.getArgument(0);
            completeSavedEntity(entity, purchaseId);
            return entity;
        });

        Purchase result = repository.save(purchase);

        assertEquals(purchaseId, result.getId());
        ArgumentCaptor<KiosqueroRealtimeEvent> eventCaptor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(realtimeEventPublisher, times(2)).publish(eventCaptor.capture());
        assertEquals(KiosqueroRealtimeEventType.ORDER_UPDATED,
                eventCaptor.getAllValues().get(0).type());
        assertEquals(KiosqueroRealtimeEventType.DASHBOARD_CHANGED,
                eventCaptor.getAllValues().get(1).type());
    }

    @Test
    void constructorWithoutPublisherShouldStillPersistPurchase() {
        PurchaseRepositoryImpl defaultRepository = new PurchaseRepositoryImpl(
                springDataRepository);
        UUID savedId = UUID.randomUUID();
        Purchase purchase = presentialPurchase(null);
        when(springDataRepository.save(any(CompraEntity.class))).thenAnswer(invocation -> {
            CompraEntity entity = invocation.getArgument(0);
            completeSavedEntity(entity, savedId);
            return entity;
        });

        Purchase result = defaultRepository.save(purchase);

        assertEquals(savedId, result.getId());
    }

    @Test
    void saveShouldHandleTutorIdNoBuffetMissingPickupAndCustomWithdrawalStatus() {
        UUID tutorId = UUID.randomUUID();
        UUID savedId = UUID.randomUUID();
        Purchase noBuffetPurchase = advancePurchase(null);
        noBuffetPurchase.setBuffetId(null);
        noBuffetPurchase.setTutorId(tutorId);
        noBuffetPurchase.setBuyerType(null);
        noBuffetPurchase.setBuyerId(null);
        noBuffetPurchase.setPickupSlotId(null);
        noBuffetPurchase.setItems(Map.of(product(null), 1));
        Purchase customWithdrawalPurchase = advancePurchase(null);
        customWithdrawalPurchase.setWithdrawalStatus(EstadoRetiro.RETIRADO);
        when(springDataRepository.save(any(CompraEntity.class)))
                .thenAnswer(invocation -> {
                    CompraEntity entity = invocation.getArgument(0);
                    completeSavedEntity(entity, savedId);
                    return entity;
                });

        Purchase noBuffetResult = repository.save(noBuffetPurchase);
        Purchase customWithdrawalResult = repository.save(customWithdrawalPurchase);

        ArgumentCaptor<CompraEntity> entityCaptor = ArgumentCaptor.forClass(CompraEntity.class);
        verify(springDataRepository, times(2)).save(entityCaptor.capture());
        CompraAnticipadaEntity noBuffetEntity =
                (CompraAnticipadaEntity) entityCaptor.getAllValues().get(0);
        CompraAnticipadaEntity customWithdrawalEntity =
                (CompraAnticipadaEntity) entityCaptor.getAllValues().get(1);
        assertEquals(savedId, noBuffetResult.getId());
        assertEquals(savedId, customWithdrawalResult.getId());
        assertEquals(tutorId, noBuffetEntity.getTutorEntity().getId());
        assertNull(noBuffetEntity.getBuffetEntity());
        assertNull(noBuffetEntity.getRetiroProgramadoEntity());
        assertEquals(EstadoRetiro.RETIRADO,
                customWithdrawalEntity.getRetiroProgramadoEntity().getEstado());
    }

    @Test
    void saveShouldPublishDashboardWithCurrentDateWhenPurchaseDateIsNull() {
        UUID savedId = UUID.randomUUID();
        Purchase purchase = presentialPurchase(null);
        purchase.setDate(null);
        when(springDataRepository.save(any(CompraEntity.class))).thenAnswer(invocation -> {
            CompraEntity entity = invocation.getArgument(0);
            completeSavedEntity(entity, savedId);
            return entity;
        });

        repository.save(purchase);

        ArgumentCaptor<KiosqueroRealtimeEvent> eventCaptor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(realtimeEventPublisher, times(2)).publish(eventCaptor.capture());
        assertEquals(KiosqueroRealtimeEventType.DASHBOARD_CHANGED,
                eventCaptor.getAllValues().get(1).type());
    }

    @Test
    void findMethodsShouldMapPurchasesFromSpringData() {
        UUID studentId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 1, 23, 59);
        CompraPresencialEntity purchase = persistedPurchase(purchaseId);
        when(springDataRepository.findAllByAlumnoIdOrderByFechaHoraDesc(studentId))
                .thenReturn(List.of(purchase));
        when(springDataRepository.findAllByAlumnoIdAndFechaHoraBetween(studentId, start, end))
                .thenReturn(List.of(purchase));
        when(springDataRepository.findAllByTutorIdOrderByFechaHoraDesc(tutorId))
                .thenReturn(List.of(purchase));
        when(springDataRepository.findByIdWithDetails(purchaseId)).thenReturn(Optional.of(purchase));
        when(springDataRepository.findRecentByBuffetId(buffetId, PageRequest.of(0, 5)))
                .thenReturn(List.of(purchase));
        when(springDataRepository.findByBuffetIdAndStatuses(
                buffetId,
                List.of(EstadoCompra.PENDIENTE),
                PageRequest.of(0, 3))).thenReturn(List.of(purchase));
        when(springDataRepository.findByBuffetIdAndDateRange(buffetId, start, end))
                .thenReturn(List.of(purchase));

        assertEquals(purchaseId, repository.findHistorialByAlumno(studentId).get(0).getId());
        assertEquals(purchaseId, repository.findByAlumnoAndDateRange(
                studentId,
                start,
                end).get(0).getId());
        assertEquals(purchaseId, repository.findHistorialByTutor(tutorId).get(0).getId());
        assertEquals(purchaseId, repository.findById(purchaseId).orElseThrow().getId());
        assertEquals(purchaseId, repository.findRecentByBuffet(buffetId, 5).get(0).getId());
        assertEquals(purchaseId, repository.findByBuffetAndStatuses(
                buffetId,
                List.of(EstadoCompra.PENDIENTE),
                3).get(0).getId());
        assertEquals(purchaseId, repository.findByBuffetAndDateRange(
                buffetId,
                start,
                end).get(0).getId());
    }

    @Test
    void expiredAndPendingPurchaseQueriesShouldMapResults() {
        UUID purchaseId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 3);
        java.time.LocalTime time = java.time.LocalTime.of(10, 0);
        List<EstadoCompra> statuses = List.of(EstadoCompra.PENDIENTE);
        CompraPresencialEntity purchase = persistedPurchase(purchaseId);
        when(springDataRepository.findExpiredAdvancePurchases(date, time, statuses))
                .thenReturn(List.of(purchase));
        when(springDataRepository.findExpiredAdvancePurchasesByBuffet(
                buffetId,
                date,
                time,
                statuses)).thenReturn(List.of(purchase));
        when(springDataRepository.findAllByAlumnoIdAndEstadoInOrderByFechaHoraDesc(
                eq(studentId),
                any())).thenReturn(List.of(purchase));

        assertEquals(purchaseId,
                repository.findExpiredAdvancePurchases(date, time, statuses).get(0).getId());
        assertEquals(purchaseId,
                repository.findExpiredAdvancePurchases(
                        buffetId,
                        date,
                        time,
                        statuses).get(0).getId());
        assertEquals(purchaseId, repository.findPendientesByAlumno(studentId).get(0).getId());
    }

    @Test
    void sumExpensesShouldCombinePresentialAndAdvanceTotals() {
        UUID studentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 31, 23, 59);
        when(springDataRepository.sumarGastosPresencialesEnRango(
                studentId,
                EstadoCompra.ENTREGADO,
                start,
                end)).thenReturn(new BigDecimal("100.00"));
        when(springDataRepository.sumarGastosAnticipadosEnRango(
                eq(studentId),
                any(),
                eq(start.toLocalDate()),
                eq(end.toLocalDate()))).thenReturn(new BigDecimal("50.00"));
        when(springDataRepository.sumarGastosPresencialesPorCategoriaEnRango(
                studentId,
                EstadoCompra.ENTREGADO,
                categoryId,
                start,
                end)).thenReturn(new BigDecimal("70.00"));
        when(springDataRepository.sumarGastosAnticipadosPorCategoriaEnRango(
                eq(studentId),
                any(),
                eq(categoryId),
                eq(start.toLocalDate()),
                eq(end.toLocalDate()))).thenReturn(new BigDecimal("30.00"));

        BigDecimal total = repository.sumarGastosPorAlumnoEnRango(studentId, start, end);
        BigDecimal categoryTotal = repository.sumarGastosPorCategoriaEnRango(
                studentId,
                categoryId,
                start,
                end
        );

        assertEquals(new BigDecimal("150.00"), total);
        assertEquals(new BigDecimal("100.00"), categoryTotal);
    }

    @Test
    void findLastPurchasesByAlumnoShouldMapCategoryFallbackAndLimitResults() {
        UUID studentId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 7, 1, 12, 0);
        CompraPresencialEntity purchase = new CompraPresencialEntity();
        purchase.setFechaHora(date);
        purchase.addDetalle(detail(productEntity("Agua", null)));
        purchase.addDetalle(detail(productEntity("Alfajor", "Golosinas")));
        when(springDataRepository.findLastByAlumno(studentId, PageRequest.of(0, 1)))
                .thenReturn(List.of(purchase));

        List<PreferenceAIRepository.CompraResumen> result =
                repository.findLastPurchasesByAlumno(studentId, 1);

        assertEquals(1, result.size());
        assertEquals("Agua", result.get(0).productoNombre());
        assertEquals("Sin Categoria", result.get(0).categoria());
        assertEquals(date, result.get(0).fecha());
    }

    @Test
    void findLastPurchasesByAlumnoShouldMapExistingCategory() {
        UUID studentId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 7, 1, 12, 0);
        CompraPresencialEntity purchase = new CompraPresencialEntity();
        purchase.setFechaHora(date);
        purchase.addDetalle(detail(productEntity("Alfajor", "Golosinas")));
        when(springDataRepository.findLastByAlumno(studentId, PageRequest.of(0, 2)))
                .thenReturn(List.of(purchase));

        List<PreferenceAIRepository.CompraResumen> result =
                repository.findLastPurchasesByAlumno(studentId, 2);

        assertEquals(1, result.size());
        assertEquals("Golosinas", result.get(0).categoria());
    }

    @Test
    void affinitiesShouldReturnEmptyWithoutCartProductsAndDelegateWhenPresent() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CartProductAffinity affinity = new CartProductAffinity(productId, 3);

        assertTrue(repository.findStudentCartAffinities(studentId, buffetId, null).isEmpty());
        assertTrue(repository.findBuffetCartAffinities(buffetId, Set.of()).isEmpty());
        verify(springDataRepository, never()).findStudentCartAffinities(
                any(),
                any(),
                any(),
                any()
        );
        verify(springDataRepository, never()).findBuffetCartAffinities(any(), any(), any());

        when(springDataRepository.findStudentCartAffinities(
                eq(studentId),
                eq(buffetId),
                eq(EstadoCompra.ENTREGADO),
                any())).thenReturn(List.of(affinity));
        when(springDataRepository.findBuffetCartAffinities(
                eq(buffetId),
                eq(EstadoCompra.ENTREGADO),
                any())).thenReturn(List.of(affinity));

        assertEquals(List.of(affinity),
                repository.findStudentCartAffinities(studentId, buffetId, Set.of(productId)));
        assertEquals(List.of(affinity),
                repository.findBuffetCartAffinities(buffetId, Set.of(productId)));
    }

    @Test
    void findKiosqueroOrdersShouldCalculateDateRangeOnlyWhenDateIsPresent() {
        UUID buffetId = UUID.randomUUID();
        UUID pickupSlotId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 2);
        List<EstadoCompra> statuses = List.of(EstadoCompra.PENDIENTE);
        when(springDataRepository.findKiosqueroOrders(
                eq(buffetId),
                eq(statuses),
                eq(date),
                eq(date.atStartOfDay()),
                eq(date.plusDays(1).atStartOfDay().minusNanos(1)),
                eq(pickupSlotId),
                eq(PageRequest.of(0, 5)))).thenReturn(List.of());
        when(springDataRepository.findKiosqueroOrders(
                eq(buffetId),
                eq(statuses),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(PageRequest.of(0, 3)))).thenReturn(List.of());

        assertTrue(repository.findKiosqueroOrders(
                buffetId,
                statuses,
                date,
                pickupSlotId,
                5
        ).isEmpty());
        assertTrue(repository.findKiosqueroOrders(
                buffetId,
                statuses,
                null,
                null,
                3
        ).isEmpty());
    }

    @Test
    void simpleDelegatesShouldUseSpringDataRepository() {
        UUID buffetId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID pickupSlotId = UUID.randomUUID();
        LocalDate pickupDate = LocalDate.of(2026, 7, 3);
        LocalDateTime since = LocalDateTime.of(2026, 7, 1, 0, 0);
        List<EstadoCompra> statuses = List.of(EstadoCompra.PENDIENTE);
        when(springDataRepository.countScheduledAdvancePurchases(
                buffetId,
                pickupSlotId,
                pickupDate,
                statuses)).thenReturn(2L);
        when(springDataRepository.existsActiveFutureScheduledByPickupSlotId(
                pickupSlotId,
                pickupDate,
                statuses)).thenReturn(true);
        when(springDataRepository.sumScheduledProductQuantity(
                buffetId,
                productId,
                pickupDate,
                statuses)).thenReturn(4L);
        when(springDataRepository
                .countByAlumnoEntityIdAndSuggestionIdIsNotNullAndEstadoAndFechaHoraAfter(
                        studentId,
                        EstadoCompra.ENTREGADO,
                        since)).thenReturn(5L);
        when(springDataRepository.existsByCodigoVerificacionAndEstadoIn(
                "RET-123",
                statuses)).thenReturn(true);

        assertEquals(2L, repository.countScheduledAdvancePurchases(
                buffetId,
                pickupSlotId,
                pickupDate,
                statuses
        ));
        assertTrue(repository.existsActiveFutureScheduledByPickupSlotId(
                pickupSlotId,
                pickupDate,
                statuses
        ));
        assertEquals(4L, repository.sumScheduledProductQuantity(
                buffetId,
                productId,
                pickupDate,
                statuses
        ));
        assertEquals(5L, repository.countHealthyPurchasesByAlumno(studentId, since));
        assertTrue(repository.existsByVerificationCodeAndStatusIn("RET-123", statuses));
    }

    private Purchase advancePurchase(UUID promotionId) {
        Purchase purchase = basePurchase(null);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setBuyerType(BuyerType.TUTOR);
        purchase.setBuyerId(UUID.randomUUID());
        purchase.setPickupSlotId(UUID.randomUUID());
        purchase.setPickupDate(LocalDate.of(2026, 7, 2));
        purchase.setWithdrawalCode("RET-123");
        purchase.setItems(Map.of(product(promotionId), 2));
        return purchase;
    }

    private Purchase presentialPurchase(UUID purchaseId) {
        Purchase purchase = basePurchase(purchaseId);
        purchase.setTipo(TipoCompra.PRESENCIAL);
        purchase.setItems(Map.of(product(null), 1));
        return purchase;
    }

    private Purchase basePurchase(UUID purchaseId) {
        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStudent(student());
        purchase.setBuffetId(UUID.randomUUID());
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setTotalAmount(new BigDecimal("1000.00"));
        purchase.setDate(LocalDateTime.of(2026, 7, 1, 12, 0));
        purchase.setSuggestionId(UUID.randomUUID());
        return purchase;
    }

    private Alumno student() {
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        return student;
    }

    private Producto product(UUID promotionId) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Alfajor");
        product.setDescripcion("Chocolate");
        product.setPrecio(new BigDecimal("500.00"));
        product.setRequierePreparacion(false);
        product.setActivo(true);
        product.setPromocionId(promotionId);
        return product;
    }

    private void completeSavedEntity(CompraEntity entity, UUID id) {
        entity.setId(id);
        entity.getDetalles().forEach(this::completeSavedDetail);
    }

    private void completeSavedDetail(CompraDetalleEntity detail) {
        detail.getProductoEntity().setNombre("Alfajor");
        detail.getProductoEntity().setDescripcion("Chocolate");
        detail.getProductoEntity().setPrecio(new BigDecimal("500.00"));
        detail.getProductoEntity().setRequierePreparacion(false);
        detail.getProductoEntity().setActivo(true);
        if (detail.getPromotionEntity() != null)
            detail.getPromotionEntity().setName("Promo combo");
    }

    private CompraDetalleEntity detail(ProductoEntity product) {
        CompraDetalleEntity detail = new CompraDetalleEntity();
        detail.setProductoEntity(product);
        detail.setCantidad(1);
        detail.setPrecioUnitario(product.getPrecio());
        detail.setSubtotal(product.getPrecio());
        return detail;
    }

    private ProductoEntity productEntity(String name, String category) {
        ProductoEntity product = new ProductoEntity();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setDescripcion("Desc");
        product.setPrecio(new BigDecimal("500.00"));
        product.setRequierePreparacion(false);
        product.setActivo(true);
        product.setCategoria(category == null ? null : categoryEntity(category));
        return product;
    }

    private CategoriaProductoEntity categoryEntity(String description) {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setId(UUID.randomUUID());
        category.setDescripcion(description);
        category.setActivo(true);
        return category;
    }

    private CompraPresencialEntity persistedPurchase(UUID purchaseId) {
        CompraPresencialEntity purchase = new CompraPresencialEntity();
        purchase.setId(purchaseId);
        purchase.setAlumnoEntity(new com.recred.backend.infrastructure.entities.AlumnoEntity());
        purchase.getAlumnoEntity().setId(UUID.randomUUID());
        purchase.setMedioPago(MedioPago.CREDITOS);
        purchase.setEstado(EstadoCompra.PENDIENTE);
        purchase.setMontoTotal(new BigDecimal("500.00"));
        purchase.setFechaHora(LocalDateTime.of(2026, 7, 1, 12, 0));
        purchase.addDetalle(detail(productEntity("Alfajor", "Golosinas")));
        return purchase;
    }
}
