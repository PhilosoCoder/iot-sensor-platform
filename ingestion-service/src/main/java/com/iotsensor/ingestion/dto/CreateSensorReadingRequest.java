package com.iotsensor.ingestion.dto;

import java.time.Instant;

import com.iotsensor.ingestion.validation.SensorId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSensorReadingRequest {

    @SensorId
    private String sensorId;

    @NotNull
    private Double value;

    @PastOrPresent
    private Instant measuredAt;

}
