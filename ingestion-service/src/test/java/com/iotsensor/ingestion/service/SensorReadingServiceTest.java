package com.iotsensor.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.iotsensor.ingestion.dto.CreateSensorReadingRequest;
import com.iotsensor.ingestion.entity.SensorReading;
import com.iotsensor.ingestion.exception.InvalidTimeRangeException;
import com.iotsensor.ingestion.mapper.SensorReadingMapperImpl;
import com.iotsensor.ingestion.repository.SensorReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    void listReadingsMapsRepositoryPage() {
        var reading = new SensorReading(UUID.randomUUID(), "sensor-1", 21.5, Instant.parse("2026-11-16T10:15:30Z"));
        when(sensorReadingRepository.findInRange(eq("sensor-1"), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reading), PageRequest.of(0, 1, SensorReadingRepository.NEWEST_FIRST), 3));

        var result = sensorReadingService.listReadings("sensor-1", null, null, PageRequest.of(0, 1));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(reading.getId());
            assertThat(dto.getSensorId()).isEqualTo("sensor-1");
            assertThat(dto.getValue()).isEqualTo(21.5);
            assertThat(dto.getMeasuredAt()).isEqualTo(reading.getMeasuredAt());
        });
    }

    @Test
    void listReadingsKeepsPageAndSizeButForcesNewestFirstSort() {
        stubEmptyPage();
        var clientRequest = PageRequest.of(3, 20, Sort.by(Sort.Order.asc("value")));

        sensorReadingService.listReadings("sensor-1", null, null, clientRequest);

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorReadingRepository).findInRange(eq("sensor-1"), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(3);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("measuredAt"), Sort.Order.desc("id")));
    }

    @Test
    void listReadingsPassesTimeRangeToRepository() {
        stubEmptyPage();
        var from = Instant.parse("2026-11-16T10:00:00Z");
        var to = Instant.parse("2026-11-16T11:00:00Z");

        assertThat(sensorReadingService.listReadings("sensor-1", from, to, PageRequest.of(0, 50))).isEmpty();

        verify(sensorReadingRepository).findInRange(eq("sensor-1"), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    void listReadingsAllowsOpenEndedRanges() {
        stubEmptyPage();
        var bound = Instant.parse("2026-11-16T10:00:00Z");

        sensorReadingService.listReadings("sensor-1", bound, null, PageRequest.of(0, 50));
        sensorReadingService.listReadings("sensor-1", null, bound, PageRequest.of(0, 50));

        verify(sensorReadingRepository).findInRange(eq("sensor-1"), eq(bound), isNull(), any(Pageable.class));
        verify(sensorReadingRepository).findInRange(eq("sensor-1"), isNull(), eq(bound), any(Pageable.class));
    }

    @Test
    void listReadingsRejectsFromAfterTo() {
        var from = Instant.parse("2026-11-16T11:00:00Z");
        var to = Instant.parse("2026-11-16T10:00:00Z");

        assertThatThrownBy(() -> sensorReadingService.listReadings("sensor-1", from, to, PageRequest.of(0, 50)))
            .isInstanceOf(InvalidTimeRangeException.class);

        verifyNoInteractions(sensorReadingRepository);
    }

    @Test
    void listReadingsRejectsEmptyRange() {
        var instant = Instant.parse("2026-11-16T10:00:00Z");

        assertThatThrownBy(() -> sensorReadingService.listReadings("sensor-1", instant, instant, PageRequest.of(0, 50)))
            .isInstanceOf(InvalidTimeRangeException.class);

        verifyNoInteractions(sensorReadingRepository);
    }

    private void stubEmptyPage() {
        when(sensorReadingRepository.findInRange(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
    }

    private static SensorReading withId(SensorReading reading) {
        reading.setId(UUID.randomUUID());
        return reading;
    }

}
