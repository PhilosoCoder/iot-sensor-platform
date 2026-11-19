package com.iotsensor.ingestion.controller;

import com.iotsensor.ingestion.dto.SensorDto;
import com.iotsensor.ingestion.service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sensors")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sensors")
public class SensorController {

    private final SensorService sensorService;

    /** Ordered by id. Paging via {@code page} (0-based) and {@code size} (default 50, capped at 500); {@code sort} is ignored. */
    @Operation(summary = "List sensors", description = "Ordered by id. `size` defaults to 50 and is capped at 500; `sort` is ignored.")
    @GetMapping
    public Page<SensorDto> listSensors(@ParameterObject Pageable pageable) {
        return sensorService.listSensors(pageable);
    }

}
