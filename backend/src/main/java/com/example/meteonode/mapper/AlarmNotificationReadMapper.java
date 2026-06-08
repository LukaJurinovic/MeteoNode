package com.example.meteonode.mapper;

import com.example.meteonode.model.dto.response.AlarmNotificationReadDTO;
import com.example.meteonode.model.entity.AlarmNotificationRead;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AlarmNotificationReadMapper {

    @Mapping(source = "id.notificationId", target = "notificationId")
    @Mapping(source = "id.userId", target = "userId")
    AlarmNotificationReadDTO toDTO(AlarmNotificationRead read);
}
