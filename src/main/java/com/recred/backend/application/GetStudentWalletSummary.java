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
import com.recred.backend.domain.models.WalletHealthSpending;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public GetStudentWalletSummaryResponse execute(
            GetStudentWalletSummaryRequest summaryRequest) {
        summaryRequest.validate();

        ConsumptionRequester requester = findConsumptionRequester(
                summaryRequest.cognitoSubject());
        UUID studentId = studentIdVisibleToRequester(requester, summaryRequest.studentId());
        DateRange dateRange = dateRangeFrom(summaryRequest);
        Billetera wallet = findWallet(studentId);
        List<WalletMovement> movements = findMovements(studentId, dateRange);
        List<StudentConsumptionPurchase> purchases = findPurchases(studentId, dateRange);

        return responseFrom(summaryFrom(studentId, dateRange, wallet, movements, purchases));
    }

    private ConsumptionRequester findConsumptionRequester(String cognitoSubject) {
        return accessRepository.findRequesterByCognitoSubject(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(
                        cognitoSubject));
    }

    private DateRange dateRangeFrom(GetStudentWalletSummaryRequest summaryRequest) {
        LocalDate to = summaryRequest.to().orElse(LocalDate.now(clock));
        LocalDate from = summaryRequest.from().orElse(to.minusDays(DEFAULT_LOOKBACK_DAYS));
        ensureDateRangeIsChronological(from, to);
        return new DateRange(from, to, clock);
    }

    private Billetera findWallet(UUID studentId) {
        return walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Billetera no encontrada para el alumno: " + studentId));
    }

    private List<WalletMovement> findMovements(UUID studentId, DateRange dateRange) {
        return movementRepository.findByStudentIdAndDateRange(
                studentId,
                dateRange.startDateTime(),
                dateRange.endDateTime()
        );
    }

    private List<StudentConsumptionPurchase> findPurchases(
            UUID studentId,
            DateRange dateRange) {
        return consumptionRepository.findPurchasesByStudentIdAndDateRange(
                studentId,
                CONSUMPTION_STATUSES,
                dateRange.startDateTime(),
                dateRange.endDateTime()
        );
    }

    private StudentWalletSummary summaryFrom(
            UUID studentId,
            DateRange dateRange,
            Billetera wallet,
            List<WalletMovement> movements,
            List<StudentConsumptionPurchase> purchases) {
        BigDecimal spentAmount = totalSpentFrom(purchases);
        BigDecimal incomeAmount = totalIncomeFrom(movements);

        return new StudentWalletSummary(
                studentId,
                wallet.getSaldoCreditos(),
                dateRange.from(),
                dateRange.to(),
                incomeAmount,
                spentAmount,
                incomeAmount.subtract(spentAmount),
                purchases.size(),
                categorySpendingFrom(purchases, spentAmount),
                healthSpendingFrom(purchases, spentAmount),
                movements
        );
    }

    private GetStudentWalletSummaryResponse responseFrom(StudentWalletSummary summary) {
        return GetStudentWalletSummaryResponse.fromDomain(summary);
    }

    private UUID studentIdVisibleToRequester(
            ConsumptionRequester requester,
            UUID requestedStudentId) {
        if (requesterIsRequestedStudent(requester, requestedStudentId)) {
            return requester.studentId();
        }

        if (requesterIsTutorForStudent(requester, requestedStudentId)) {
            return requestedStudentId;
        }

        throw new ConsumoAlumnoNoAutorizadoException(requestedStudentId);
    }

    private boolean requesterIsRequestedStudent(
            ConsumptionRequester requester,
            UUID requestedStudentId) {
        return requester.role() == RolUsuario.ALUMNO
                && requester.studentId() != null
                && requester.studentId().equals(requestedStudentId);
    }

    private boolean requesterIsTutorForStudent(
            ConsumptionRequester requester,
            UUID requestedStudentId) {
        return requester.role() == RolUsuario.PADRE
                && accessRepository.tutorCanAccessStudent(
                        requester.userId(),
                        requestedStudentId);
    }

    private void ensureDateRangeIsChronological(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new RangoFechasConsumoInvalidoException(from, to);
        }
    }

    private BigDecimal totalIncomeFrom(List<WalletMovement> movements) {
        return movements.stream()
                .filter(movement -> movement.direction() == WalletMovementDirection.ENTRADA)
                .map(WalletMovement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalSpentFrom(List<StudentConsumptionPurchase> purchases) {
        return purchases.stream()
                .map(StudentConsumptionPurchase::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<WalletCategorySpending> categorySpendingFrom(
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

    private List<WalletHealthSpending> healthSpendingFrom(
            List<StudentConsumptionPurchase> purchases,
            BigDecimal totalSpent) {
        Map<String, BigDecimal> amountByClassification = new LinkedHashMap<>();
        purchases.stream()
                .flatMap(purchase -> purchase.products().stream())
                .forEach(product -> amountByClassification.merge(
                        healthClassification(product),
                        product.subtotal(),
                        BigDecimal::add
                ));

        return amountByClassification.entrySet()
                .stream()
                .map(entry -> new WalletHealthSpending(
                        entry.getKey(),
                        entry.getValue(),
                        percentage(entry.getValue(), totalSpent)
                ))
                .sorted(Comparator.comparing(WalletHealthSpending::amount).reversed())
                .toList();
    }

    private String healthClassification(StudentConsumptionProduct product) {
        if (productHasNoHealthClassifications(product)) {
            return "Sin clasificar";
        }
        return product.healthClassifications().get(0);
    }

    private boolean productHasNoHealthClassifications(StudentConsumptionProduct product) {
        return product.healthClassifications() == null
                || product.healthClassifications().isEmpty();
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(new BigDecimal("100"))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private record DateRange(LocalDate from, LocalDate to, Clock clock) {

        private LocalDateTime startDateTime() {
            ZoneId appZone = clock.getZone();
            ZonedDateTime zonedDateTime = from.atStartOfDay().atZone(appZone);
            return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        }

        private LocalDateTime endDateTime() {
            ZoneId appZone = clock.getZone();
            ZonedDateTime zonedDateTime = to.atTime(LocalTime.MAX).atZone(appZone);
            return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        }
    }
}
