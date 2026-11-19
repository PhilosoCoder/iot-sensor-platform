package com.iotsensor.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
    void listReadingsReturnsPageOfReadings() {
        var readings = List.of(
            new SensorReadingDto(UUID.randomUUID(), "sensor-1", 22.0, Instant.parse("2026-11-16T10:20:00Z")),
            new SensorReadingDto(UUID.randomUUID(), "sensor-1", 21.5, Instant.parse("2026-11-16T10:15:00Z"))
        );
        when(sensorReadingService.listReadings(eq("sensor-1"), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(readings, PageRequest.of(1, 2), 5));

        mockMvc.perform(get("/v1/sensors/sensor-1/readings").param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].sensorId").value("sensor-1"))
            .andExpect(jsonPath("$.content[0].value").value(22.0))
            .andExpect(jsonPath("$.content[0].measuredAt").value("2026-11-16T10:20:00Z"))
            .andExpect(jsonPath("$.content[1].value").value(21.5))
            .andExpect(jsonPath("$.page.size").value(2))
            .andExpect(jsonPath("$.page.number").value(1))
            .andExpect(jsonPath("$.page.totalElements").value(5))
            .andExpect(jsonPath("$.page.totalPages").value(3))
            // VIA_DTO: no Spring Data internals (pageable, sort, ...) leak into the response
            .andExpect(jsonPath("$.pageable").doesNotExist())
            .andExpect(jsonPath("$.sort").doesNotExist());
    }

    @Test
    @SneakyThrows
    void listReadingsReturnsEmptyPageForUnknownSensor() {
        when(sensorReadingService.listReadings(eq("unknown"), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(Page.empty(PageRequest.of(0, 50)));

        mockMvc.perform(get("/v1/sensors/unknown/readings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @SneakyThrows
    void listReadingsUsesDefaultPageSizeOf50() {
        var pageable = performListReadingsAndCapturePageable(get("/v1/sensors/sensor-1/readings"));

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    @SneakyThrows
    void listReadingsPassesRequestedPageAndSize() {
        var pageable = performListReadingsAndCapturePageable(
            get("/v1/sensors/sensor-1/readings").param("page", "3").param("size", "20"));

        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    @SneakyThrows
    void listReadingsCapsPageSizeAt500() {
        var pageable = performListReadingsAndCapturePageable(
            get("/v1/sensors/sensor-1/readings").param("size", "10000"));

        assertThat(pageable.getPageSize()).isEqualTo(500);
    }

    @Test
    @SneakyThrows
    void listReadingsPassesTimeRangeToService() {
        var from = Instant.parse("2026-11-16T10:00:00Z");
        var to = Instant.parse("2026-11-16T11:00:00Z");
        when(sensorReadingService.listReadings(eq("sensor-1"), eq(from), eq(to), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get("/v1/sensors/sensor-1/readings")
                .param("from", "2026-11-16T10:00:00Z")
                .param("to", "2026-11-16T11:00:00Z"))
            .andExpect(status().isOk());

        verify(sensorReadingService).listReadings(eq("sensor-1"), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    @SneakyThrows
    void oldReadingsPathIsGone() {
        mockMvc.perform(get("/v1/readings/sensor-1"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void listReadingsRejectsInvalidSensorIdInPath() {
        mockMvc.perform(get("/v1/sensors/{sensorId}/readings", "bad!id"))
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
        mockMvc.perform(get("/v1/sensors/{sensorId}/readings", "a".repeat(65)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("sensorId"))
            .andExpect(jsonPath("$.errors[0].message").value("must be at most 64 characters"));

        verifyNoInteractions(sensorReadingService);
    }

    @Test
    @SneakyThrows
    void listReadingsRejectsUnparseableTimestamp() {
        mockMvc.perform(get("/v1/sensors/sensor-1/readings").param("from", "yesterday"))
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
        when(sensorReadingService.listReadings(eq("sensor-1"), eq(from), eq(to), any(Pageable.class)))
            .thenThrow(new InvalidTimeRangeException(from, to));

        mockMvc.perform(get("/v1/sensors/sensor-1/readings")
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
    private Pageable performListReadingsAndCapturePageable(MockHttpServletRequestBuilder request) {
        when(sensorReadingService.listReadings(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(request).andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorReadingService).listReadings(eq("sensor-1"), isNull(), isNull(), captor.capture());
        return captor.getValue();
    }

    @SneakyThrows
    private ResultActions postReading(String json) {
        return mockMvc.perform(post("/v1/readings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
    }

}
