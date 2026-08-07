package com.iotsensor.ingestion.repository;

import com.iotsensor.ingestion.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, String> {

}
