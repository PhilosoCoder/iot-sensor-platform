package com.iotsensor.ingestion.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSensorReadingRequest {

    private String sensorId;

    private double value;

    private Instant measuredAt;

}
