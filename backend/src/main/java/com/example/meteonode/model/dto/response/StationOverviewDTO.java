package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.Metric;
import com.example.meteonode.model.enums.Status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StationOverviewDTO(
        Integer stationId,
        String stationName,
        Status status,
        int nodeCount,
        int onlineNodes,
        List<SensorReading> readings
) {
    public record SensorReading(
            Integer sensorId,
            Integer nodeId,
            String nodeDisplayName,
            Metric metric,
            BigDecimal value,
            Instant measuredAt
    ) {}
}
