package com.iotsensor.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.iotsensor.ingestion.entity.Sensor;
import com.iotsensor.ingestion.mapper.SensorMapperImpl;
import com.iotsensor.ingestion.repository.SensorRepository;
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
class SensorServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    private SensorService sensorService;

    @BeforeEach
    void setUp() {
        sensorService = new SensorService(sensorRepository, new SensorMapperImpl());
    }

    @Test
    void listSensorsMapsRepositoryPage() {
        var sensor = new Sensor("sensor-1", "Temperature Sensor", "temperature", "warehouse", "celsius", true);
        when(sensorRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(sensor), PageRequest.of(0, 1, Sort.by("id")), 2));

        var result = sensorService.listSensors(PageRequest.of(0, 1));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo("sensor-1");
            assertThat(dto.getName()).isEqualTo("Temperature Sensor");
            assertThat(dto.isActive()).isTrue();
        });
    }

    @Test
    void listSensorsKeepsPageAndSizeButForcesSortById() {
        when(sensorRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        sensorService.listSensors(PageRequest.of(2, 10, Sort.by(Sort.Order.desc("name"))));

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("id"));
    }

}
