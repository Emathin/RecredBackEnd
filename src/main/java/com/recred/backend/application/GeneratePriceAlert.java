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
    public void execute(Producto producto, BigDecimal oldPrice,
                        BigDecimal newPrice, UUID buffetId) {
        List<Alumno> affectedStudents = studentAIProfileRepository
                .findStudentsWithFavoriteProduct(producto.getId());

        LOGGER.info(
                "price_alert_candidates productId={} affectedStudents={}",
                producto.getId(),
                affectedStudents.size()
        );

        for (Alumno alumno : affectedStudents) {
            if (hasRecentAlert(alumno.getId(), producto.getId())) {
                LOGGER.info(
                        "price_alert_skipped_recent studentId={} productId={}",
                        alumno.getId(),
                        producto.getId()
                );
                continue;
            }
            priceAlertAIRepository.analyzeImpact(alumno, producto, oldPrice, newPrice)
                    .map(result -> new PriceAlertSuggestion(
                            alumno.getId(),
                            producto.getId(),
                            buffetId,
                            tutorUserIds(alumno),
                            producto.getNombre(),
                            result.titulo(),
                            result.mensaje(),
                            oldPrice,
                            newPrice,
                            result.razonIA()
                    ))
                    .ifPresentOrElse(
                            sugerenciaIARepository::savePriceAlert,
                            () -> LOGGER.warn(
                                    "price_alert_ai_empty studentId={} productId={}",
                                    alumno.getId(),
                                    producto.getId()
                            )
                    );
        }
    }

    private boolean hasRecentAlert(UUID studentId, UUID productId) {
        LocalDateTime since = LocalDateTime.now(clock).minusHours(COOLDOWN_HOURS);
        return sugerenciaIARepository.hasRecentPriceAlert(studentId, productId, since);
    }

    private Set<UUID> tutorUserIds(Alumno alumno) {
        return alumno.getTutores().stream()
                .filter(tutor -> tutor.getUsuario() != null)
                .filter(tutor -> tutor.getUsuario().getId() != null)
                .map(tutor -> tutor.getUsuario().getId())
                .collect(Collectors.toSet());
    }
}
