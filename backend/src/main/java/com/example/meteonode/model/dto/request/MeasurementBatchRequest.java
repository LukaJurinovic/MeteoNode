package com.example.meteonode.model.dto.request;

import com.example.meteonode.model.enums.Metric;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MeasurementBatchRequest(@NotNull Integer nodeId, @NotEmpty List<Entry> measurements) {
    public record Entry(
            @NotNull Integer sensorId,
            @NotNull Metric metric,
            @NotNull BigDecimal value,
            @NotNull Instant measuredAt
    ) {}
}
