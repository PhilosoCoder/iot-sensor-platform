package com.iotsensor.ingestion.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.iotsensor.ingestion.dto.CreateSensorReadingRequest;
import com.iotsensor.ingestion.dto.SensorReadingDto;
import com.iotsensor.ingestion.exception.InvalidTimeRangeException;
import com.iotsensor.ingestion.mapper.SensorReadingMapper;
import com.iotsensor.ingestion.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;

    private final SensorReadingMapper sensorReadingMapper;

    public SensorReadingDto createReading(CreateSensorReadingRequest request) {
        var reading = sensorReadingMapper.requestToEntity(request);
        if (reading.getMeasuredAt() == null) {
            reading.setMeasuredAt(Instant.now());
        }
        // The database stores microseconds; truncate so the POST response matches what GET returns later.
        reading.setMeasuredAt(reading.getMeasuredAt().truncatedTo(ChronoUnit.MICROS));
        var saved = sensorReadingRepository.save(reading);
        return sensorReadingMapper.entityToDto(saved);
    }

    public List<SensorReadingDto> listReadings(String sensorId, Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new InvalidTimeRangeException(from, to);
        }
        var readings = sensorReadingRepository.findInRangeNewestFirst(sensorId, from, to);
        return readings.stream().map(sensorReadingMapper::entityToDto).toList();
    }

}
