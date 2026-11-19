package com.iotsensor.ingestion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @Test
    @SneakyThrows
    void readingsCanBeFilteredByHalfOpenTimeRange() {
        postReading("it-sensor-5", 1.0, "2026-11-16T09:00:00Z");
        postReading("it-sensor-5", 2.0, "2026-11-16T10:00:00Z");
        postReading("it-sensor-5", 3.0, "2026-11-16T10:30:00Z");
        postReading("it-sensor-5", 4.0, "2026-11-16T11:00:00Z");

        // from is inclusive, to is exclusive
        mockMvc.perform(get("/v1/readings/it-sensor-5")
                .param("from", "2026-11-16T10:00:00Z")
                .param("to", "2026-11-16T11:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].value").value(3.0))
            .andExpect(jsonPath("$[1].value").value(2.0));

        mockMvc.perform(get("/v1/readings/it-sensor-5").param("from", "2026-11-16T10:30:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].value").value(4.0))
            .andExpect(jsonPath("$[1].value").value(3.0));

        mockMvc.perform(get("/v1/readings/it-sensor-5").param("to", "2026-11-16T10:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].value").value(1.0));
    }

    @Test
    @SneakyThrows
    void invalidTimeRangeIsRejected() {
        mockMvc.perform(get("/v1/readings/it-sensor-5")
                .param("from", "2026-11-16T10:00:00Z")
                .param("to", "2026-11-16T10:00:00Z"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors[0].field").value("from"))
            .andExpect(jsonPath("$.errors[0].message").value("must be before 'to'"));
    }

    @Test
    @SneakyThrows
    void invalidReadingIsRejectedAndNotStored() {
        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sensorId": "it-sensor-6"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.instance").value("/v1/readings"))
            .andExpect(jsonPath("$.errors[0].field").value("value"))
            .andExpect(jsonPath("$.errors[0].message").value("must not be null"));

        mockMvc.perform(get("/v1/readings/it-sensor-6"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
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
