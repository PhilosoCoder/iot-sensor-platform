package com.iotsensor.ingestion.controller;

import java.util.List;

import com.iotsensor.ingestion.dto.CreateSensorReadingRequest;
import com.iotsensor.ingestion.dto.SensorReadingDto;
import com.iotsensor.ingestion.service.SensorReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/readings")
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SensorReadingDto createReading(@RequestBody CreateSensorReadingRequest request) {
        return sensorReadingService.createReading(request);
    }

    @GetMapping("/{sensorId}")
    public List<SensorReadingDto> listReadings(@PathVariable String sensorId) {
        return sensorReadingService.listReadings(sensorId);
    }

}
