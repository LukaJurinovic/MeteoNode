package com.example.meteonode.mapper;

import com.example.meteonode.model.dto.response.NodeDTO;
import com.example.meteonode.model.entity.Node;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NodeMapper {

    @Mapping(source = "station.id", target = "stationId")
    NodeDTO toDTO(Node node);
}
