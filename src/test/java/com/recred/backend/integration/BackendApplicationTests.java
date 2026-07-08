package com.recred.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BackendApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        ApplicationContext context = givenApplicationContext();

        thenApplicationContextIsLoaded(context);
    }

    @Test
    void healthEndpointShouldReturn200Ok() throws Exception {
        RequestBuilder request = givenHealthRequest();

        ResultActions response = whenPerforming(request);

        thenResponseIsOk(response);
    }

    private ApplicationContext givenApplicationContext() {
        return applicationContext;
    }

    private RequestBuilder givenHealthRequest() {
        return get("/actuator/health");
    }

    private ResultActions whenPerforming(RequestBuilder request) throws Exception {
        return mockMvc.perform(request);
    }

    private void thenApplicationContextIsLoaded(ApplicationContext context) {
        assertNotNull(context);
    }

    private void thenResponseIsOk(ResultActions response) throws Exception {
        response.andExpect(status().isOk());
    }
}
