package com.example.meteonode.model.dto.request;

import com.example.meteonode.model.enums.Role;

public record UpdateRoleRequest(String username, Role role) {}
