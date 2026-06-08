package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.response.AlarmRuleDTO;
import com.example.meteonode.model.dto.response.MeasurementDTO;
import com.example.meteonode.service.domain.AlarmNotificationService;
import com.example.meteonode.service.domain.AlarmRuleService;
import com.example.meteonode.service.domain.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmEvaluationService {

    private final AlarmRuleService alarmRuleService;
    private final AlarmNotificationService alarmNotificationService;
    private final UserService userService;

    @Transactional
    public void evaluate(MeasurementDTO measurement) {
        var activeRules = alarmRuleService.findActiveBySensorAndMetric(
                measurement.sensorId(), measurement.metric());

        if (activeRules.isEmpty()) {
            log.debug("Alarm eval: no active rule for sensor {} metric {} (value {})",
                    measurement.sensorId(), measurement.metric(), measurement.value());
            return;
        }

        var recipients = userService.getAllUserIds();

        if (recipients.isEmpty()) {
            log.warn("Alarm eval: {} rule(s) matched sensor {} metric {} but there are no users to notify",
                    activeRules.size(), measurement.sensorId(), measurement.metric());
            return;
        }

        var now = Instant.now();

        for (AlarmRuleDTO rule : activeRules) {
            if (!isBreached(measurement.value(), rule)) {
                log.debug("Alarm eval: rule {} not breached by value {} (min {}, max {})",
                        rule.id(), measurement.value(), rule.thresholdMin(), rule.thresholdMax());
                continue;
            }
            if (!alarmRuleService.tryFire(rule.id(), rule.cooldownSeconds(), now)) {
                log.debug("Alarm eval: rule {} breached by value {} but suppressed by cooldown ({}s)",
                        rule.id(), measurement.value(), rule.cooldownSeconds());
                continue;
            }

            var message = buildMessage(measurement, rule);
            for (Integer userId : recipients) {
                alarmNotificationService.create(rule.id(), measurement.id(), userId, message);
            }
            log.info("Alarm fired: rule {} [{}] sensor {} {} = {} -> {} notification(s) created",
                    rule.id(), rule.severity(), measurement.sensorId(), measurement.metric(),
                    measurement.value(), recipients.size());
        }
    }

    private boolean isBreached(BigDecimal value, AlarmRuleDTO rule) {
        return (rule.thresholdMin() != null && value.compareTo(rule.thresholdMin()) < 0)
                || (rule.thresholdMax() != null && value.compareTo(rule.thresholdMax()) > 0);
    }

    private String buildMessage(MeasurementDTO measurement, AlarmRuleDTO rule) {
        return String.format("Alarm [%s]: %s %s = %s (min: %s, max: %s)",
                rule.severity(), Objects.requireNonNullElse(rule.name(), "(unnamed)"),
                measurement.metric(), measurement.value(),
                rule.thresholdMin(), rule.thresholdMax());
    }
}
