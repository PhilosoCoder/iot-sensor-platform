package com.iotsensor.ingestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sensor {

    @Id
    @Column(length = 64)
    private String id;

    private String name;

    private String type;

    private String location;

    private String unit;

    private boolean active;

}
