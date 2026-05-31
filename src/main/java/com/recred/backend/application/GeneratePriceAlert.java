package com.recred.backend.application;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.PriceAlertSuggestion;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.repositories.PriceAlertAIRepository;
import com.recred.backend.domain.repositories.StudentAIProfileRepository;
import com.recred.backend.domain.repositories.AISuggestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final AISuggestionRepository aiSuggestionRepository;
    private final Clock clock;

    public GeneratePriceAlert(
            StudentAIProfileRepository studentAIProfileRepository,
            PriceAlertAIRepository priceAlertAIRepository,
            AISuggestionRepository aiSuggestionRepository,
            Clock clock) {
        this.studentAIProfileRepository = studentAIProfileRepository;
        this.priceAlertAIRepository = priceAlertAIRepository;
        this.aiSuggestionRepository = aiSuggestionRepository;
        this.clock = clock;
    }

    @Transactional
    public void execute(Product product, BigDecimal oldPrice,
                        BigDecimal newPrice, UUID buffetId) {
        List<Student> affectedStudents = studentAIProfileRepository
                .findStudentsWithFavoriteProduct(product.getId());

        LOGGER.info(
                "price_alert_candidates productId={} affectedStudents={}",
                product.getId(),
                affectedStudents.size()
        );

        for (Student student : affectedStudents) {
            if (hasRecentAlert(student.getId(), product.getId())) {
                LOGGER.info(
                        "price_alert_skipped_recent studentId={} productId={}",
                        student.getId(),
                        product.getId()
                );
                continue;
            }
            priceAlertAIRepository.analyzeImpact(student, product, oldPrice, newPrice)
                    .map(result -> new PriceAlertSuggestion(
                            student.getId(),
                            product.getId(),
                            buffetId,
                            tutorUserIds(student),
                            product.getName(),
                            result.titulo(),
                            result.mensaje(),
                            oldPrice,
                            newPrice,
                            result.razonIA()
                    ))
                    .ifPresentOrElse(
                            aiSuggestionRepository::savePriceAlert,
                            () -> LOGGER.warn(
                                    "price_alert_ai_empty studentId={} productId={}",
                                    student.getId(),
                                    product.getId()
                            )
                    );
        }
    }

    private boolean hasRecentAlert(UUID studentId, UUID productId) {
        LocalDateTime since = LocalDateTime.now(clock).minusHours(COOLDOWN_HOURS);
        return aiSuggestionRepository.hasRecentPriceAlert(studentId, productId, since);
    }

    private Set<UUID> tutorUserIds(Student student) {
        return student.getTutors().stream()
                .filter(tutor -> tutor.getUser() != null)
                .filter(tutor -> tutor.getUser().getId() != null)
                .map(tutor -> tutor.getUser().getId())
                .collect(Collectors.toSet());
    }
}
