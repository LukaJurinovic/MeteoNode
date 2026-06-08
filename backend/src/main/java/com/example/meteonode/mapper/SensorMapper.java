package com.example.meteonode.mapper;

import com.example.meteonode.model.dto.response.SensorDTO;
import com.example.meteonode.model.entity.Sensor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SensorMapper {

    @Mapping(source = "node.id", target = "nodeId")
    @Mapping(source = "active", target = "isActive")
    SensorDTO toDTO(Sensor sensor);
}
