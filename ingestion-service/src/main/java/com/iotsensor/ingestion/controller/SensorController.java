package com.iotsensor.ingestion.controller;

import java.util.List;

import com.iotsensor.ingestion.dto.SensorDto;
import com.iotsensor.ingestion.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sensors")
public class SensorController {

    private final SensorService sensorService;

    @GetMapping
    public List<SensorDto> listSensors() {
        return sensorService.listSensors();
    }

}
