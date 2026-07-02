package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GeneratePriceAlert {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratePriceAlert.class);
    private static final int COOLDOWN_HOURS = 24;

    private final StudentAIProfileRepository studentAIProfileRepository;
    private final PriceAlertAIRepository priceAlertAIRepository;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final Clock clock;

    public GeneratePriceAlert(
            StudentAIProfileRepository studentAIProfileRepository,
            PriceAlertAIRepository priceAlertAIRepository,
            SugerenciaIARepository sugerenciaIARepository,
            Clock clock) {
        this.studentAIProfileRepository = studentAIProfileRepository;
        this.priceAlertAIRepository = priceAlertAIRepository;
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.clock = clock;
    }

    @Transactional
    public void execute(GeneratePriceAlertRequest request) {
        request.validate();

        Producto product = request.product();
        List<Alumno> affectedStudents = findStudentsWithFavoriteProduct(product);

        generateAlertsForAffectedStudents(request, affectedStudents);
    }

    private void generateAlertsForAffectedStudents(
            GeneratePriceAlertRequest request,
            List<Alumno> affectedStudents
    ) {
        Producto product = request.product();

        for (Alumno alumno : affectedStudents) {
            if (shouldSkipRecentAlert(alumno, product)) {
                continue;
            }

            generateAndSaveAlert(alumno, request);
        }
    }

    private List<Alumno> findStudentsWithFavoriteProduct(Producto product) {
        return studentAIProfileRepository.findStudentsWithFavoriteProduct(product.getId());
    }

    private boolean shouldSkipRecentAlert(Alumno student, Producto product) {
        if (!hasRecentAlert(student.getId(), product.getId())) {
            return false;
        }

        LOGGER.info(
                "price_alert_skipped_recent studentId={} productId={}",
                student.getId(),
                product.getId()
        );
        return true;
    }

    private void generateAndSaveAlert(Alumno student, GeneratePriceAlertRequest request) {
        Producto product = request.product();
        priceAlertAIRepository.analyzeImpact(
                        student,
                        product,
                        request.oldPrice(),
                        request.newPrice()
                )
                .map(result -> buildPriceAlertSuggestion(student, request, result))
                .ifPresentOrElse(
                        sugerenciaIARepository::savePriceAlert,
                        () -> logEmptyAIResult(student, product)
                );
    }

    private PriceAlertSuggestion buildPriceAlertSuggestion(
            Alumno student,
            GeneratePriceAlertRequest request,
            PriceAlertAIRepository.PriceAlertResult result
    ) {
        Producto product = request.product();
        return new PriceAlertSuggestion(
                student.getId(),
                student.getUsuarioId(),
                product.getId(),
                request.buffetId(),
                tutorUserIds(student),
                product.getNombre(),
                result.titulo(),
                result.mensaje(),
                request.oldPrice(),
                request.newPrice(),
                result.razonIA()
        );
    }

    private void logEmptyAIResult(Alumno student, Producto product) {
        LOGGER.warn(
                "price_alert_ai_empty studentId={} productId={}",
                student.getId(),
                product.getId()
        );
    }

    private boolean hasRecentAlert(UUID studentId, UUID productId) {
        return sugerenciaIARepository.hasRecentPriceAlert(
                studentId,
                productId,
                recentAlertLimit()
        );
    }

    private LocalDateTime recentAlertLimit() {
        return LocalDateTime.now(clock).minusHours(COOLDOWN_HOURS);
    }

    private Set<UUID> tutorUserIds(Alumno alumno) {
        return alumno.getTutores().stream()
                .filter(tutor -> tutor.getUsuario() != null)
                .filter(tutor -> tutor.getUsuario().getId() != null)
                .map(tutor -> tutor.getUsuario().getId())
                .collect(Collectors.toSet());
    }
}
