package com.recred.backend.config;

import com.recred.backend.application.ApproveSchoolRegistration;
import com.recred.backend.application.ApproveSchoolRegistrationRequest;
import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.application.SubmitSchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.services.CognitoDirectorIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DevelopmentSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevelopmentSeeder.class);

    @Bean
    public CommandLineRunner seedSchool(SubmitSchoolRegistration submitUseCase, 
                                        ApproveSchoolRegistration approveUseCase,
                                        SchoolRegistrationRequestRepository repo,
                                        CognitoDirectorIdentityService cognito) {
        return args -> {
            boolean exists = repo.existsBySchoolEmailAndStatus(
                    "emmanuel.orieta@gmail.com", 
                    SchoolRegistrationStatus.APPROVED);
            
            if (!exists) {
                LOGGER.info("Seeder: Creando cuenta de Colegio de prueba...");
                try {
                    submitUseCase.execute(new SubmitSchoolRegistrationRequest(
                        "Colegio San José 4", 
                        "emmanuel.orieta@gmail.com", 
                        "1122334455", 
                        "987654321",
                        "Emmanuel", 
                        "Orieta", 
                        "emmanuel.orieta@gmail.com", 
                        "1122334455", 
                        "87654321", 
                        "director_emma"
                    ));
                } catch (Exception e) {
                    LOGGER.info("La solicitud ya estaba creada: {}", 
                            e.getMessage());
                }
                
                try {
                    List<SchoolRegistrationRequest> pending = repo.findByStatus(
                            SchoolRegistrationStatus.PENDING);
                            
                    for (SchoolRegistrationRequest req : pending) {
                        if (req.getSchoolEmail().equals("emmanuel.orieta@gmail.com")) {
                            approveUseCase.execute(
                                    new ApproveSchoolRegistrationRequest(req.getId()));
                            
                            cognito.setPermanentPassword("director_emma", "Recred2026!");
                                    
                            LOGGER.info("==================================================");
                            LOGGER.info("✅ COLEGIO SAN JOSÉ CREADO EXITOSAMENTE!");
                            LOGGER.info("✅ Usuario: director_emma (o emmanuel.orieta@gmail.com)");
                            LOGGER.info("✅ Contraseña DEFINITIVA: Recred2026!");
                            LOGGER.info("==================================================");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error al aprobar colegio de prueba: {}", e.getMessage(), e);
                }
            }
        };
    }
}
