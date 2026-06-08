package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.SensorType;

public record SensorDTO(Integer id, Integer nodeId, SensorType sensorType, boolean isActive) {}
