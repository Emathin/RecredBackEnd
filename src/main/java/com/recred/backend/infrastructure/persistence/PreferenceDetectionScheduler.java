package com.recred.backend.infrastructure.persistence;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PreferenceDetectionScheduler {

    private final StudentRepository studentRepository;
    private final SpringDataBuffetRepository buffetRepository;
    private final DetectStudentPreferences detectStudentPreferences;

    public PreferenceDetectionScheduler(
            StudentRepository studentRepository,
            SpringDataBuffetRepository buffetRepository,
            DetectStudentPreferences detectStudentPreferences) {
        this.studentRepository = studentRepository;
        this.buffetRepository = buffetRepository;
        this.detectStudentPreferences = detectStudentPreferences;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void runDetectionForAllStudents() {
        log.info("Starting scheduled preference detection for all students...");
        
        List<Student> students = studentRepository.findAll();
        List<BuffetEntity> buffets = buffetRepository.findAll();

        if (buffets.isEmpty()) {
            log.warn("No buffets found. Skipping detection.");
            return;
        }

        buffets.forEach(buffet -> {
            students.stream()
                    .filter(Student::getActive)
                    .forEach(alumno -> {
                        try {
                            detectStudentPreferences.execute(alumno.getId(), buffet.getId());
                        } catch (Exception e) {
                            log.error("Error detecting preferences for student {}: {}",
                                    alumno.getId(), e.getMessage());
                        }
                    });
        });

        log.info("Scheduled preference detection finished.");
    }
}
