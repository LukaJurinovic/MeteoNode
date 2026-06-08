package com.example.meteonode.model.dto.request;

import com.example.meteonode.model.enums.Severity;

import java.math.BigDecimal;

public record UpdateAlarmRuleRequest(
        String name,
        BigDecimal thresholdMin,
        BigDecimal thresholdMax,
        Severity severity,
        Integer cooldownSeconds
) {}
