package com.example.meteonode.mapper;

import com.example.meteonode.model.dto.response.MeasurementDTO;
import com.example.meteonode.model.entity.Measurement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MeasurementMapper {

    @Mapping(source = "sensor.id", target = "sensorId")
    MeasurementDTO toDTO(Measurement measurement);
}
