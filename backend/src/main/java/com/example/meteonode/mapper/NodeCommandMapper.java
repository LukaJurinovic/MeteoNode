package com.example.meteonode.mapper;

import com.example.meteonode.model.dto.response.NodeCommandDTO;
import com.example.meteonode.model.entity.NodeCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NodeCommandMapper {

    @Mapping(source = "node.id", target = "nodeId")
    NodeCommandDTO toDTO(NodeCommand command);
}
