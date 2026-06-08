package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.SensorType;

import java.util.Map;

public record NodeProvisionResponse(Integer nodeId, Map<SensorType, Integer> sensorIds, boolean created) {}
