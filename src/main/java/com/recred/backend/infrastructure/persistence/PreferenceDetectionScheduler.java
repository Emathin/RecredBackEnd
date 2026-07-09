package com.recred.backend.infrastructure.persistence;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DetectStudentPreferencesRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PreferenceDetectionScheduler {

    private final AlumnoRepository alumnoRepository;
    private final SpringDataBuffetRepository buffetRepository;
    private final DetectStudentPreferences detectStudentPreferences;

    public PreferenceDetectionScheduler(
            AlumnoRepository alumnoRepository,
            SpringDataBuffetRepository buffetRepository,
            DetectStudentPreferences detectStudentPreferences) {
        this.alumnoRepository = alumnoRepository;
        this.buffetRepository = buffetRepository;
        this.detectStudentPreferences = detectStudentPreferences;
    }

    @Scheduled(cron = "${preference-detection.cron:0 0 2 * * SUN}")
    public void runDetectionForAllStudents() {
        log.info("Starting scheduled preference detection for all students...");
        
        List<Alumno> alumnos = alumnoRepository.findAll();
        List<BuffetEntity> buffets = buffetRepository.findAll();

        if (buffets.isEmpty()) {
            log.warn("No buffets found. Skipping detection.");
            return;
        }

        buffets.forEach(buffet -> {
            alumnos.stream()
                    .filter(Alumno::getActivo)
                    .forEach(alumno -> {
                        try {
                            detectStudentPreferences.execute(
                                    new DetectStudentPreferencesRequest(
                                            alumno.getId(),
                                            buffet.getId()
                                    )
                            );
                        } catch (Exception e) {
                            log.error("Error detecting preferences for student {}: {}",
                                    alumno.getId(), e.getMessage());
                        }
                    });
        });

        log.info("Scheduled preference detection finished.");
    }
}
