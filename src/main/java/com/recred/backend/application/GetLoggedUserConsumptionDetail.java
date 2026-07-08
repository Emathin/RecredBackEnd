package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoRequeridoParaConsumoException;
import com.recred.backend.domain.exceptions.ConsumoAlumnoNoAutorizadoException;
import com.recred.backend.domain.exceptions.RangoFechasConsumoInvalidoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.EstadoCompra;
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
import java.util.UUID;

public class GetLoggedUserConsumptionDetail {

    private static final int DEFAULT_LOOKBACK_DAYS = 30;

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

    public GetLoggedUserConsumptionDetailResponse execute(
            GetLoggedUserConsumptionDetailRequest request
    ) {
        requireRequest(request);
        ConsumptionRequester requester = requester(request.cognitoSubject());
        UUID studentId = resolveStudentId(requester, request.requestedStudentId());
        LocalDate to = requestedTo(request);
        LocalDate from = requestedFrom(request, to);
        validateDateRange(from, to);
        return GetLoggedUserConsumptionDetailResponse.from(detail(studentId, from, to));
    }

    private void requireRequest(GetLoggedUserConsumptionDetailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
    }

    private ConsumptionRequester requester(String cognitoSubject) {
        return accessRepository.findRequesterByCognitoSubject(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));
    }

    private StudentConsumptionDetail detail(UUID studentId, LocalDate from, LocalDate to) {
        List<StudentConsumptionPurchase> purchases =
                consumptionRepository.findPurchasesByStudentIdAndDateRange(
                        studentId,
                        EstadoCompra.studentConsumptionStatuses(),
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
        if (requester.isStudent()) {
            return resolveStudentForStudentUser(requester, requestedStudentId);
        }

        if (requester.isTutor()) {
            return resolveStudentForTutorUser(requester, requestedStudentId);
        }

        UUID studentId = requestedStudentId.orElse(null);
        throw new ConsumoAlumnoNoAutorizadoException(studentId);
    }

    private UUID resolveStudentForStudentUser(
            ConsumptionRequester requester,
            Optional<UUID> requestedStudentId
    ) {
        if (!requester.hasStudentId()) {
            throw new ConsumoAlumnoNoAutorizadoException(null);
        }

        if (requestedStudentId.isPresent()
                && !requester.isStudentFor(requestedStudentId.get())) {
            throw new ConsumoAlumnoNoAutorizadoException(requestedStudentId.get());
        }

        return requester.studentId();
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

    private LocalDate requestedTo(GetLoggedUserConsumptionDetailRequest request) {
        return request.requestedTo().orElse(LocalDate.now(clock));
    }

    private LocalDate requestedFrom(GetLoggedUserConsumptionDetailRequest request, LocalDate to) {
        return request.requestedFrom().orElse(to.minusDays(DEFAULT_LOOKBACK_DAYS));
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
