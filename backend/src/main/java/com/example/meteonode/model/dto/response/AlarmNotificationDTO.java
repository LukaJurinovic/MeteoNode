package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.Metric;
import com.example.meteonode.model.enums.Severity;

import java.math.BigDecimal;
import java.time.Instant;

public record AlarmNotificationDTO(
        Integer id,
        Integer ruleId,
        Integer sensorId,
        Metric metric,
        BigDecimal value,
        String message,
        Severity severity,
        Instant triggeredAt,
        boolean read
) {
    public AlarmNotificationDTO withRead(boolean newRead) {
        return new AlarmNotificationDTO(id, ruleId, sensorId, metric, value, message, severity, triggeredAt, newRead);
    }
}
