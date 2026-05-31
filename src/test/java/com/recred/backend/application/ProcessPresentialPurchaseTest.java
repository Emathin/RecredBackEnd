package com.recred.backend.application;

import com.recred.backend.domain.exceptions.StudentNotFoundException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.TimeRestrictionException;
import com.recred.backend.domain.exceptions.HealthRestrictionException;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.Wallet;
import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.RestrictionsValidatorService;
import com.recred.backend.domain.services.TimeValidatorService;
import com.recred.backend.domain.services.BudgetValidatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPresentialPurchaseTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private TimeValidatorService timeValidator;
    @Mock
    private RestrictionsValidatorService healthValidator;
    @Mock
    private BudgetValidatorService budgetValidator;

    @InjectMocks
    private ProcessPresentialPurchase useCase;

    private UUID studentId;
    private Student student;
    private Wallet wallet;
    private Map<Product, Integer> cart;
    private Product product;
    private ProductCategory category;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new Student();
        student.setId(studentId);

        wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setCreditBalance(new BigDecimal("100.00"));

        category = new ProductCategory();
        category.setId(UUID.randomUUID());

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setPrice(new BigDecimal("10.00"));
        product.setCategory(category);

        cart = new HashMap<>();
        cart.put(product, 2);
    }

    @Test
    void execute_WithValidData_ShouldProcessPurchaseAndCallValidators() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(studentId, cart);

        verify(timeValidator).validatePurchase(
                eq(student),
                eq(cart.keySet()),
                any(LocalDateTime.class)
        );
        verify(healthValidator).validatePurchase(student, cart.keySet());

        Map<UUID, BigDecimal> expectedCategoryTotals = new HashMap<>();
        expectedCategoryTotals.put(category.getId(), new BigDecimal("20.00"));

        verify(budgetValidator).validateBudget(
                eq(studentId),
                eq(new BigDecimal("20.00")),
                eq(expectedCategoryTotals),
                any(LocalDateTime.class)
        );

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(walletRepository).save(any(Wallet.class));
        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertEquals(PurchaseState.ENTREGADO, purchaseCaptor.getValue().getStatus());
    }

    @Test
    void execute_WhenProductRequiresPreparation_ShouldSavePendingPurchase() {
        product.setRequiresPreparation(true);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(studentId, cart);

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertEquals(PurchaseState.PENDIENTE, purchaseCaptor.getValue().getStatus());
    }

    @Test
    void execute_WhenProductHasNoCategory_ShouldValidateEmptyCategoryTotals() {
        product.setCategory(null);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(studentId, cart);

        verify(budgetValidator).validateBudget(
                eq(studentId),
                eq(new BigDecimal("20.00")),
                eq(Map.of()),
                any(LocalDateTime.class)
        );
    }

    @Test
    void execute_WhenProductCategoryHasNoId_ShouldValidateEmptyCategoryTotals() {
        category.setId(null);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(studentId, cart);

        verify(budgetValidator).validateBudget(
                eq(studentId),
                eq(new BigDecimal("20.00")),
                eq(Map.of()),
                any(LocalDateTime.class)
        );
    }

    @Test
    void execute_WhenProductPriceIsNull_ShouldUseZeroTotals() {
        product.setPrice(null);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        useCase.execute(studentId, cart);

        Map<UUID, BigDecimal> expectedCategoryTotals = new HashMap<>();
        expectedCategoryTotals.put(category.getId(), BigDecimal.ZERO);
        verify(budgetValidator).validateBudget(
                eq(studentId),
                eq(BigDecimal.ZERO),
                eq(expectedCategoryTotals),
                any(LocalDateTime.class)
        );
    }

    @Test
    void execute_WhenStudentNotFound_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class, () -> useCase.execute(studentId, cart));

        verifyNoInteractions(
                timeValidator,
                healthValidator,
                budgetValidator,
                walletRepository,
                purchaseRepository
        );
    }

    @Test
    void execute_WhenInsufficientBalance_ShouldThrowException() {
        wallet.setCreditBalance(new BigDecimal("10.00"));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class,
                () -> useCase.execute(studentId, cart));

        verify(timeValidator).validatePurchase(
                eq(student),
                eq(cart.keySet()),
                any(LocalDateTime.class)
        );
        verify(healthValidator).validatePurchase(student, cart.keySet());
        verify(budgetValidator).validateBudget(any(), any(), any(), any());
        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenWalletNotFound_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> useCase.execute(studentId, cart));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenHealthRestrictionFails_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        doThrow(new HealthRestrictionException(""))
                .when(healthValidator)
                .validatePurchase(student, cart.keySet());

        assertThrows(HealthRestrictionException.class, () -> useCase.execute(studentId, cart));

        verify(timeValidator).validatePurchase(
                eq(student),
                eq(cart.keySet()),
                any(LocalDateTime.class)
        );
        verifyNoInteractions(budgetValidator, walletRepository, purchaseRepository);
    }

    @Test
    void execute_WhenTimeRestrictionFails_ShouldThrowException() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        doThrow(new TimeRestrictionException(""))
                .when(timeValidator)
                .validatePurchase(
                        eq(student),
                        eq(cart.keySet()),
                        any(LocalDateTime.class)
                );

        assertThrows(TimeRestrictionException.class,
                () -> useCase.execute(studentId, cart));

        verifyNoInteractions(
                healthValidator,
                budgetValidator,
                walletRepository,
                purchaseRepository
        );
    }
}
