package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ConsumoAlumnoNoAutorizadoException;
import com.recred.backend.domain.exceptions.RangoFechasConsumoInvalidoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.StudentConsumptionProduct;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import com.recred.backend.domain.models.StudentWalletSummary;
import com.recred.backend.domain.models.WalletCategorySpending;
import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetStudentWalletSummary {

    private static final int DEFAULT_LOOKBACK_DAYS = 30;
    private static final Set<EstadoCompra> CONSUMPTION_STATUSES = Set.of(
            EstadoCompra.ENTREGADO
    );

    private final ConsumptionAccessRepository accessRepository;
    private final WalletRepository walletRepository;
    private final MovimientoBilleteraRepository movementRepository;
    private final StudentConsumptionRepository consumptionRepository;
    private final Clock clock;

    public GetStudentWalletSummary(
            ConsumptionAccessRepository accessRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movementRepository,
            StudentConsumptionRepository consumptionRepository,
            Clock clock) {
        this.accessRepository = accessRepository;
        this.walletRepository = walletRepository;
        this.movementRepository = movementRepository;
        this.consumptionRepository = consumptionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StudentWalletSummary execute(
            String cognitoSubject,
            UUID requestedStudentId,
            Optional<LocalDate> requestedFrom,
            Optional<LocalDate> requestedTo) {
        ConsumptionRequester requester = accessRepository
                .findRequesterByCognitoSubject(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));
        UUID studentId = resolveStudentId(requester, requestedStudentId);
        LocalDate to = requestedTo.orElse(LocalDate.now(clock));
        LocalDate from = requestedFrom.orElse(to.minusDays(DEFAULT_LOOKBACK_DAYS));
        validateDateRange(from, to);

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        Billetera wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Billetera no encontrada para el alumno: " + studentId));
        List<WalletMovement> movements = movementRepository.findByStudentIdAndDateRange(
                studentId,
                start,
                end
        );
        List<StudentConsumptionPurchase> purchases =
                consumptionRepository.findPurchasesByStudentIdAndDateRange(
                        studentId,
                        CONSUMPTION_STATUSES,
                        start,
                        end
                );
        BigDecimal spentAmount = totalSpent(purchases);
        BigDecimal incomeAmount = totalIncome(movements);

        return new StudentWalletSummary(
                studentId,
                wallet.getSaldoCreditos(),
                from,
                to,
                incomeAmount,
                spentAmount,
                incomeAmount.subtract(spentAmount),
                purchases.size(),
                categorySpending(purchases, spentAmount),
                List.of(),
                movements
        );
    }

    private UUID resolveStudentId(ConsumptionRequester requester, UUID requestedStudentId) {
        if (requester.role() == RolUsuario.ALUMNO) {
            UUID studentId = requester.studentId();
            if (studentId == null || !studentId.equals(requestedStudentId)) {
                throw new ConsumoAlumnoNoAutorizadoException(requestedStudentId);
            }
            return studentId;
        }

        if (requester.role() == RolUsuario.PADRE
                && accessRepository.tutorCanAccessStudent(requester.userId(), requestedStudentId)) {
            return requestedStudentId;
        }

        throw new ConsumoAlumnoNoAutorizadoException(requestedStudentId);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new RangoFechasConsumoInvalidoException(from, to);
        }
    }

    private BigDecimal totalIncome(List<WalletMovement> movements) {
        return movements.stream()
                .filter(movement -> movement.direction() == WalletMovementDirection.ENTRADA)
                .map(WalletMovement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalSpent(List<StudentConsumptionPurchase> purchases) {
        return purchases.stream()
                .map(StudentConsumptionPurchase::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<WalletCategorySpending> categorySpending(
            List<StudentConsumptionPurchase> purchases,
            BigDecimal totalSpent) {
        Map<String, BigDecimal> amountByCategory = purchases.stream()
                .flatMap(purchase -> purchase.products().stream())
                .collect(Collectors.groupingBy(
                        this::categoryName,
                        Collectors.mapping(
                                StudentConsumptionProduct::subtotal,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        return amountByCategory.entrySet()
                .stream()
                .map(entry -> new WalletCategorySpending(
                        entry.getKey(),
                        entry.getValue(),
                        percentage(entry.getValue(), totalSpent)
                ))
                .sorted(Comparator.comparing(WalletCategorySpending::amount).reversed())
                .toList();
    }

    private String categoryName(StudentConsumptionProduct product) {
        return product.categoryName() == null ? "Sin categoria" : product.categoryName();
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(new BigDecimal("100"))
                .divide(total, 2, RoundingMode.HALF_UP);
    }
}
