package com.iotsensor.ingestion.service;

import com.iotsensor.ingestion.dto.SensorDto;
import com.iotsensor.ingestion.mapper.SensorMapper;
import com.iotsensor.ingestion.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorService {

    static final Sort BY_ID = Sort.by("id");

    private final SensorRepository sensorRepository;

    private final SensorMapper sensorMapper;

    public Page<SensorDto> listSensors(Pageable pageable) {
        var page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), BY_ID);
        return sensorRepository.findAll(page).map(sensorMapper::entityToDto);
    }

}
