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
    public Optional<FirebaseApp> firebaseApp(FirebaseProperties firebaseProperties) {
        if (!firebaseProperties.enabled()) {
            return Optional.empty();
        }
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return Optional.of(FirebaseApp.getInstance());
            }
            return Optional.of(FirebaseApp.initializeApp(options(firebaseProperties)));
        } catch (IOException exception) {
            LOGGER.warn("Firebase initialization failed: {}", exception.getMessage());
            return Optional.empty();
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
