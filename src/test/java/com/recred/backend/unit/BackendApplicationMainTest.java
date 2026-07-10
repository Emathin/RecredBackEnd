package com.recred.backend.unit;

import com.recred.backend.BackendApplication;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackendApplicationMainTest {

    @Test
    void mainShouldSetUtcTimezoneAndRunSpringApplication() {
        TimeZone original = TimeZone.getDefault();
        String[] args = new String[]{"--test"};

        try (MockedStatic<SpringApplication> springApplication =
                Mockito.mockStatic(SpringApplication.class)) {
            BackendApplication.main(args);

            assertEquals("UTC", TimeZone.getDefault().getID());
            springApplication.verify(() -> SpringApplication.run(BackendApplication.class, args));
        } finally {
             TimeZone.setDefault(original);
        }
    }
}
