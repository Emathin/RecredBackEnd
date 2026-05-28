package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoRequeridoParaConsumoException;
import com.recred.backend.domain.exceptions.ConsumoAlumnoNoAutorizadoException;
import com.recred.backend.domain.exceptions.RangoFechasConsumoInvalidoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.StudentConsumptionDetail;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class GetLoggedUserConsumptionDetail {

    private static final int DEFAULT_LOOKBACK_DAYS = 30;
    private static final Set<EstadoCompra> CONSUMPTION_STATUSES = Set.of(
            EstadoCompra.ENTREGADO
    );

    private final ConsumptionAccessRepository accessRepository;
    private final StudentConsumptionRepository consumptionRepository;
    private final Clock clock;

    public GetLoggedUserConsumptionDetail(
            ConsumptionAccessRepository accessRepository,
            StudentConsumptionRepository consumptionRepository,
            Clock clock
    ) {
        this.accessRepository = accessRepository;
        this.consumptionRepository = consumptionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StudentConsumptionDetail execute(
            String cognitoSubject,
            StudentConsumptionCommand command
    ) {
        ConsumptionRequester requester = accessRepository
                .findRequesterByCognitoSubject(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));
        UUID studentId = resolveStudentId(requester, command.studentId());
        LocalDate to = command.to().orElse(LocalDate.now(clock));
        LocalDate from = command.from().orElse(to.minusDays(DEFAULT_LOOKBACK_DAYS));
        validateDateRange(from, to);

        List<StudentConsumptionPurchase> purchases =
                consumptionRepository.findPurchasesByStudentIdAndDateRange(
                        studentId,
                        CONSUMPTION_STATUSES,
                        startOfDay(from),
                        endOfDay(to)
                );
        return new StudentConsumptionDetail(
                studentId,
                from,
                to,
                totalAmount(purchases),
                purchases.size(),
                purchases
        );
    }

    private UUID resolveStudentId(
            ConsumptionRequester requester,
            Optional<UUID> requestedStudentId
    ) {
        if (requester.role() == RolUsuario.ALUMNO) {
            return resolveStudentForStudentUser(requester, requestedStudentId);
        }

        if (requester.role() == RolUsuario.PADRE) {
            return resolveStudentForTutorUser(requester, requestedStudentId);
        }

        UUID studentId = requestedStudentId.orElse(null);
        throw new ConsumoAlumnoNoAutorizadoException(studentId);
    }

    private UUID resolveStudentForStudentUser(
            ConsumptionRequester requester,
            Optional<UUID> requestedStudentId
    ) {
        UUID studentId = requester.studentId();

        if (studentId == null) {
            throw new ConsumoAlumnoNoAutorizadoException(null);
        }

        if (requestedStudentId.isPresent() && !studentId.equals(requestedStudentId.get())) {
            throw new ConsumoAlumnoNoAutorizadoException(requestedStudentId.get());
        }

        return studentId;
    }

    private UUID resolveStudentForTutorUser(
            ConsumptionRequester requester,
            Optional<UUID> requestedStudentId
    ) {
        UUID studentId = requestedStudentId
                .orElseThrow(AlumnoRequeridoParaConsumoException::new);

        if (!accessRepository.tutorCanAccessStudent(requester.userId(), studentId)) {
            throw new ConsumoAlumnoNoAutorizadoException(studentId);
        }

        return studentId;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new RangoFechasConsumoInvalidoException(from, to);
        }
    }

    private BigDecimal totalAmount(List<StudentConsumptionPurchase> purchases) {
        return purchases.stream()
                .map(StudentConsumptionPurchase::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
}
