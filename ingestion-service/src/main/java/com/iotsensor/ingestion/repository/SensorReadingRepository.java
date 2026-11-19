package com.iotsensor.ingestion.repository;

import static org.springframework.data.domain.Sort.Order.desc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.iotsensor.ingestion.entity.SensorReading;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SensorReadingRepository extends
        JpaRepository<SensorReading, UUID>,
        JpaSpecificationExecutor<SensorReading> {

    /**
     * The id tie-break makes the order deterministic and matches idx_sensor_reading_sensor_id_measured_at.
     */
    Sort NEWEST_FIRST = Sort.by(desc("measuredAt"), desc("id"));

    /**
     * Readings of a sensor in the half-open range [from, to), newest first. A null bound means "unbounded".
     * <p>
     * Built as a Specification so that only the bounds that are set end up in the SQL: a static
     * {@code (:from is null or ...)} JPQL query fails on PostgreSQL ("could not determine data type of parameter").
     */
    default List<SensorReading> findInRangeNewestFirst(String sensorId, Instant from, Instant to) {
        Specification<SensorReading> spec = (root, query, cb) -> cb.equal(root.get("sensorId"), sensorId);
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("measuredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.<Instant>get("measuredAt"), to));
        }
        return findAll(spec, NEWEST_FIRST);
    }

}
