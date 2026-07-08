package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class StudentAssistantContextBuilder {

    private static final int RECENT_PAYMENT_LIMIT = 5;
    private static final int RECENT_PURCHASE_LIMIT = 10;
    private static final int UPCOMING_EVENT_DAYS = 30;

    private final WalletRepository walletRepository;
    private final SchoolAssistantPaymentRepository paymentRepository;
    private final SchoolAssistantPurchaseRepository purchaseRepository;
    private final SchoolCalendarRepository schoolCalendarRepository;
    private final SchoolAssistantRoleCatalog roleCatalog;
    private final Clock clock;

    public StudentAssistantContextBuilder(
            WalletRepository walletRepository,
            SchoolAssistantPaymentRepository paymentRepository,
            SchoolAssistantPurchaseRepository purchaseRepository,
            SchoolCalendarRepository schoolCalendarRepository,
            SchoolAssistantRoleCatalog roleCatalog,
            Clock clock
    ) {
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.schoolCalendarRepository = schoolCalendarRepository;
        this.roleCatalog = roleCatalog;
        this.clock = clock;
    }

    public SchoolAssistantContext build(UUID studentId) {
        LocalDate today = LocalDate.now(clock);
        return new SchoolAssistantContext(
                studentId,
                walletRepository.findByStudentId(studentId).map(Billetera::getSaldoCreditos),
                paymentRepository.findRecentByStudentId(studentId, RECENT_PAYMENT_LIMIT),
                purchaseRepository.findRecentByStudentId(studentId, RECENT_PURCHASE_LIMIT),
                schoolCalendarRepository.findByStudentIdAndDateRange(
                        studentId,
                        today,
                        today.plusDays(UPCOMING_EVENT_DAYS)
                ),
                SchoolAssistantRole.ALUMNO,
                null,
                null,
                List.of(),
                roleCatalog.capabilitiesFor(SchoolAssistantRole.ALUMNO)
        );
    }
}
