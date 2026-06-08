package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.Metric;

import java.math.BigDecimal;
import java.time.Instant;

public record MeasurementDTO(
        Long id,
        Integer sensorId,
        Metric metric,
        BigDecimal value,
        Instant measuredAt,
        Instant receivedAt
) {}
