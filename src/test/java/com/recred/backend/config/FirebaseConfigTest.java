package com.recred.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

class FirebaseConfigTest {

    @Test
    void returnEmptyWhenFirebaseIsDisabled() {
        FirebaseConfig config = new FirebaseConfig();

        Optional<FirebaseApp> app = config.firebaseApp(new FirebaseProperties(false, ""));

        assertTrue(app.isEmpty());
    }

    @Test
    void returnEmptyWhenFirebaseCannotInitialize() {
        FirebaseConfig config = new FirebaseConfig();

        Optional<FirebaseApp> app = config.firebaseApp(new FirebaseProperties(
                true,
                "missing-service-account.json"
        ));

        assertTrue(app.isEmpty());
    }

    @Test
    void returnExistingFirebaseAppWhenAlreadyInitialized() {
        FirebaseConfig config = new FirebaseConfig();
        FirebaseApp firebaseApp = Mockito.mock(FirebaseApp.class);

        try (MockedStatic<FirebaseApp> firebaseApps = Mockito.mockStatic(FirebaseApp.class)) {
            firebaseApps.when(FirebaseApp::getApps).thenReturn(List.of(firebaseApp));
            firebaseApps.when(FirebaseApp::getInstance).thenReturn(firebaseApp);

            Optional<FirebaseApp> app = config.firebaseApp(new FirebaseProperties(true, ""));

            assertTrue(app.isPresent());
            assertSame(firebaseApp, app.orElseThrow());
        }
    }

    @Test
    void initializeFirebaseAppWithDefaultCredentials() throws IOException {
        FirebaseConfig config = new FirebaseConfig();
        FirebaseApp firebaseApp = Mockito.mock(FirebaseApp.class);
        GoogleCredentials credentials = Mockito.mock(GoogleCredentials.class);

        try (
                MockedStatic<FirebaseApp> firebaseApps = Mockito.mockStatic(FirebaseApp.class);
                MockedStatic<GoogleCredentials> googleCredentials = Mockito.mockStatic(
                        GoogleCredentials.class)
        ) {
            firebaseApps.when(FirebaseApp::getApps).thenReturn(List.of());
            googleCredentials.when(GoogleCredentials::getApplicationDefault)
                    .thenReturn(credentials);
            firebaseApps.when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)))
                    .thenReturn(firebaseApp);

            Optional<FirebaseApp> app = config.firebaseApp(new FirebaseProperties(true, ""));

            assertTrue(app.isPresent());
            assertSame(firebaseApp, app.orElseThrow());
        }
    }

    @Test
    void initializeFirebaseAppWithServiceAccountCredentials(@TempDir Path tempDir)
            throws IOException {
        FirebaseConfig config = new FirebaseConfig();
        FirebaseApp firebaseApp = Mockito.mock(FirebaseApp.class);
        GoogleCredentials credentials = Mockito.mock(GoogleCredentials.class);
        Path serviceAccount = tempDir.resolve("firebase.json");
        Files.writeString(serviceAccount, "{}");

        try (
                MockedStatic<FirebaseApp> firebaseApps = Mockito.mockStatic(FirebaseApp.class);
                MockedStatic<GoogleCredentials> googleCredentials = Mockito.mockStatic(
                        GoogleCredentials.class)
        ) {
            firebaseApps.when(FirebaseApp::getApps).thenReturn(List.of());
            googleCredentials.when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
                    .thenReturn(credentials);
            firebaseApps.when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)))
                    .thenReturn(firebaseApp);

            Optional<FirebaseApp> app = config.firebaseApp(new FirebaseProperties(
                    true,
                    serviceAccount.toString()
            ));

            assertTrue(app.isPresent());
            assertSame(firebaseApp, app.orElseThrow());
        }
    }

    @Test
    void detectConfiguredServiceAccountPath() {
        assertTrue(new FirebaseProperties(true, "firebase.json").hasServiceAccountPath());
    }
}
