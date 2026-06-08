package com.example.meteonode.mapper;

import com.example.meteonode.model.dto.response.AlarmNotificationDTO;
import com.example.meteonode.model.entity.AlarmNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AlarmNotificationMapper {

    @Mapping(source = "rule.id",         target = "ruleId")
    @Mapping(source = "rule.sensor.id",  target = "sensorId")
    @Mapping(source = "rule.metric",     target = "metric")
    @Mapping(source = "measurement.value", target = "value")
    @Mapping(source = "rule.severity",   target = "severity")
    @Mapping(source = "createdAt",       target = "triggeredAt")
    @Mapping(target = "read",            ignore = true)
    AlarmNotificationDTO toDTO(AlarmNotification notification);
}
