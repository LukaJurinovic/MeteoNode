package com.example.meteonode.mapper;

import com.example.meteonode.model.dto.request.RegisterRequest;
import com.example.meteonode.model.dto.response.UserDTO;
import com.example.meteonode.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(RegisterRequest request);
}
