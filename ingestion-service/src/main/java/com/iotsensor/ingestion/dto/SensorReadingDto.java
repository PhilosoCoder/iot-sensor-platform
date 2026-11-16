package com.iotsensor.ingestion.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorReadingDto {

    private UUID id;

    private String sensorId;

    private double value;

    private Instant timestamp;

}
