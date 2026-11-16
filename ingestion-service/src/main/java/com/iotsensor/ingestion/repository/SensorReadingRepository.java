package com.iotsensor.ingestion.repository;

import java.util.List;
import java.util.UUID;

import com.iotsensor.ingestion.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorReadingRepository extends JpaRepository<SensorReading, UUID> {

    List<SensorReading> findBySensorIdOrderByTimestampDesc(String sensorId);

}
