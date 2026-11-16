package com.iotsensor.ingestion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.iotsensor.ingestion.dto.CreateSensorReadingRequest;
import com.iotsensor.ingestion.dto.SensorReadingDto;
import com.iotsensor.ingestion.service.SensorReadingService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SensorReadingController.class)
class SensorReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorReadingService sensorReadingService;

    @Test
    @SneakyThrows
    void createReadingReturnsCreatedReading() {
        var id = UUID.fromString("7b0c1f0e-3c1a-4d2a-9a51-2f5b6f1d9e01");
        var timestamp = Instant.parse("2026-11-16T10:15:30Z");
        var created = new SensorReadingDto(id, "sensor-1", 21.5, timestamp);

        when(sensorReadingService.createReading(any(CreateSensorReadingRequest.class))).thenReturn(created);

        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sensorId": "sensor-1",
                      "value": 21.5,
                      "timestamp": "2026-11-16T10:15:30Z"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.sensorId").value("sensor-1"))
            .andExpect(jsonPath("$.value").value(21.5))
            .andExpect(jsonPath("$.timestamp").value("2026-11-16T10:15:30Z"));

        verify(sensorReadingService).createReading(new CreateSensorReadingRequest("sensor-1", 21.5, timestamp));
    }

    @Test
    @SneakyThrows
    void listReadingsReturnsReadingsForSensor() {
        var readings = List.of(
            new SensorReadingDto(UUID.randomUUID(), "sensor-1", 22.0, Instant.parse("2026-11-16T10:20:00Z")),
            new SensorReadingDto(UUID.randomUUID(), "sensor-1", 21.5, Instant.parse("2026-11-16T10:15:00Z"))
        );

        when(sensorReadingService.listReadings("sensor-1")).thenReturn(readings);

        mockMvc.perform(get("/v1/readings/sensor-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].sensorId").value("sensor-1"))
            .andExpect(jsonPath("$[0].value").value(22.0))
            .andExpect(jsonPath("$[0].timestamp").value("2026-11-16T10:20:00Z"))
            .andExpect(jsonPath("$[1].value").value(21.5));

        verify(sensorReadingService).listReadings("sensor-1");
    }

    @Test
    @SneakyThrows
    void listReadingsReturnsEmptyListForUnknownSensor() {
        when(sensorReadingService.listReadings("unknown")).thenReturn(List.of());

        mockMvc.perform(get("/v1/readings/unknown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

}
