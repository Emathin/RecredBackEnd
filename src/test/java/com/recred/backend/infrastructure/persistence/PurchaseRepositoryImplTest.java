package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.PaymentMethod;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.ProductCategoryEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import com.recred.backend.infrastructure.entities.PurchaseDetailEntity;
import com.recred.backend.infrastructure.entities.PurchaseEntity;
import com.recred.backend.infrastructure.entities.PresentialPurchaseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
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
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        repository = new PurchaseRepositoryImpl(springDataRepository);
        purchaseId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        productId = UUID.randomUUID();
        purchase = purchase(null);
    }

    @Test
    void save_WhenPurchaseDoesNotHaveId_ShouldCreatePresentialPurchase() {
        UUID savedPurchaseId = UUID.randomUUID();
        when(springDataRepository.save(any(PurchaseEntity.class)))
                .thenAnswer(invocation -> {
                    PurchaseEntity entity = invocation.getArgument(0);
                    entity.setId(savedPurchaseId);
                    return entity;
                });

        Purchase savedPurchase = repository.save(purchase);

        ArgumentCaptor<PurchaseEntity> entityCaptor = ArgumentCaptor.forClass(PurchaseEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        PurchaseEntity savedEntity = entityCaptor.getValue();
        assertTrue(savedEntity instanceof PresentialPurchaseEntity);
        assertEquals(studentId, savedEntity.getStudentEntity().getId());
        assertEquals(PurchaseState.PENDIENTE, savedEntity.getPurchaseState());
        assertEquals(productId, savedEntity.getDetails().get(0).getProductEntity().getId());
        assertEquals(savedPurchaseId, savedPurchase.getId());
    }

    @Test
    void save_WhenPurchaseHasId_ShouldUpdateExistingPurchase() {
        PresentialPurchaseEntity existingEntity = new PresentialPurchaseEntity();
        existingEntity.setId(purchaseId);
        existingEntity.addDetalle(new PurchaseDetailEntity());
        purchase.setId(purchaseId);

        when(springDataRepository.findById(purchaseId)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(existingEntity)).thenReturn(existingEntity);

        Purchase savedPurchase = repository.save(purchase);

        assertEquals(purchaseId, savedPurchase.getId());
        assertEquals(1, existingEntity.getDetails().size());
        assertEquals(productId, existingEntity.getDetails().get(0).getProductEntity().getId());
        verify(springDataRepository).findById(purchaseId);
        verify(springDataRepository).save(existingEntity);
    }

    @Test
    void findById_WhenPurchaseExists_ShouldReturnMappedPurchase() {
        PresentialPurchaseEntity entity = compraEntity();
        when(springDataRepository.findById(purchaseId)).thenReturn(Optional.of(entity));

        Optional<Purchase> result = repository.findById(purchaseId);

        assertTrue(result.isPresent());
        assertEquals(purchaseId, result.get().getId());
        assertEquals(studentId, result.get().getStudent().getId());
    }

    @Test
    void findById_WhenPurchaseDoesNotExist_ShouldReturnEmpty() {
        when(springDataRepository.findById(purchaseId)).thenReturn(Optional.empty());

        Optional<Purchase> result = repository.findById(purchaseId);

        assertTrue(result.isEmpty());
    }

    @Test
    void sumExpensesByStudentInRange_ShouldDelegateToSpringDataRepository() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(springDataRepository.sumarGastosGeneralesEnRango(
                studentId,
                PurchaseState.ENTREGADO,
                start,
                end
        )).thenReturn(new BigDecimal("20.00"));

        BigDecimal result = repository.sumExpensesByStudentInRange(studentId, start, end);

        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    void sumExpensesByCategoryInRange_ShouldDelegateToSpringDataRepository() {
        UUID categoryId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(springDataRepository.sumarGastosPorCategoriaEnRango(
                studentId,
                PurchaseState.ENTREGADO,
                categoryId,
                start,
                end
        )).thenReturn(new BigDecimal("10.00"));

        BigDecimal result = repository.sumExpensesByCategoryInRange(
                studentId,
                categoryId,
                start,
                end
        );

        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void findLastPurchasesByStudent_ShouldMapToDomainSummary() {
        ProductEntity productEntity =
                new ProductEntity();
        productEntity.setName("Alfajor");
        ProductCategoryEntity categoria = new ProductCategoryEntity();
        categoria.setDescription("Snacks");
        productEntity.setCategory(categoria);

        PurchaseEntity compra = new PresentialPurchaseEntity();
        compra.setDateAndTime(LocalDateTime.now());

        PurchaseDetailEntity detalle = new PurchaseDetailEntity();
        detalle.setProductEntity(productEntity);
        detalle.setPurchase(compra);
        compra.setDetails(List.of(detalle));

        when(springDataRepository.findLastByAlumno(eq(studentId), any(PageRequest.class)))
                .thenReturn(List.of(compra));

        List<PreferenceAIRepository.PurchaseSummary> result =
                repository.findLastPurchasesByStudent(studentId, 5);

        assertEquals(1, result.size());
        assertEquals("Alfajor", result.get(0).productName());
        assertEquals("Snacks", result.get(0).category());
    }

    @Test
    void findLastPurchasesByStudent_ShouldHandleNullProductCategory() {
        ProductEntity productEntity =
                new ProductEntity();
        productEntity.setName("Agua");
        productEntity.setCategory(null);

        PurchaseEntity compra = new PresentialPurchaseEntity();
        compra.setDateAndTime(LocalDateTime.now());

        PurchaseDetailEntity detalle = new PurchaseDetailEntity();
        detalle.setProductEntity(productEntity);
        detalle.setPurchase(compra);
        compra.setDetails(List.of(detalle));

        when(springDataRepository.findLastByAlumno(eq(studentId), any(PageRequest.class)))
                .thenReturn(List.of(compra));

        List<PreferenceAIRepository.PurchaseSummary> result =
                repository.findLastPurchasesByStudent(studentId, 5);

        assertEquals("Sin Categoria", result.get(0).category());
    }

    private Purchase purchase(UUID id) {
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setStudent(student());
        purchase.setPaymentMethod(PaymentMethod.CREDITOS);
        purchase.setStatus(PurchaseState.PENDIENTE);
        purchase.setTotalAmount(new BigDecimal("12.00"));
        purchase.setDate(LocalDateTime.now());
        purchase.setItems(Map.of(product(), 2));
        return purchase;
    }

    private Student student() {
        Student student = new Student();
        student.setId(studentId);
        return student;
    }

    private Product product() {
        Product product = new Product();
        product.setId(productId);
        product.setPrice(new BigDecimal("6.00"));
        return product;
    }

    private PresentialPurchaseEntity compraEntity() {
        PresentialPurchaseEntity entity = new PresentialPurchaseEntity();
        entity.setId(purchaseId);
        entity.setStudentEntity(alumnoEntity());
        entity.setPaymentMethod(PaymentMethod.CREDITOS);
        entity.setPurchaseState(PurchaseState.PENDIENTE);
        entity.setTotalAmount(new BigDecimal("12.00"));
        entity.setDateAndTime(LocalDateTime.now());
        return entity;
    }

    private StudentEntity alumnoEntity() {
        StudentEntity entity = new StudentEntity();
        entity.setId(studentId);
        return entity;
    }
}
