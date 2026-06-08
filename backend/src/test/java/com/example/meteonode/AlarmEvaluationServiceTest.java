package com.example.meteonode;

import com.example.meteonode.model.dto.response.AlarmRuleDTO;
import com.example.meteonode.model.dto.response.MeasurementDTO;
import com.example.meteonode.model.enums.Metric;
import com.example.meteonode.model.enums.Severity;
import com.example.meteonode.service.application.AlarmEvaluationService;
import com.example.meteonode.service.domain.AlarmNotificationService;
import com.example.meteonode.service.domain.AlarmRuleService;
import com.example.meteonode.service.domain.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlarmEvaluationServiceTest {

    @Mock AlarmRuleService alarmRuleService;
    @Mock AlarmNotificationService alarmNotificationService;
    @Mock UserService userService;
    @InjectMocks AlarmEvaluationService service;

    private MeasurementDTO measurement;

    @BeforeEach
    void setUp() {
        measurement = new MeasurementDTO(1L, 10, Metric.TEMPERATURE, new BigDecimal("30.0"), Instant.now(), null);
    }

    private AlarmRuleDTO ruleWithMax(BigDecimal max) {
        return new AlarmRuleDTO(1, "High Temp", 10, null, null, Metric.TEMPERATURE,
                null, max, Severity.WARNING, true, 0, null);
    }

    private AlarmRuleDTO ruleWithMin(BigDecimal min) {
        return new AlarmRuleDTO(2, "Low Temp", 10, null, null, Metric.TEMPERATURE,
                min, null, Severity.WARNING, true, 0, null);
    }

    @Test
    void noActiveRules_skipsEvaluation() {
        when(alarmRuleService.findActiveBySensorAndMetric(anyInt(), any())).thenReturn(List.of());

        service.evaluate(measurement);

        verify(alarmNotificationService, never()).create(anyInt(), anyLong(), anyInt(), anyString());
    }

    @Test
    void valueAboveMax_triggersNotification() {
        when(alarmRuleService.findActiveBySensorAndMetric(anyInt(), any()))
                .thenReturn(List.of(ruleWithMax(new BigDecimal("25.0"))));
        when(userService.getAllUserIds()).thenReturn(List.of(1));
        when(alarmRuleService.tryFire(anyInt(), any(), any(Instant.class))).thenReturn(true);

        service.evaluate(measurement);

        verify(alarmRuleService).tryFire(anyInt(), any(), any(Instant.class));
        verify(alarmNotificationService).create(anyInt(), anyLong(), anyInt(), anyString());
    }

    @Test
    void valueBelowMin_triggersNotification() {
        when(alarmRuleService.findActiveBySensorAndMetric(anyInt(), any()))
                .thenReturn(List.of(ruleWithMin(new BigDecimal("35.0"))));
        when(userService.getAllUserIds()).thenReturn(List.of(1));
        when(alarmRuleService.tryFire(anyInt(), any(), any(Instant.class))).thenReturn(true);

        service.evaluate(measurement);

        verify(alarmRuleService).tryFire(anyInt(), any(), any(Instant.class));
        verify(alarmNotificationService).create(anyInt(), anyLong(), anyInt(), anyString());
    }

    @Test
    void valueInRange_noNotification() {
        var rule = new AlarmRuleDTO(1, "Range", 10, null, null, Metric.TEMPERATURE,
                new BigDecimal("20.0"), new BigDecimal("40.0"), Severity.WARNING, true, 0, null);
        when(alarmRuleService.findActiveBySensorAndMetric(anyInt(), any())).thenReturn(List.of(rule));
        when(userService.getAllUserIds()).thenReturn(List.of(1));

        service.evaluate(measurement);

        verify(alarmRuleService, never()).tryFire(anyInt(), any(), any(Instant.class));
        verify(alarmNotificationService, never()).create(anyInt(), anyLong(), anyInt(), anyString());
    }

    @Test
    void tryFire_returnsFalse_suppressesNotification() {
        when(alarmRuleService.findActiveBySensorAndMetric(anyInt(), any()))
                .thenReturn(List.of(ruleWithMax(new BigDecimal("25.0"))));
        when(userService.getAllUserIds()).thenReturn(List.of(1));
        when(alarmRuleService.tryFire(anyInt(), any(), any(Instant.class))).thenReturn(false);

        service.evaluate(measurement);

        verify(alarmNotificationService, never()).create(anyInt(), anyLong(), anyInt(), anyString());
    }

    @Test
    void noRecipients_skipsNotification() {
        when(alarmRuleService.findActiveBySensorAndMetric(anyInt(), any()))
                .thenReturn(List.of(ruleWithMax(new BigDecimal("25.0"))));
        when(userService.getAllUserIds()).thenReturn(List.of());

        service.evaluate(measurement);

        verify(alarmRuleService, never()).tryFire(anyInt(), any(), any(Instant.class));
        verify(alarmNotificationService, never()).create(anyInt(), anyLong(), anyInt(), anyString());
    }
}
