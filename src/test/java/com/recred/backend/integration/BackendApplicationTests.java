package com.recred.backend.integration;

import com.recred.backend.BackendApplication;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recred.backend.infrastructure.openMeteo.OpenMeteoWeatherClient;

class BackendApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEndpointShouldReturn200Ok() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

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
