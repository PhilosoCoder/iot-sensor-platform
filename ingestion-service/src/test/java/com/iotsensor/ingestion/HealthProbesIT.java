package com.iotsensor.ingestion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class HealthProbesIT extends IngestionIT {

    @Test
    @SneakyThrows
    void livenessProbeIsUp() {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @SneakyThrows
    void readinessProbeIsUp() {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @SneakyThrows
    void overallHealthIsUp() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @SneakyThrows
    void sensitiveActuatorEndpointsAreNotExposed() {
        mockMvc.perform(get("/actuator/env"))
            .andExpect(status().isNotFound());
    }

}
