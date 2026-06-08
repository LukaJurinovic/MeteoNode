package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.Metric;
import com.example.meteonode.model.enums.Severity;
import com.example.meteonode.model.enums.SensorType;

import java.math.BigDecimal;
import java.time.Instant;

public record AlarmRuleDTO(
        Integer id,
        String name,
        Integer sensorId,
        SensorType sensorType,
        Integer nodeId,
        Metric metric,
        BigDecimal thresholdMin,
        BigDecimal thresholdMax,
        Severity severity,
        boolean isActive,
        Integer cooldownSeconds,
        Instant lastFiredAt
) {}
