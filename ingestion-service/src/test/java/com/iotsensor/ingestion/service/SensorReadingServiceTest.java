package com.iotsensor.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.iotsensor.ingestion.dto.CreateSensorReadingRequest;
import com.iotsensor.ingestion.entity.SensorReading;
import com.iotsensor.ingestion.mapper.SensorReadingMapperImpl;
import com.iotsensor.ingestion.repository.SensorReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SensorReadingServiceTest {

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    private SensorReadingService sensorReadingService;

    @BeforeEach
    void setUp() {
        sensorReadingService = new SensorReadingService(sensorReadingRepository, new SensorReadingMapperImpl());
    }

    @Test
    void createReadingKeepsProvidedMeasuredAt() {
        var measuredAt = Instant.parse("2026-11-16T10:15:30Z");
        when(sensorReadingRepository.save(any(SensorReading.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        var result = sensorReadingService.createReading(new CreateSensorReadingRequest("sensor-1", 21.5, measuredAt));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getSensorId()).isEqualTo("sensor-1");
        assertThat(result.getValue()).isEqualTo(21.5);
        assertThat(result.getMeasuredAt()).isEqualTo(measuredAt);
    }

    @Test
    void createReadingTruncatesMeasuredAtToMicroseconds() {
        when(sensorReadingRepository.save(any(SensorReading.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        var result = sensorReadingService.createReading(
            new CreateSensorReadingRequest("sensor-1", 21.5, Instant.parse("2026-11-16T10:15:30.123456789Z")));

        assertThat(result.getMeasuredAt()).isEqualTo(Instant.parse("2026-11-16T10:15:30.123456Z"));
    }

    @Test
    void createReadingDefaultsMeasuredAtToNowWhenMissing() {
        when(sensorReadingRepository.save(any(SensorReading.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        // Truncated like the stored value, otherwise "now" may land before "before" within the same microsecond.
        var before = Instant.now().truncatedTo(ChronoUnit.MICROS);

        sensorReadingService.createReading(new CreateSensorReadingRequest("sensor-1", 21.5, null));

        var captor = ArgumentCaptor.forClass(SensorReading.class);
        verify(sensorReadingRepository).save(captor.capture());
        assertThat(captor.getValue().getMeasuredAt()).isBetween(before, Instant.now());
    }

    @Test
    void listReadingsMapsRepositoryResults() {
        var reading = new SensorReading(UUID.randomUUID(), "sensor-1", 21.5, Instant.parse("2026-11-16T10:15:30Z"));
        when(sensorReadingRepository.findBySensorIdOrderByMeasuredAtDesc("sensor-1")).thenReturn(List.of(reading));

        var result = sensorReadingService.listReadings("sensor-1");

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(reading.getId());
            assertThat(dto.getSensorId()).isEqualTo("sensor-1");
            assertThat(dto.getValue()).isEqualTo(21.5);
            assertThat(dto.getMeasuredAt()).isEqualTo(reading.getMeasuredAt());
        });
    }

    private static SensorReading withId(SensorReading reading) {
        reading.setId(UUID.randomUUID());
        return reading;
    }

}
