package com.iotsensor.ingestion.controller;

import java.time.Instant;

import com.iotsensor.ingestion.dto.CreateSensorReadingRequest;
import com.iotsensor.ingestion.dto.SensorReadingDto;
import com.iotsensor.ingestion.service.SensorReadingService;
import com.iotsensor.ingestion.validation.SensorId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Readings are written to {@code /v1/readings} and read as a sub-resource of their sensor.
 */
@Tag(name = "Readings")
@RestController
@RequiredArgsConstructor
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    @Operation(summary = "Record a reading", description = "`measuredAt` defaults to the time of receipt.")
    @PostMapping("/v1/readings")
    @ResponseStatus(HttpStatus.CREATED)
    public SensorReadingDto createReading(@Valid @RequestBody CreateSensorReadingRequest request) {
        return sensorReadingService.createReading(request);
    }

    /**
     * Newest first. {@code from} (inclusive) and {@code to} (exclusive) are optional ISO-8601 instants,
     * e.g. {@code 2026-11-16T10:00:00Z}. Paging via {@code page} (0-based) and {@code size}
     * (default 50, capped at 500); {@code sort} is ignored.
     */
    @Operation(
        summary = "List readings of a sensor",
        description = "Newest first. `from` is inclusive, `to` exclusive. `size` defaults to 50 and is capped at 500; `sort` is ignored.")
    @GetMapping("/v1/sensors/{sensorId}/readings")
    public Page<SensorReadingDto> listReadings(
        @PathVariable @SensorId String sensorId,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @ParameterObject Pageable pageable
    ) {
        return sensorReadingService.listReadings(sensorId, from, to, pageable);
    }

}
