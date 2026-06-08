package com.example.meteonode.service.domain;

import com.example.meteonode.exception.ResourceNotFoundException;
import com.example.meteonode.mapper.AlarmRuleMapper;
import com.example.meteonode.model.dto.request.CreateAlarmRuleRequest;
import com.example.meteonode.model.dto.request.UpdateAlarmRuleRequest;
import com.example.meteonode.model.dto.response.AlarmRuleDTO;
import com.example.meteonode.model.entity.AlarmRule;
import com.example.meteonode.model.entity.Sensor;
import com.example.meteonode.model.enums.Metric;
import com.example.meteonode.repository.AlarmRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmRuleService {

    private final AlarmRuleRepository alarmRuleRepository;
    private final AlarmRuleMapper alarmRuleMapper;

    @Transactional(readOnly = true)
    public List<AlarmRuleDTO> findAll() {
        return alarmRuleRepository.findAll().stream()
                .map(alarmRuleMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlarmRuleDTO> findBySensorId(Integer sensorId) {
        return alarmRuleRepository.findBySensorId(sensorId).stream()
                .map(alarmRuleMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlarmRuleDTO> findActiveBySensorAndMetric(Integer sensorId, Metric metric) {
        return alarmRuleRepository.findBySensorIdAndMetricAndIsActiveTrue(sensorId, metric).stream()
                .map(alarmRuleMapper::toDTO)
                .toList();
    }

    @Transactional
    public AlarmRuleDTO create(CreateAlarmRuleRequest request) {
        var sensorRef = new Sensor();
        sensorRef.setId(request.sensorId());

        var rule = new AlarmRule();
        rule.setName(request.name());
        rule.setSensor(sensorRef);
        rule.setMetric(request.metric());
        rule.setThresholdMin(request.thresholdMin());
        rule.setThresholdMax(request.thresholdMax());
        rule.setSeverity(request.severity());
        if (request.cooldownSeconds() != null) rule.setCooldownSeconds(request.cooldownSeconds());

        return alarmRuleMapper.toDTO(alarmRuleRepository.save(rule));
    }

    @Transactional
    public AlarmRuleDTO update(Integer id, UpdateAlarmRuleRequest request) {
        var rule = getById(id);
        if (request.name() != null) rule.setName(request.name());
        if (request.thresholdMin() != null) rule.setThresholdMin(request.thresholdMin());
        if (request.thresholdMax() != null) rule.setThresholdMax(request.thresholdMax());
        if (request.severity() != null) rule.setSeverity(request.severity());
        if (request.cooldownSeconds() != null) rule.setCooldownSeconds(request.cooldownSeconds());
        return alarmRuleMapper.toDTO(rule);
    }

    @Transactional
    public AlarmRuleDTO toggle(Integer id) {
        var rule = getById(id);
        rule.setActive(!rule.isActive());
        return alarmRuleMapper.toDTO(rule);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryFire(Integer id, Integer cooldownSeconds, Instant now) {
        var threshold = (cooldownSeconds != null && cooldownSeconds > 0)
                ? now.minusSeconds(cooldownSeconds)
                : now;
        return alarmRuleRepository.fireIfReady(id, now, threshold) > 0;
    }

    @Transactional
    public void delete(Integer id) {
        alarmRuleRepository.delete(getById(id));
    }

    private AlarmRule getById(Integer id) {
        return alarmRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alarm rule not found: " + id));
    }
}
