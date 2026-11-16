package com.iotsensor.ingestion.mapper;

import com.iotsensor.ingestion.dto.CreateSensorReadingRequest;
import com.iotsensor.ingestion.dto.SensorReadingDto;
import com.iotsensor.ingestion.entity.SensorReading;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SensorReadingMapper {

    SensorReadingDto entityToDto(SensorReading sensorReading);

    @Mapping(target = "id", ignore = true)
    SensorReading requestToEntity(CreateSensorReadingRequest request);

}
