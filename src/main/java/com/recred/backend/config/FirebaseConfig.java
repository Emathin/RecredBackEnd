package com.recred.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp firebaseApp(FirebaseProperties firebaseProperties) {
        if (!firebaseProperties.enabled()) {
            return null;
        }
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }
            return FirebaseApp.initializeApp(options(firebaseProperties));
        } catch (Exception exception) {
            LOGGER.error("Error crítico al inicializar Firebase", exception);
            return null;
        }
    }

    private FirebaseOptions options(FirebaseProperties firebaseProperties) throws IOException {
        return FirebaseOptions.builder()
                .setCredentials(credentials(firebaseProperties))
                .build();
    }

    private GoogleCredentials credentials(FirebaseProperties firebaseProperties)
            throws IOException {
        if (firebaseProperties.hasServiceAccountPath()) {
            try (FileInputStream stream = new FileInputStream(
                    firebaseProperties.serviceAccountPath())) {
                return GoogleCredentials.fromStream(stream);
            }
        }
        return GoogleCredentials.getApplicationDefault();
    }
}
