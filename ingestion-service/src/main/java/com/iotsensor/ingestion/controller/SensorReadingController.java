package com.iotsensor.ingestion.controller;

import java.time.Instant;
import java.util.List;

import com.iotsensor.ingestion.dto.CreateSensorReadingRequest;
import com.iotsensor.ingestion.dto.SensorReadingDto;
import com.iotsensor.ingestion.service.SensorReadingService;
import com.iotsensor.ingestion.validation.SensorId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/readings")
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SensorReadingDto createReading(@Valid @RequestBody CreateSensorReadingRequest request) {
        return sensorReadingService.createReading(request);
    }

    /**
     * {@code from} (inclusive) and {@code to} (exclusive) are optional ISO-8601 instants,
     * e.g. {@code 2026-11-16T10:00:00Z}.
     */
    @GetMapping("/{sensorId}")
    public List<SensorReadingDto> listReadings(
        @PathVariable @SensorId String sensorId,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to
    ) {
        return sensorReadingService.listReadings(sensorId, from, to);
    }

}
