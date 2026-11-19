package com.iotsensor.ingestion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.iotsensor.ingestion.config.ValidationConfig;
import com.iotsensor.ingestion.dto.CreateSensorReadingRequest;
import com.iotsensor.ingestion.dto.SensorReadingDto;
import com.iotsensor.ingestion.exception.InvalidTimeRangeException;
import com.iotsensor.ingestion.service.SensorReadingService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(SensorReadingController.class)
@Import(ValidationConfig.class)
class SensorReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorReadingService sensorReadingService;

    @Test
    @SneakyThrows
    void createReadingReturnsCreatedReading() {
        var id = UUID.fromString("7b0c1f0e-3c1a-4d2a-9a51-2f5b6f1d9e01");
        var measuredAt = Instant.parse("2026-11-16T10:15:30Z");
        var created = new SensorReadingDto(id, "sensor-1", 21.5, measuredAt);

        when(sensorReadingService.createReading(any(CreateSensorReadingRequest.class))).thenReturn(created);

        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sensorId": "sensor-1",
                      "value": 21.5,
                      "measuredAt": "2026-11-16T10:15:30Z"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.sensorId").value("sensor-1"))
            .andExpect(jsonPath("$.value").value(21.5))
            .andExpect(jsonPath("$.measuredAt").value("2026-11-16T10:15:30Z"));

        verify(sensorReadingService).createReading(new CreateSensorReadingRequest("sensor-1", 21.5, measuredAt));
    }

    @Test
    @SneakyThrows
    void createReadingAcceptsAllAllowedSensorIdCharacters() {
        when(sensorReadingService.createReading(any(CreateSensorReadingRequest.class)))
            .thenReturn(new SensorReadingDto(UUID.randomUUID(), "Hall-A_2.temp", 1.0, Instant.now()));

        postReading("""
            {"sensorId": "Hall-A_2.temp", "value": 1.0}
            """)
            .andExpect(status().isCreated());
    }

    @Test
    @SneakyThrows
    void createReadingRejectsMissingValue() {
        postReading("""
            {"sensorId": "sensor-1"}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("value"))
            .andExpect(jsonPath("$.errors[0].message").value("must not be null"));

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void createReadingRejectsMissingSensorId() {
        postReading("""
            {"value": 1.0}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("sensorId"))
            .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void createReadingRejectsSensorIdWithInvalidCharacters() {
        postReading("""
            {"sensorId": "sensor 1/x", "value": 1.0}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("sensorId"))
            .andExpect(jsonPath("$.errors[0].message").value("may only contain letters, digits, '.', '_' and '-'"));

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void createReadingRejectsTooLongSensorId() {
        postReading("""
            {"sensorId": "%s", "value": 1.0}
            """.formatted("a".repeat(65)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("sensorId"))
            .andExpect(jsonPath("$.errors[0].message").value("must be at most 64 characters"));

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void createReadingReportsAllErrorsSortedByField() {
        postReading("""
            {"sensorId": "bad id", "measuredAt": "2999-01-01T00:00:00Z"}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(3))
            .andExpect(jsonPath("$.errors[0].field").value("measuredAt"))
            .andExpect(jsonPath("$.errors[1].field").value("sensorId"))
            .andExpect(jsonPath("$.errors[2].field").value("value"));
    }

    @Test
    @SneakyThrows
    void createReadingAcceptsMeasuredAtSlightlyInTheFutureWithinClockDriftTolerance() {
        when(sensorReadingService.createReading(any(CreateSensorReadingRequest.class)))
            .thenReturn(new SensorReadingDto(UUID.randomUUID(), "sensor-1", 1.0, Instant.now()));
        var measuredAt = Instant.now().plus(2, ChronoUnit.MINUTES);

        postReading("""
            {"sensorId": "sensor-1", "value": 1.0, "measuredAt": "%s"}
            """.formatted(measuredAt))
            .andExpect(status().isCreated());
    }

    @Test
    @SneakyThrows
    void createReadingRejectsMeasuredAtBeyondClockDriftTolerance() {
        var measuredAt = Instant.now().plus(10, ChronoUnit.MINUTES);

        postReading("""
            {"sensorId": "sensor-1", "value": 1.0, "measuredAt": "%s"}
            """.formatted(measuredAt))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("measuredAt"))
            .andExpect(jsonPath("$.errors[0].message").value("must be a date in the past or in the present"));

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void listReadingsReturnsReadingsForSensor() {
        var readings = List.of(
            new SensorReadingDto(UUID.randomUUID(), "sensor-1", 22.0, Instant.parse("2026-11-16T10:20:00Z")),
            new SensorReadingDto(UUID.randomUUID(), "sensor-1", 21.5, Instant.parse("2026-11-16T10:15:00Z"))
        );

        when(sensorReadingService.listReadings("sensor-1", null, null)).thenReturn(readings);

        mockMvc.perform(get("/v1/readings/sensor-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].sensorId").value("sensor-1"))
            .andExpect(jsonPath("$[0].value").value(22.0))
            .andExpect(jsonPath("$[0].measuredAt").value("2026-11-16T10:20:00Z"))
            .andExpect(jsonPath("$[1].value").value(21.5));

        verify(sensorReadingService).listReadings("sensor-1", null, null);
    }

    @Test
    @SneakyThrows
    void listReadingsReturnsEmptyListForUnknownSensor() {
        when(sensorReadingService.listReadings("unknown", null, null)).thenReturn(List.of());

        mockMvc.perform(get("/v1/readings/unknown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @SneakyThrows
    void listReadingsPassesTimeRangeToService() {
        var from = Instant.parse("2026-11-16T10:00:00Z");
        var to = Instant.parse("2026-11-16T11:00:00Z");
        when(sensorReadingService.listReadings("sensor-1", from, to)).thenReturn(List.of());

        mockMvc.perform(get("/v1/readings/sensor-1")
                .param("from", "2026-11-16T10:00:00Z")
                .param("to", "2026-11-16T11:00:00Z"))
            .andExpect(status().isOk());

        verify(sensorReadingService).listReadings("sensor-1", from, to);
    }

    @Test
    @SneakyThrows
    void listReadingsRejectsInvalidSensorIdInPath() {
        mockMvc.perform(get("/v1/readings/{sensorId}", "bad!id"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("sensorId"))
            .andExpect(jsonPath("$.errors[0].message").value("may only contain letters, digits, '.', '_' and '-'"));

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void listReadingsRejectsTooLongSensorIdInPath() {
        mockMvc.perform(get("/v1/readings/{sensorId}", "a".repeat(65)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("sensorId"))
            .andExpect(jsonPath("$.errors[0].message").value("must be at most 64 characters"));

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void listReadingsRejectsUnparseableTimestamp() {
        mockMvc.perform(get("/v1/readings/sensor-1").param("from", "yesterday"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("from"))
            .andExpect(jsonPath("$.errors[0].message").value("has an invalid format"));

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void listReadingsMapsInvalidTimeRangeToBadRequest() {
        var from = Instant.parse("2026-11-16T11:00:00Z");
        var to = Instant.parse("2026-11-16T10:00:00Z");
        when(sensorReadingService.listReadings("sensor-1", from, to)).thenThrow(new InvalidTimeRangeException(from, to));

        mockMvc.perform(get("/v1/readings/sensor-1")
                .param("from", "2026-11-16T11:00:00Z")
                .param("to", "2026-11-16T10:00:00Z"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Invalid time range."))
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("from"))
            .andExpect(jsonPath("$.errors[0].message").value("must be before 'to'"));
    }

    @Test
    @SneakyThrows
    void malformedJsonIsStillAProblemDetail() {
        postReading("{not json")
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400));
    }

    @SneakyThrows
    private ResultActions postReading(String json) {
        return mockMvc.perform(post("/v1/readings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
    }

}
