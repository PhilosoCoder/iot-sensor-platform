package com.iotsensor.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorDto {

    private String id;

    private String name;

    private String type;

    private String location;

    private String unit;

    private boolean active;

}
