package com.example.meteonode.model.dto.request;

import com.example.meteonode.model.enums.Metric;
import com.example.meteonode.model.enums.Severity;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAlarmRuleRequest(
        String name,
        @NotNull Integer sensorId,
        @NotNull Metric metric,
        BigDecimal thresholdMin,
        BigDecimal thresholdMax,
        @NotNull Severity severity,
        @Min(0) @Max(86400) Integer cooldownSeconds
) {
    @AssertTrue(message = "At least one of thresholdMin or thresholdMax must be specified")
    public boolean isAtLeastOneThresholdSet() {
        return thresholdMin != null || thresholdMax != null;
    }
}
