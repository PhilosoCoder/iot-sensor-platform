package com.iotsensor.ingestion.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "sensor_reading",
    indexes = @Index(name = "idx_sensor_reading_sensor_id_timestamp", columnList = "sensor_id, reading_timestamp")
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Column(name = "reading_value", nullable = false)
    private double value;

    @Column(name = "reading_timestamp", nullable = false)
    private Instant timestamp;

}
