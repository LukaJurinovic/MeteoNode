package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.Role;

public record UserDTO(Integer id, String username, String email, Role role) {}
