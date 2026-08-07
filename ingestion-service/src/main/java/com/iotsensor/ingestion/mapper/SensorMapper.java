package com.iotsensor.ingestion.mapper;

import com.iotsensor.ingestion.dto.SensorDto;
import com.iotsensor.ingestion.entity.Sensor;
import org.mapstruct.Mapper;

@Mapper
public interface SensorMapper {

    SensorDto entityToDto(Sensor sensor);

}
