package com.example.meteonode.mapper;

import com.example.meteonode.model.dto.response.WeatherStationDTO;
import com.example.meteonode.model.entity.WeatherStation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeatherStationMapper {

    @Mapping(source = "owner.id", target = "ownerId")
    WeatherStationDTO toDTO(WeatherStation station);
}
