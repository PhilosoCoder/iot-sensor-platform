package com.iotsensor.ingestion.service;

import java.util.List;

import com.iotsensor.ingestion.dto.SensorDto;
import com.iotsensor.ingestion.mapper.SensorMapper;
import com.iotsensor.ingestion.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorRepository sensorRepository;

    private final SensorMapper sensorMapper;

    public List<SensorDto> listSensors() {
        var sensors = sensorRepository.findAll();
        return sensors.stream().map(sensorMapper::entityToDto).toList();
    }

}
