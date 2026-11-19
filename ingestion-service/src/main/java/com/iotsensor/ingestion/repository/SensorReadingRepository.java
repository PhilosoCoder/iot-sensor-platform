package com.iotsensor.ingestion.repository;

import static org.springframework.data.domain.Sort.Order.desc;

import java.time.Instant;
import java.util.UUID;

import com.iotsensor.ingestion.entity.SensorReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SensorReadingRepository
    extends JpaRepository<SensorReading, UUID>, JpaSpecificationExecutor<SensorReading> {

    /** Matches idx_sensor_reading_sensor_id_measured_at; keep the two in sync. */
    Sort NEWEST_FIRST = Sort.by(desc("measuredAt"), desc("id"));

    /**
     * Built as a Specification so that only the bounds that are set end up in the SQL: a static
     * {@code (:from is null or ...)} JPQL query fails on PostgreSQL ("could not determine data type of parameter").
     */
    default Page<SensorReading> findInRange(String sensorId, Instant from, Instant to, Pageable pageable) {
        Specification<SensorReading> spec = (root, _, cb) -> cb.equal(root.get("sensorId"), sensorId);
        if (from != null) {
            spec = spec.and((root, _, cb) -> cb.greaterThanOrEqualTo(root.get("measuredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, _, cb) -> cb.lessThan(root.get("measuredAt"), to));
        }
        return findAll(spec, pageable);
    }

}
