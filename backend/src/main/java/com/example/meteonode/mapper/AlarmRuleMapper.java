package com.example.meteonode.mapper;

import com.example.meteonode.model.dto.response.AlarmRuleDTO;
import com.example.meteonode.model.entity.AlarmRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AlarmRuleMapper {

    @Mapping(source = "sensor.id",         target = "sensorId")
    @Mapping(source = "sensor.sensorType", target = "sensorType")
    @Mapping(source = "sensor.node.id",    target = "nodeId")
    @Mapping(source = "active",            target = "isActive")
    AlarmRuleDTO toDTO(AlarmRule rule);
}
