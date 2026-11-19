package com.iotsensor.ingestion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SensorReadingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SneakyThrows
    void postedReadingsAreReturnedNewestFirst() {
        postReading("it-sensor-1", 20.0, "2026-11-16T10:00:00Z");
        postReading("it-sensor-1", 22.0, "2026-11-16T10:10:00Z");
        postReading("it-sensor-1", 21.0, "2026-11-16T10:05:00Z");
        postReading("it-sensor-2", 99.0, "2026-11-16T10:30:00Z");

        mockMvc.perform(get("/v1/readings/it-sensor-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].value").value(22.0))
            .andExpect(jsonPath("$[1].value").value(21.0))
            .andExpect(jsonPath("$[2].value").value(20.0));
    }

    @Test
    @SneakyThrows
    void postedReadingWithoutMeasuredAtGetsOne() {
        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sensorId": "it-sensor-3", "value": 5.5}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.measuredAt").isNotEmpty());
    }

    @Test
    @SneakyThrows
    void postAndGetReturnTheSameMeasuredAt() {
        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sensorId": "it-sensor-4", "value": 1.0, "measuredAt": "2026-11-16T10:15:30.123456789Z"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.measuredAt").value("2026-11-16T10:15:30.123456Z"));

        mockMvc.perform(get("/v1/readings/it-sensor-4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].measuredAt").value("2026-11-16T10:15:30.123456Z"));
    }

    @SneakyThrows
    private void postReading(String sensorId, double value, String measuredAt) {
        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sensorId": "%s", "value": %s, "measuredAt": "%s"}
                    """.formatted(sensorId, value, measuredAt)))
            .andExpect(status().isCreated());
    }

}
